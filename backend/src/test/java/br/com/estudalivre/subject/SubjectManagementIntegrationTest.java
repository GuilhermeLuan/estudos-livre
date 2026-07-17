package br.com.estudalivre.subject;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.testing.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SubjectManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserRepository identityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        cleanDatabase();
    }

    @AfterEach
    void clearDataAfterTest() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void authenticatedUserCreatesASubjectAndFindsItInTheActiveCatalog() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");

        mockMvc.perform(withSpaCsrf(post("/api/subjects")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Língua Portuguesa  "}
                                """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Língua Portuguesa"))
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/subjects").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Língua Portuguesa"));
    }

    @Test
    void subjectNameMustContainBetweenOneAndOneHundredTwentyCharacters() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");

        mockMvc.perform(withSpaCsrf(post("/api/subjects")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.name").value("Informe o nome da matéria."));

        mockMvc.perform(withSpaCsrf(post("/api/subjects")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(121) + "\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name")
                        .value("O nome deve ter no máximo 120 caracteres."));
    }

    @Test
    void subjectMutationKeepsTheSpaCsrfCookieAvailable() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");

        createSubject(principal, "Língua Portuguesa");

        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void ownerRenamesASubjectAndReadsTheUpdatedRepresentation() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Português");

        mockMvc.perform(withSpaCsrf(put("/api/subjects/{id}", subjectId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Língua Portuguesa"}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Língua Portuguesa"));

        mockMvc.perform(get("/api/subjects/{id}", subjectId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.name").value("Língua Portuguesa"));
    }

    @Test
    void ownerArchivesAndRestoresASubjectWithoutDeletingIt() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Raciocínio Lógico");

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/archive", subjectId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/archive", subjectId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/subjects").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/subjects").param("status", "archived").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(subjectId.toString()))
                .andExpect(jsonPath("$[0].archived").value(true));
        mockMvc.perform(get("/api/subjects/{id}", subjectId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/restore", subjectId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/restore", subjectId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/subjects").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(subjectId.toString()));
    }

    @Test
    void subjectsAreInvisibleAndUnmodifiableToOtherUsers() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID ownedSubjectId = createSubject(owner, "Direito Constitucional");
        UUID otherSubjectId = createSubject(otherUser, "Informática");

        mockMvc.perform(get("/api/subjects").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ownedSubjectId.toString()));

        mockMvc.perform(get("/api/subjects/{id}", otherSubjectId).with(user(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));
        mockMvc.perform(withSpaCsrf(put("/api/subjects/{id}", otherSubjectId)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nome alterado\"}")))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/archive", otherSubjectId)
                        .with(user(owner))))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/restore", otherSubjectId)
                        .with(user(owner))))
                .andExpect(status().isNotFound());
    }

    private IdentityPrincipal createUser(String email) {
        UUID id = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode("uma frase senha segura");
        identityUserRepository.create(id, email, passwordHash, "America/Sao_Paulo");
        return new IdentityPrincipal(id, email, passwordHash, "America/Sao_Paulo");
    }

    private UUID createSubject(IdentityPrincipal principal, String name) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/subjects")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private MockHttpServletRequestBuilder withSpaCsrf(MockHttpServletRequestBuilder request) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
    }
}
