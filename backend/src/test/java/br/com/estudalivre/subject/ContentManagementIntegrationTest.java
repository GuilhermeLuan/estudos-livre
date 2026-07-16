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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ContentManagementIntegrationTest {

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

    @Test
    void ownerCreatesContentAndFindsItOnlyUnderTheSelectedSubject() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID constitutional = createSubject(principal, "Direito Constitucional");
        UUID administrative = createSubject(principal, "Direito Administrativo");

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", constitutional)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Direitos   Fundamentais  "}
                                """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.subjectId").value(constitutional.toString()))
                .andExpect(jsonPath("$.name").value("Direitos Fundamentais"))
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents", constitutional)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Direitos Fundamentais"));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents", administrative)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void activeContentNamesAreUniquePerSubjectAfterNormalization() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID constitutional = createSubject(principal, "Direito Constitucional");
        UUID administrative = createSubject(principal, "Direito Administrativo");

        createContent(principal, constitutional, "Direitos Fundamentais");

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", constitutional)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  DIREITOS   fundamentais "}
                                """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conteúdo duplicado"));

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", administrative)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Direitos Fundamentais"}
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    void ownerRenamesArchivesAndRestoresContentWithoutDeletingIt() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Tributário");
        UUID contentId = createContent(principal, subjectId, "Obrigação Tributária");

        mockMvc.perform(withSpaCsrf(put("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Crédito Tributário"}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Crédito Tributário"));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Crédito Tributário"));

        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/archive", subjectId, contentId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents", subjectId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/subjects/{subjectId}/contents", subjectId)
                        .param("status", "archived")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(contentId.toString()));

        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/restore", subjectId, contentId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void contentsAreInvisibleAndUnmodifiableOutsideTheirOwnerAndSubject() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID ownedSubject = createSubject(owner, "Direito Penal");
        UUID otherOwnedSubject = createSubject(owner, "Processo Penal");
        UUID contentId = createContent(owner, ownedSubject, "Teoria do Crime");

        mockMvc.perform(get("/api/subjects/{subjectId}/contents", ownedSubject).with(user(otherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", ownedSubject)
                        .with(user(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tentativa"}
                                """)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(
                        "/api/subjects/{subjectId}/contents/{contentId}", ownedSubject, contentId)
                        .with(user(otherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conteúdo não encontrado"));
        mockMvc.perform(withSpaCsrf(put(
                        "/api/subjects/{subjectId}/contents/{contentId}", ownedSubject, contentId)
                        .with(user(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nome alterado"}
                                """)))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/archive", ownedSubject, contentId)
                        .with(user(otherUser))))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/restore", ownedSubject, contentId)
                        .with(user(otherUser))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/subjects/{subjectId}/contents/{contentId}", otherOwnedSubject, contentId)
                        .with(user(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoringArchivedContentRejectsAnActiveNormalizedDuplicate() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID archivedContent = createContent(principal, subjectId, "Licitações e Contratos");

        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/archive", subjectId, archivedContent)
                        .with(user(principal))))
                .andExpect(status().isOk());
        UUID activeContent = createContent(principal, subjectId, "LICITAÇÕES   E CONTRATOS");

        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/restore", subjectId, archivedContent)
                        .with(user(principal))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conteúdo duplicado"));

        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/archive", subjectId, activeContent)
                        .with(user(principal))))
                .andExpect(status().isOk());
        mockMvc.perform(withSpaCsrf(post(
                        "/api/subjects/{subjectId}/contents/{contentId}/restore", subjectId, archivedContent)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void contentNameAndCatalogStatusMustBeValid() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Língua Portuguesa");

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", subjectId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Informe o nome do conteúdo."));
        mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", subjectId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(121) + "\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name")
                        .value("O nome deve ter no máximo 120 caracteres."));
        mockMvc.perform(get("/api/subjects/{subjectId}/contents", subjectId)
                        .param("status", "unknown")
                        .with(user(principal)))
                .andExpect(status().isBadRequest());
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM content");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM identity_user");
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

    private UUID createContent(IdentityPrincipal principal, UUID subjectId, String name) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", subjectId)
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
