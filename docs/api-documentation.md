# API documentation

The base URL is `http://localhost:8080/api/v1`. Use [Swagger UI](http://localhost:8080/swagger-ui/index.html) or the [OpenAPI JSON](http://localhost:8080/v3/api-docs).

| Method and path | Input | Success | Common errors |
|---|---|---|---|
| POST /auth/register | JSON `fullName`, `email`, `password`, `confirmPassword` | 201 profile | 400 invalid input, 409 duplicate |
| POST /auth/login | JSON `email`, `password` | 200 `accessToken`, `tokenType`, `expiresIn` | 400 invalid input, 401 bad credentials |
| GET /users/me | `Authorization: Bearer <accessToken>` | 200 profile | 401 missing, invalid or expired token, or deleted account |

Profiles contain `id` (UUID), `fullName`, `email`, `role` (`MERCHANT`) and `createdAt` (ISO 8601 UTC). Send POST bodies with `Content-Type: application/json`.

JWTs expire 30 minutes after issuance. The login response gives `expiresIn` in seconds (1800). Sign in again for a fresh token. Logout removes the token from browser storage. A copy remains usable until expiration, key rotation, or removal of its account row.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Check the submitted fields.",
  "correlationId": "registration-check-1",
  "fieldErrors": {"confirmPassword": "Passwords must match."}
}
```

Request-wide errors use an empty `fieldErrors` object. Common codes include `VALIDATION_ERROR`, `INVALID_JSON`, `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS` and `UNAUTHORIZED`. Send `X-Correlation-ID` with 1–64 letters, digits, dots, underscores or hyphens to reuse the same ID in the response and logs. Use `GET /actuator/health` on the API origin to check application and database health.
