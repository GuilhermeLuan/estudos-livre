package br.com.estudalivre.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.testing.IntegrationTest;
import br.com.estudalivre.identity.service.PasswordResetLinkService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordResetLinkService passwordResetLinkService;

    @BeforeEach
    void clearIdentityData() {
        jdbcTemplate.update("DELETE FROM password_reset_token");
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void resetLinkChangesThePasswordOnceAndInvalidatesEverySession() throws Exception {
        Cookie bootstrapCsrf = csrfCookie();
        bootstrap("pessoa@example.com", bootstrapCsrf);
        Cookie firstSession = login("uma frase senha segura", csrfCookie());
        Cookie secondSession = login("uma frase senha segura", csrfCookie());

        String token = "token-operacional-de-256-bits-simulado";
        UUID userId = jdbcTemplate.queryForObject(
                "SELECT id FROM identity_user WHERE email = ?", UUID.class, "pessoa@example.com");
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("""
                        INSERT INTO password_reset_token (token_hash, user_id, created_at, expires_at)
                        VALUES (?, ?, ?, ?)
                        """,
                sha256(token), userId,
                createdAt.atOffset(ZoneOffset.UTC),
                createdAt.plus(30, ChronoUnit.MINUTES).atOffset(ZoneOffset.UTC));

        Cookie resetCsrf = csrfCookie();
        reset(token, "uma nova frase senha segura", resetCsrf)
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(firstSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").cookie(secondSession))
                .andExpect(status().isUnauthorized());
        loginExpecting("uma frase senha segura", status().isUnauthorized());
        loginExpecting("uma nova frase senha segura", status().isNoContent());

        reset(token, "outra frase senha segura", csrfCookie())
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedLinkStoresOnlyAHashForExactlyThirtyMinutesAndReplacesOlderLinks() throws Exception {
        bootstrap("pessoa@example.com", csrfCookie());

        String firstLink = passwordResetLinkService.generateLink(" PESSOA@example.com ");
        String secondLink = passwordResetLinkService.generateLink("pessoa@example.com");

        String rawToken = secondLink.substring(secondLink.indexOf("token=") + "token=".length());
        var persisted = jdbcTemplate.queryForMap("""
                SELECT token_hash, created_at, expires_at
                FROM password_reset_token
                """);
        String storedHash = ((String) persisted.get("token_hash")).strip();
        Instant createdAt = ((java.sql.Timestamp) persisted.get("created_at")).toInstant();
        Instant expiresAt = ((java.sql.Timestamp) persisted.get("expires_at")).toInstant();

        org.assertj.core.api.Assertions.assertThat(firstLink)
                .startsWith("http://localhost:8080/redefinir-senha?token=")
                .isNotEqualTo(secondLink);
        org.assertj.core.api.Assertions.assertThat(storedHash)
                .isEqualTo(sha256(rawToken))
                .doesNotContain(rawToken);
        org.assertj.core.api.Assertions.assertThat(Duration.between(createdAt, expiresAt))
                .isEqualTo(Duration.ofMinutes(30));
        org.assertj.core.api.Assertions.assertThat(
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_token", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void concurrentResetAttemptsConsumeTheTokenExactlyOnce() throws Exception {
        bootstrap("pessoa@example.com", csrfCookie());
        String link = passwordResetLinkService.generateLink("pessoa@example.com");
        String token = link.substring(link.indexOf("token=") + "token=".length());
        Cookie firstCsrf = csrfCookie();
        Cookie secondCsrf = csrfCookie();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return reset(token, "primeira nova senha segura", firstCsrf)
                        .andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return reset(token, "segunda nova senha segura", secondCsrf)
                        .andReturn().getResponse().getStatus();
            });

            start.countDown();

            org.assertj.core.api.Assertions.assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(204, 400);
        }
    }

    @Test
    void expiredAndUnknownTokensReceiveTheSamePublicResponse() throws Exception {
        bootstrap("pessoa@example.com", csrfCookie());
        String link = passwordResetLinkService.generateLink("pessoa@example.com");
        String token = link.substring(link.indexOf("token=") + "token=".length());
        jdbcTemplate.update("""
                UPDATE password_reset_token
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '30 minutes',
                    expires_at = CURRENT_TIMESTAMP
                """);

        Cookie expiredCsrf = csrfCookie();
        String expiredResponse = reset(token, "uma nova frase senha segura", expiredCsrf)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        Cookie unknownCsrf = csrfCookie();
        String unknownResponse = reset("token-que-nao-existe", "uma nova frase senha segura", unknownCsrf)
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(unknownResponse).isEqualTo(expiredResponse);
    }

    @Test
    void passwordResetRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-qualquer",
                                  "newPassword": "uma nova frase senha segura"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private void bootstrap(String email, Cookie csrf) throws Exception {
        mockMvc.perform(post("/api/auth/bootstrap")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "uma frase senha segura",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private Cookie login(String password, Cookie csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn().getResponse().getCookie("SESSION");
    }

    private void loginExpecting(String password, org.springframework.test.web.servlet.ResultMatcher statusMatcher)
            throws Exception {
        Cookie csrf = csrfCookie();
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", password))
                .andExpect(statusMatcher);
    }

    private org.springframework.test.web.servlet.ResultActions reset(
            String token, String newPassword, Cookie csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/password/reset")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s","newPassword":"%s"}
                        """.formatted(token, newPassword)));
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
