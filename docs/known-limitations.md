# Known limitations

- The wallet and transaction sections are empty by design.
- The browser uses a single 30-minute JWT. It clears the session on its expiration timer or when a protected API request returns 401. Logout does not revoke copied tokens.
- The local environment uses loopback HTTP and example credentials. Email verification, password reset and login rate limiting are outside the milestone.
- Reset erases the entire development volume. Development seeding adds fixtures only when their email addresses are absent.

## Verification on the implementation machine

Docker Compose accepted the configuration. Container startup and volume reset could not be exercised because the local Docker engine pipe was unavailable. The packaged backend and built frontend were exercised outside containers against PostgreSQL 18.7. The checks covered registration, login, logout, token rejection, concurrent duplicate registration, the dashboard, persistence across a backend restart, Swagger and health. All nine Maven tests passed.
