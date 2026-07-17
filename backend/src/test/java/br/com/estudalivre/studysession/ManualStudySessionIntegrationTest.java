package br.com.estudalivre.studysession;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ManualStudySessionIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM study_cycle_run");
        jdbcTemplate.update("DELETE FROM study_cycle_suggestion_subject");
        jdbcTemplate.update("DELETE FROM study_cycle_stage");
        jdbcTemplate.update("DELETE FROM study_cycle");
        jdbcTemplate.update("DELETE FROM content");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void createsAFinishedManualSessionInTheOwnersTimeZoneAndListsItImmediately() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com", "Pacific/Auckland");
        UUID subjectId = createSubject(principal, "Direito Constitucional");

        String created = mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAtLocal":"2026-07-17T20:30:00",
                                  "effectiveSeconds":3600,
                                  "subjectId":"%s",
                                  "notes":"Revisão dos direitos fundamentais"
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.startedAt").value("2026-07-17T08:30:00Z"))
                .andExpect(jsonPath("$.finishedAt").value("2026-07-17T09:30:00Z"))
                .andExpect(jsonPath("$.measuredSeconds").value(3600))
                .andExpect(jsonPath("$.effectiveSeconds").value(3600))
                .andExpect(jsonPath("$.notes").value("Revisão dos direitos fundamentais"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID sessionId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/study-sessions/history").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].origin").value("MANUAL"))
                .andExpect(jsonPath("$[0].notes").value("Revisão dos direitos fundamentais"));
    }

    @Test
    void distributesAManualSessionAcrossTheActiveRunsStagesForTheSameSubject() throws Exception {
        IdentityPrincipal principal = createUser("ciclo@example.com", "America/Sao_Paulo");
        UUID mathematicsId = createSubject(principal, "Matemática");
        UUID portugueseId = createSubject(principal, "Língua Portuguesa");
        UUID cycleId = createActiveCycle(
                principal,
                "Ciclo intercalado",
                List.of(
                        new StageInput(mathematicsId, 30),
                        new StageInput(portugueseId, 20),
                        new StageInput(mathematicsId, 30)));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAtLocal":"2026-07-17T08:00:00",
                                  "effectiveSeconds":4800,
                                  "subjectId":"%s"
                                }
                                """.formatted(mathematicsId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.credits.length()").value(2))
                .andExpect(jsonPath("$.credits[0].stagePosition").value(1))
                .andExpect(jsonPath("$.credits[0].creditedSeconds").value(1800))
                .andExpect(jsonPath("$.credits[1].stagePosition").value(3))
                .andExpect(jsonPath("$.credits[1].creditedSeconds").value(1800));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].creditedSeconds").value(1800))
                .andExpect(jsonPath("$.stages[1].creditedSeconds").value(0))
                .andExpect(jsonPath("$.stages[2].creditedSeconds").value(1800))
                .andExpect(jsonPath("$.currentRun.currentStagePosition").value(2));
    }

    @Test
    void keepsManualSessionsAndTheirReferencesInsideTheOwnersBoundary() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com", "America/Sao_Paulo");
        IdentityPrincipal other = createUser("outra@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(owner, "Administração Pública");

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAtLocal":"2026-07-17T08:00:00",
                                  "effectiveSeconds":1800,
                                  "subjectId":"%s"
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAtLocal":"2026-07-17T08:00:00",
                                  "effectiveSeconds":1800,
                                  "subjectId":"%s"
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/study-sessions/history").with(user(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAManualSessionWithoutPositiveDuration() throws Exception {
        IdentityPrincipal principal = createUser("validacao@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Penal");

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAtLocal":"2026-07-17T08:00:00",
                                  "effectiveSeconds":0,
                                  "subjectId":"%s"
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    private IdentityPrincipal createUser(String email, String timeZone) {
        UUID id = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode("uma frase senha segura");
        identityUserRepository.create(id, email, passwordHash, timeZone);
        return new IdentityPrincipal(id, email, passwordHash, timeZone);
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

    private UUID createActiveCycle(
            IdentityPrincipal principal,
            String name,
            List<StageInput> stages) throws Exception {
        String created = mockMvc.perform(withSpaCsrf(post("/api/study-cycles")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID cycleId = UUID.fromString(JsonPath.read(created, "$.id"));
        String stagesJson = stages.stream()
                .map(stage -> "{\"subjectId\":\"%s\",\"targetMinutes\":%d}"
                        .formatted(stage.subjectId(), stage.targetMinutes()))
                .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"stages\":[%s]}".formatted(name, stagesJson))))
                .andExpect(status().isOk());
        mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                        .with(user(principal))))
                .andExpect(status().isOk());
        return cycleId;
    }

    private record StageInput(UUID subjectId, int targetMinutes) {
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
