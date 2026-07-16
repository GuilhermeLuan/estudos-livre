package br.com.estudalivre.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.testing.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@TestPropertySource(properties = "app.registration-enabled=true")
class PublicRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void bootstrapStatusAnnouncesThatPublicRegistrationIsEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled").value(true));
    }

    @Test
    void visitorCanRegisterAndThenAuthenticate() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  PESSOA@example.com ",
                                  "password": "uma frase senha segura",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "pessoa@example.com")
                        .param("password", "uma frase senha segura"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    void registrationRejectsAnEmailThatAlreadyExists() throws Exception {
        Cookie csrf = csrfCookie();
        register("pessoa@example.com", csrf).andExpect(status().isCreated());

        register(" PESSOA@example.com ", csrf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));
    }

    @Test
    void registrationRequiresCsrfAndValidAccountData() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pessoa@example.com",
                                  "password": "uma frase senha segura",
                                  "timeZone": "America/Sao_Paulo"
                                }
                                """))
                .andExpect(status().isForbidden());

        Cookie csrf = csrfCookie();
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nao-e-um-email",
                                  "password": "curta",
                                  "timeZone": "fuso-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String email, Cookie csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "uma frase senha segura",
                          "timeZone": "America/Sao_Paulo"
                        }
                        """.formatted(email)));
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
