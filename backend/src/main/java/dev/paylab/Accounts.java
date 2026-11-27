package dev.paylab;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class Accounts {
    record Profile(UUID id, String fullName, String email, String role, Instant createdAt) {}
    private record Credentials(UUID id, String passwordHash) {}

    private final JdbcClient db;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    Accounts(JdbcClient db, PasswordEncoder passwords) {
        this.db = db;
        this.passwords = passwords;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    Optional<Profile> create(String name, String email, String password) {
        return db.sql("""
                INSERT INTO users (full_name, email, password_hash)
                VALUES (:name, :email, :hash)
                ON CONFLICT (email) DO NOTHING
                RETURNING id, full_name, email, role, created_at
                """)
                .param("name", name.strip())
                .param("email", normalize(email))
                .param("hash", passwords.encode(password))
                .query((row, index) -> new Profile(row.getObject("id", UUID.class),
                        row.getString("full_name"), row.getString("email"), row.getString("role"),
                        row.getObject("created_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    Optional<UUID> authenticate(String email, String password) {
        var credentials = db.sql("SELECT id, password_hash FROM users WHERE email = :email")
                .param("email", normalize(email))
                .query((row, index) -> new Credentials(row.getObject("id", UUID.class), row.getString("password_hash")))
                .optional();
        String hash = credentials.map(Credentials::passwordHash).orElse(dummyHash);
        return passwords.matches(password, hash) ? credentials.map(Credentials::id) : Optional.empty();
    }

    Optional<Profile> find(UUID id) {
        return db.sql("SELECT id, full_name, email, role, created_at FROM users WHERE id = :id")
                .param("id", id)
                .query((row, index) -> new Profile(row.getObject("id", UUID.class),
                        row.getString("full_name"), row.getString("email"), row.getString("role"),
                        row.getObject("created_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
