package br.com.estudalivre.studycycle;

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
class CustomStudyCycleIntegrationTest {

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
    void authenticatedUserCreatesAndListsMultipleNamedCustomCycleDrafts() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");

        createCycle(principal, "  Ciclo pós-edital  ");
        createCycle(principal, "Ciclo de manutenção");

        mockMvc.perform(get("/api/study-cycles").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ciclo de manutenção"))
                .andExpect(jsonPath("$[0].mode").value("CUSTOM"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].totalMinutes").value(0))
                .andExpect(jsonPath("$[0].activatable").value(false))
                .andExpect(jsonPath("$[1].name").value("Ciclo pós-edital"));
    }

    @Test
    void ownerSavesOrderedStagesAndReadsTheRecalculatedCycleTotal() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID portugueseId = createSubject(principal, "Língua Portuguesa");
        UUID mathId = createSubject(principal, "Matemática");
        UUID cycleId = createCycle(principal, "Ciclo inicial");

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo pós-edital",
                                  "stages":[
                                    {"subjectId":"%s","targetMinutes":90},
                                    {"subjectId":"%s","targetMinutes":60},
                                    {"subjectId":"%s","targetMinutes":45}
                                  ]
                                }
                                """.formatted(mathId, portugueseId, mathId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ciclo pós-edital"))
                .andExpect(jsonPath("$.totalMinutes").value(195))
                .andExpect(jsonPath("$.activatable").value(true))
                .andExpect(jsonPath("$.stages.length()").value(3))
                .andExpect(jsonPath("$.stages[0].position").value(1))
                .andExpect(jsonPath("$.stages[0].subjectName").value("Matemática"))
                .andExpect(jsonPath("$.stages[1].position").value(2))
                .andExpect(jsonPath("$.stages[1].subjectName").value("Língua Portuguesa"))
                .andExpect(jsonPath("$.stages[2].position").value(3))
                .andExpect(jsonPath("$.stages[2].targetMinutes").value(45));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMinutes").value(195))
                .andExpect(jsonPath("$.stages[0].subjectId").value(mathId.toString()))
                .andExpect(jsonPath("$.stages[1].subjectId").value(portugueseId.toString()))
                .andExpect(jsonPath("$.stages[2].subjectId").value(mathId.toString()));
    }

    @Test
    void stageDurationMustBePositiveAndDivisibleByFiveWhileLongBlocksOnlyWarn() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Constitucional");
        UUID cycleId = createCycle(principal, "Ciclo jurídico");

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ciclo jurídico","stages":[null]}
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo jurídico",
                                  "stages":[{"subjectId":"%s","targetMinutes":62}]
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("A duração de cada etapa deve ser múltipla de 5 minutos."));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMinutes").value(0))
                .andExpect(jsonPath("$.activatable").value(false));

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo jurídico",
                                  "stages":[{"subjectId":"%s","targetMinutes":240}]
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMinutes").value(240))
                .andExpect(jsonPath("$.stages[0].longBlockWarning").value(true));
    }

    @Test
    void stagesRejectArchivedSubjectsAndSubjectsOwnedByAnotherUserWithoutLeakingThem() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID ownerSubjectId = createSubject(owner, "Administração Pública");
        UUID otherCycleId = createCycle(otherUser, "Ciclo alheio");

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", otherCycleId)
                        .with(user(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo alheio",
                                  "stages":[{"subjectId":"%s","targetMinutes":60}]
                                }
                                """.formatted(ownerSubjectId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(withSpaCsrf(post("/api/subjects/{id}/archive", ownerSubjectId)
                        .with(user(owner))))
                .andExpect(status().isOk());
        UUID ownerCycleId = createCycle(owner, "Ciclo arquivado");

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", ownerCycleId)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo arquivado",
                                  "stages":[{"subjectId":"%s","targetMinutes":60}]
                                }
                                """.formatted(ownerSubjectId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(get("/api/study-cycles/{id}", ownerCycleId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages.length()").value(0));
    }

    @Test
    void cyclesAreInvisibleAndUnmodifiableToOtherUsers() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID ownerCycleId = createCycle(owner, "Ciclo privado");
        UUID otherCycleId = createCycle(otherUser, "Outro ciclo");

        mockMvc.perform(get("/api/study-cycles").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ownerCycleId.toString()));

        mockMvc.perform(get("/api/study-cycles/{id}", otherCycleId).with(user(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", otherCycleId)
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tentativa de alteração","stages":[]}
                                """)))
                .andExpect(status().isNotFound());
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_cycle_stage");
        jdbcTemplate.update("DELETE FROM study_cycle");
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

    private UUID createCycle(IdentityPrincipal principal, String name) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/study-cycles")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
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
