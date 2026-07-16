package br.com.estudalivre.identity.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenRepository {

    private final JdbcClient jdbcClient;

    public PasswordResetTokenRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<PasswordResetToken> lockUsable(String tokenHash, Instant now) {
        return jdbcClient.sql("""
                        SELECT token.token_hash, token.user_id, identity.email
                        FROM password_reset_token token
                        JOIN identity_user identity ON identity.id = token.user_id
                        WHERE token.token_hash = :tokenHash
                          AND token.expires_at > :now
                        FOR UPDATE OF token
                        """)
                .param("tokenHash", tokenHash)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .query((resultSet, rowNumber) -> new PasswordResetToken(
                        resultSet.getString("token_hash").strip(),
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("email")))
                .optional();
    }

    public void delete(String tokenHash) {
        jdbcClient.sql("DELETE FROM password_reset_token WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .update();
    }

    public void deleteByUserId(UUID userId) {
        jdbcClient.sql("DELETE FROM password_reset_token WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    public void create(String tokenHash, UUID userId, Instant createdAt, Instant expiresAt) {
        jdbcClient.sql("""
                        INSERT INTO password_reset_token (token_hash, user_id, created_at, expires_at)
                        VALUES (:tokenHash, :userId, :createdAt, :expiresAt)
                        """)
                .param("tokenHash", tokenHash)
                .param("userId", userId)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
                .update();
    }
}
