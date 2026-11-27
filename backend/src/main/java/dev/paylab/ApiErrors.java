package dev.paylab;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiErrors extends ResponseEntityExceptionHandler {
    record Body(String code, String message, String correlationId, Map<String, String> fieldErrors) {}

    static class Failure extends RuntimeException {
        final int status;
        final String code;
        final Map<String, String> fields;

        Failure(int status, String code, String message) {
            this(status, code, message, Map.of());
        }

        Failure(int status, String code, String message, Map<String, String> fields) {
            super(message);
            this.status = status;
            this.code = code;
            this.fields = fields;
        }
    }

    private final ObjectMapper json;

    ApiErrors(ObjectMapper json) {
        this.json = json;
    }

    @ExceptionHandler(Failure.class)
    ResponseEntity<Body> failure(Failure error) {
        return ResponseEntity.status(error.status).body(body(error.code, error.getMessage(), error.fields));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object defaultBody,
            org.springframework.http.HttpHeaders headers, org.springframework.http.HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {
        if (error instanceof MethodArgumentNotValidException invalid) {
            var fields = new TreeMap<String, String>();
            invalid.getBindingResult().getFieldErrors().forEach(field -> fields.merge(
                    field.getField(), field.getDefaultMessage(), (first, second) ->
                            first.compareTo(second) <= 0 ? first : second));
            return new ResponseEntity<>(body("VALIDATION_ERROR", "Check the submitted fields.", fields), headers, status);
        }
        if (error instanceof HttpMessageNotReadableException) {
            return new ResponseEntity<>(body("INVALID_JSON", "Provide a valid JSON request body.", Map.of()), headers, status);
        }
        return new ResponseEntity<>(body("HTTP_" + status.value(), "The request could not be completed.", Map.of()),
                headers, status);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Body> unexpected(Exception error) {
        LoggerFactory.getLogger(ApiErrors.class).atError().addKeyValue("exceptionType", error.getClass().getName())
                .log("Unhandled request error");
        return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR", "An unexpected error occurred.", Map.of()));
    }

    void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, 401, "UNAUTHORIZED", "Sign in to continue.");
    }

    void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), body(code, message, Map.of()));
    }

    private static Body body(String code, String message, Map<String, String> fields) {
        return new Body(code, message, MDC.get("correlationId"), fields);
    }
}
