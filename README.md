# PayLab

PayLab is a merchant-account training environment for UI, API, database and integration testing.

## Start with Docker Compose

From this directory, with a running Docker engine in Linux-container mode and Compose v2:

```powershell
Copy-Item .env.example .env
docker compose up --build --wait
docker compose ps
```

Use `cp .env.example .env` on macOS or Linux. The example selects the development profile and binds the published ports to loopback. Set `SPRING_PROFILES_ACTIVE=default` in a fresh environment to start without seeded fixtures.

| Service | Default address |
|---|---|
| UI | http://localhost:3000 |
| Registration | http://localhost:3000/register |
| Dashboard | http://localhost:3000/dashboard |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

The default database and user are both `paylab`. The example password is `paylab_local`. Use `docker compose exec db psql -U paylab -d paylab` or connect through the published port with a SQL client.

## Development test accounts

| Full name | Email | Password |
|---|---|---|
| Avery Morgan | avery@example.test | PayLabDemo21 |
| Jordan Lee | jordan@example.test | PayLabDemo21 |

The `dev` profile inserts missing fixture accounts after migrations. To rerun the seeder against an existing development database, run `docker compose restart backend`.

## Stop or reset

Run `docker compose down` to stop the stack and keep the data.

To **erase the entire local PayLab development database**, verify you are targeting the training project and have no data to keep. With the `dev` profile in `.env`, run:

```powershell
docker compose down --volumes
docker compose up --build --wait
```

Flyway recreates the schema and the development profile reseeds the two accounts. Sign out or clear the site's session storage after reset.

## Developer checks

With Java 21 selected by `JAVA_HOME`, Maven 3.9, and Node 24:

```powershell
mvn -f backend/pom.xml test
npm --prefix frontend ci
npm --prefix frontend run build
```

For local iteration with PostgreSQL already provisioned, set `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PAYLAB_DB_URL` (for example, `jdbc:postgresql://localhost:5432/paylab`), `PAYLAB_JWT_SECRET` and optionally `SPRING_PROFILES_ACTIVE` in the backend shell. Run `mvn -f backend/pom.xml spring-boot:run` and `npm --prefix frontend run dev` in separate shells. Vite serves on port 5173 and proxies API calls to port 8080 by default. Set `PAYLAB_PROXY_TARGET` to override the backend origin.

Check health and Swagger. Register a fresh email, try registering its uppercase equivalent, and verify that the duplicate is rejected. Sign in, check the profile and empty dashboard sections, refresh, log out, and revisit the dashboard URL. In Swagger, call `/users/me` before and after authorizing with a login token. Restart the backend and sign in with the newly registered account again. Inspect PostgreSQL for the normalized email, password hash, role and timestamps.

## Troubleshooting

- **Docker connection error** — Start the engine and check Linux-container mode with `docker info`.
- **Missing variables** — Copy the example into this directory and run `docker compose config --quiet`. The signing secret must be at least 32 UTF-8 bytes.
- **Port in use** — Change `FRONTEND_PORT`, `BACKEND_PORT`, or `POSTGRES_PORT` in `.env` and rerun Compose.
- **Unhealthy service** — Inspect `docker compose ps` and `docker compose logs db backend frontend`. PostgreSQL initializes its credentials on first use of the volume. Change the role password inside PostgreSQL or perform a local reset if you replace the example password later.
- **Seed login fails** — Confirm that the backend uses the `dev` profile. Seeds preserve existing rows. A development reset restores the fixture credentials.
- **Maven uses older Java** — Check `mvn -version` and point `JAVA_HOME` to the Java 21 JDK.

[Architecture](docs/architecture.md) · [Business requirements](docs/business-requirements.md) · [API documentation](docs/api-documentation.md) · [Database schema](docs/database-schema.md) · [Known limitations](docs/known-limitations.md)
