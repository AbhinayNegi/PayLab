# Database schema

Flyway applies `V1__create_users.sql` and records its execution in `flyway_schema_history`.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Generated primary key |
| full_name | VARCHAR(120) | Required, nonblank |
| email | VARCHAR(254) | Required, unique, lowercase, no leading or trailing spaces |
| password_hash | TEXT | Required, nonempty PBKDF2 hash |
| role | VARCHAR(20) | Required, defaults and is constrained to MERCHANT |
| created_at | TIMESTAMPTZ | Required, defaults to insert time |
| updated_at | TIMESTAMPTZ | Required, defaults to insert time |

Milestone 1 has no user update operation. Both timestamps reflect account creation during normal use.

```sql
SELECT id, full_name, email, role, created_at, updated_at, length(password_hash) AS hash_length
FROM users
ORDER BY created_at, id;
```

Development seeds use the account service after Flyway completes. The Compose named volume retains user rows across ordinary teardown and restarts.
