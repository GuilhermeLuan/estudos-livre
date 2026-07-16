package br.com.estudalivre.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.testing.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class IdentityAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserRepository identityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void emptyInstallationRequiresBootstrap() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(true));
    }

    @Test
    void publicAuthenticationResponseIssuesTheSpaCsrfCookie() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(cookie().sameSite("XSRF-TOKEN", "Lax"));
    }

    @Test
    void bootstrapRejectsAnInvalidPasswordWithProblemDetails() throws Exception {
        Cookie csrfCookie = csrfCookie();
        mockMvc.perform(post("/api/auth/bootstrap")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pessoa@example.com",
                                  "password": "curta",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getContentType())
                        .startsWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void bootstrapRejectsAnInvalidIanaTimeZone() throws Exception {
        bootstrapFirstAccount("pessoa@example.com", "Sao Paulo")
                .andExpect(status().isBadRequest())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getContentType())
                        .startsWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("O fuso horário deve ser um identificador IANA válido."));
    }

    @Test
    void anonymousUserCannotReadTheCurrentIdentity() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pessoa@example.com",
                                  "password": "uma frase senha segura",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstVisitorCanCreateTheInitialAccount() throws Exception {
        bootstrapFirstAccount("  PRIMEIRA@Example.com ", "America/Sao_Paulo")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(false));
    }

    @Test
    void registeredUserCanLoginAndReadTheCurrentIdentity() throws Exception {
        Cookie csrfCookie = csrfCookie();

        bootstrapFirstAccount("pessoa@example.com", "America/Sao_Paulo", csrfCookie)
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", "uma frase senha segura"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(login.getResponse().getCookie("SESSION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pessoa@example.com"))
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"));
    }

    @Test
    void loginRejectsInvalidCredentialsWithoutRevealingTheirCause() throws Exception {
        Cookie csrfCookie = csrfCookie();
        bootstrapFirstAccount("pessoa@example.com", "America/Sao_Paulo", csrfCookie)
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", "senha completamente errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getErrorMessage())
                        .isEqualTo("Credenciais inválidas."));
    }

    @Test
    void persistedSecurityContextDoesNotRetainThePasswordHash() throws Exception {
        Cookie csrfCookie = csrfCookie();
        bootstrapFirstAccount("pessoa@example.com", "America/Sao_Paulo", csrfCookie)
                .andExpect(status().isCreated());

        login("pessoa@example.com", csrfCookie);

        Boolean containsPasswordHash = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM spring_session_attributes
                    WHERE POSITION(convert_to('{bcrypt}', 'UTF8') IN attribute_bytes) > 0
                )
                """, Boolean.class);
        org.assertj.core.api.Assertions.assertThat(containsPasswordHash).isFalse();
    }

    @Test
    void httpsLoginIssuesASecureSessionCookie() throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/auth/bootstrap-status").secure(true))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/bootstrap")
                        .secure(true)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pessoa@example.com",
                                  "password": "uma frase senha segura",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .secure(true)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", "uma frase senha segura"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly("SESSION", true))
                .andExpect(cookie().secure("SESSION", true))
                .andExpect(cookie().sameSite("SESSION", "Lax"));
    }

    @Test
    void authenticatedSessionsKeepUsersIsolated() throws Exception {
        Cookie firstCsrf = csrfCookie();
        Cookie secondCsrf = csrfCookie();

        bootstrapFirstAccount("primeira@example.com", "America/Sao_Paulo", firstCsrf)
                .andExpect(status().isCreated());
        identityUserRepository.create(
                UUID.randomUUID(),
                "segunda@example.com",
                passwordEncoder.encode("uma frase senha segura"),
                "America/Recife");

        Cookie firstSession = login("primeira@example.com", firstCsrf)
                .getResponse().getCookie("SESSION");
        Cookie secondSession = login("segunda@example.com", secondCsrf)
                .getResponse().getCookie("SESSION");

        mockMvc.perform(get("/api/auth/me").cookie(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("primeira@example.com"))
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"));

        mockMvc.perform(get("/api/auth/me").cookie(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("segunda@example.com"))
                .andExpect(jsonPath("$.timeZone").value("America/Recife"));
    }

    @Test
    void logoutInvalidatesThePersistedSession() throws Exception {
        Cookie csrfCookie = csrfCookie();
        bootstrapFirstAccount("pessoa@example.com", "America/Sao_Paulo", csrfCookie)
                .andExpect(status().isCreated());

        MvcResult login = login("pessoa@example.com", csrfCookie);
        Cookie sessionCookie = login.getResponse().getCookie("SESSION");

        MvcResult authenticatedRequest = mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshedCsrf = authenticatedRequest.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessionCookie, refreshedCsrf)
                        .header("X-XSRF-TOKEN", refreshedCsrf.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapClosesAfterTheFirstAccount() throws Exception {
        bootstrapFirstAccount("primeira@example.com", "America/Sao_Paulo")
                .andExpect(status().isCreated());

        bootstrapFirstAccount("segunda@example.com", "America/Recife")
                .andExpect(status().isConflict())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getContentType())
                        .startsWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Cadastro inicial indisponível"));
    }

    @Test
    void concurrentBootstrapCreatesExactlyOneFirstAccount() throws Exception {
        Cookie firstCsrf = csrfCookie();
        Cookie secondCsrf = csrfCookie();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return bootstrapFirstAccount("primeira@example.com", "America/Sao_Paulo", firstCsrf)
                        .andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return bootstrapFirstAccount("segunda@example.com", "America/Recife", secondCsrf)
                        .andReturn().getResponse().getStatus();
            });

            start.countDown();

            org.assertj.core.api.Assertions.assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }

    private org.springframework.test.web.servlet.ResultActions bootstrapFirstAccount(
            String email,
            String timeZone) throws Exception {
        return bootstrapFirstAccount(email, timeZone, csrfCookie());
    }

    private org.springframework.test.web.servlet.ResultActions bootstrapFirstAccount(
            String email,
            String timeZone,
            Cookie csrfCookie) throws Exception {
        return mockMvc.perform(post("/api/auth/bootstrap")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "uma frase senha segura",
                          "timeZone": "%s"
                        }
                        """.formatted(email, timeZone)));
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
    }

    private MvcResult login(String email, Cookie csrfCookie) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", "uma frase senha segura"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();
    }
}
