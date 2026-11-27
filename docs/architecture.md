# Architecture

The React and TypeScript SPA uses relative `/api/v1` paths. Compose serves the Vite build through Nginx, which proxies API calls to Spring Boot on the Docker network. Vite's development server provides the same browser API paths. Spring Boot also publishes a direct port for API testing.

Spring MVC deserializes and validates JSON. The account service uses parameterized Spring JDBC queries. Flyway migrates PostgreSQL at startup. Registration uses one insert with PostgreSQL conflict handling, so concurrent registrations cannot both claim the same email. Passwords use Spring Security PBKDF2 with independent salts.

Spring Security verifies HS256 bearer JWTs using the configured secret, expected issuer and expiration. The subject is the user UUID. Profile lookups read the current database row. The browser stores the token in session storage to survive same-tab reloads and clears it on expiration or logout.

A request filter echoes a valid correlation ID or generates a UUID. Error envelopes and JSON request logs include the ID. Logs include method, path, status and duration while excluding request bodies, credentials and tokens.
