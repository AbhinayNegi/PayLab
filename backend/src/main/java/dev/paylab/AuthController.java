package dev.paylab;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class AuthController {
    record Registration(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 128)
            @Pattern(regexp = "(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*",
                    message = "Use an uppercase letter, a lowercase letter, and a digit.")
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password,
            @NotBlank @Size(max = 128)
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String confirmPassword) {
        Registration {
            fullName = fullName == null ? null : fullName.strip();
            email = email == null ? null : Accounts.normalize(email);
        }
    }

    record Login(@NotBlank @Email @Size(max = 254) String email,
                 @NotBlank @Size(max = 128)
                 @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) {
        Login {
            email = email == null ? null : Accounts.normalize(email);
        }
    }

    private final Accounts accounts;
    private final JwtEncoder encoder;

    AuthController(Accounts accounts, JwtEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a merchant")
    @ApiResponse(responseCode = "201", description = "Merchant created")
    @ApiResponse(responseCode = "400", description = "Invalid registration",
            content = @Content(schema = @Schema(implementation = ApiErrors.Body.class)))
    @ApiResponse(responseCode = "409", description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ApiErrors.Body.class)))
    Accounts.Profile register(@Valid @RequestBody Registration input) {
        if (!input.password().equals(input.confirmPassword())) {
            throw new ApiErrors.Failure(400, "VALIDATION_ERROR", "Check the submitted fields.",
                    java.util.Map.of("confirmPassword", "Passwords must match."));
        }
        return accounts.create(input.fullName(), input.email(), input.password())
                .orElseThrow(() -> new ApiErrors.Failure(409, "EMAIL_ALREADY_EXISTS",
                        "An account with this email already exists."));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Sign in and obtain a 30-minute JWT")
    @ApiResponse(responseCode = "200", description = "Access token")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiErrors.Body.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiErrors.Body.class)))
    Security.Session login(@Valid @RequestBody Login input) {
        UUID id = accounts.authenticate(input.email(), input.password())
                .orElseThrow(() -> new ApiErrors.Failure(401, "INVALID_CREDENTIALS", "Email or password is incorrect."));
        return Security.issue(encoder, id);
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get the signed-in merchant profile", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Merchant profile")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token or account",
            content = @Content(schema = @Schema(implementation = ApiErrors.Body.class)))
    Accounts.Profile me(@AuthenticationPrincipal Jwt jwt) {
        return accounts.find(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new ApiErrors.Failure(401, "UNAUTHORIZED", "Sign in to continue."));
    }
}
