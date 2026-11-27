package dev.paylab;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = "paylab.jwt-secret=test-signing-key-long-enough-for-hs256")
@Import(Security.class)
class AuthControllerTest {
    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ME = "/api/v1/users/me";
    private static final String VALID = """
            {"fullName":" Avery Morgan ","email":"AVERY@example.test","password":"Example123","confirmPassword":"Example123"}
            """;

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean Accounts accounts;

    @Test
    void registrationNormalizesInputAndReturnsProfile() throws Exception {
        UUID id = UUID.randomUUID();
        when(accounts.create("Avery Morgan", "avery@example.test", "Example123"))
                .thenReturn(Optional.of(new Accounts.Profile(id, "Avery Morgan", "avery@example.test",
                        "MERCHANT", Instant.EPOCH)));
        mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void mismatchedPasswordDoesNotCreateAccount() throws Exception {
        mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("confirmPassword\":\"Example123", "confirmPassword\":\"Example124")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword").value("Passwords must match."));
        verifyNoInteractions(accounts);
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void missingInputReturnsValidationErrors() throws Exception {
        mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword").exists());
        verifyNoInteractions(accounts);
    }

    @Test
    void malformedJsonReturnsConsistentError() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void badCredentialsReturnGenericError() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"avery@example.test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unauthenticatedRequestsAreCorrelated() throws Exception {
        mvc.perform(get(ME).header("X-Correlation-ID", "sample-42"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", "sample-42"))
                .andExpect(jsonPath("$.correlationId").value("sample-42"));
    }

    @Test
    void jwtAuthorizesProfileRequest() throws Exception {
        UUID id = UUID.randomUUID();
        when(accounts.find(id)).thenReturn(Optional.of(new Accounts.Profile(id, "Avery Morgan",
                "avery@example.test", "MERCHANT", Instant.EPOCH)));
        String token = Security.issue(encoder, id).accessToken();
        mvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("avery@example.test"));
    }

    @Test
    void expiredJwtCannotAccessProfile() throws Exception {
        var claims = JwtClaimsSet.builder()
                .issuer("paylab")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now().minusSeconds(1801))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        mvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(accounts);
    }
}
