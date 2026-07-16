package br.com.estudalivre.identity.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IdentityUserRepository {

    private final JdbcClient jdbcClient;

    public IdentityUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean existsAny() {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM identity_user)")
                .query(Boolean.class)
                .single();
    }

    public void lockBootstrap() {
        jdbcClient.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtext('identity-bootstrap'))")
                .query(Integer.class)
                .single();
    }

    public void create(UUID id, String email, String passwordHash, String timeZone) {
        jdbcClient.sql("""
                        INSERT INTO identity_user (id, email, password_hash, time_zone)
                        VALUES (:id, :email, :passwordHash, :timeZone)
                        """)
                .param("id", id)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .param("timeZone", timeZone)
                .update();
    }

    public Optional<IdentityUser> findByEmail(String email) {
        return jdbcClient.sql("""
                        SELECT id, email, password_hash, time_zone
                        FROM identity_user
                        WHERE email = :email
                        """)
                .param("email", email)
                .query((resultSet, rowNumber) -> new IdentityUser(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("time_zone")))
                .optional();
    }

    public Optional<IdentityUser> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, email, password_hash, time_zone
                        FROM identity_user
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((resultSet, rowNumber) -> new IdentityUser(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("time_zone")))
                .optional();
    }

    public void updatePassword(UUID id, String passwordHash) {
        jdbcClient.sql("""
                        UPDATE identity_user
                        SET password_hash = :passwordHash, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("passwordHash", passwordHash)
                .update();
    }
}
