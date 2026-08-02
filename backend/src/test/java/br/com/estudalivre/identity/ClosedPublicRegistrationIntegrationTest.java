package br.com.estudalivre.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.testing.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
@TestPropertySource(properties = "app.registration-enabled=false")
class ClosedPublicRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUserRepository identityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void closedRegistrationRejectsNewAccountsButKeepsExistingLoginAvailable() throws Exception {
        String password = "uma frase senha segura";
        String existingEmail = "persisted-" + UUID.randomUUID() + "@example.com";
        String newEmail = "new-" + UUID.randomUUID() + "@example.com";
        String timeZone = "America/Sao_Paulo";
        identityUserRepository.create(
                UUID.randomUUID(),
                existingEmail,
                passwordEncoder.encode(password),
                timeZone);

        MvcResult bootstrapStatus = mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled").value(false))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrf = bootstrapStatus.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"%s",
                                  "timeZone":"%s"
                                }
                                """.formatted(newEmail, password, timeZone)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Cadastro indisponível"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", existingEmail)
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();
        Cookie session = login.getResponse().getCookie("SESSION");

        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(existingEmail))
                .andExpect(jsonPath("$.timeZone").value(timeZone));
    }
}
