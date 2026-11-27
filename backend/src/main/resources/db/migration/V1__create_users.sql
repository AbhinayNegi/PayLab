CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL CHECK (length(btrim(full_name)) > 0),
    email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email) AND email = btrim(email)),
    password_hash TEXT NOT NULL CHECK (length(password_hash) > 0),
    role VARCHAR(20) NOT NULL DEFAULT 'MERCHANT' CHECK (role = 'MERCHANT'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
