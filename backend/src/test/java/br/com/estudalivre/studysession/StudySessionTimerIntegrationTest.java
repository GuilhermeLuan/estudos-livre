package br.com.estudalivre.studysession;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
class StudySessionTimerIntegrationTest {

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
    void startsFromTheCurrentCycleStageWithOptionalContentAndRecoversIt() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Constitucional");
        UUID contentId = createContent(principal, subjectId, "Controle de constitucionalidade");
        UUID cycleId = createConfiguredAndActiveCycle(principal, subjectId, "Reta final");

        String created = mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":"CYCLE",
                                  "cycleId":"%s",
                                  "contentId":"%s"
                                }
                                """.formatted(cycleId, contentId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("CYCLE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subject.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.subject.name").value("Direito Constitucional"))
                .andExpect(jsonPath("$.content.id").value(contentId.toString()))
                .andExpect(jsonPath("$.cycle.id").value(cycleId.toString()))
                .andExpect(jsonPath("$.cycle.runNumber").value(1))
                .andExpect(jsonPath("$.cycle.stagePosition").value(1))
                .andExpect(jsonPath("$.cycle.targetMinutes").value(60))
                .andExpect(jsonPath("$.measuredSeconds").isNumber())
                .andExpect(jsonPath("$.serverNow").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID sessionId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/study-sessions/current").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.content.name").value("Controle de constitucionalidade"));
    }

    @Test
    void startsAFreeSessionAndAllowsNoContent() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Língua Portuguesa");

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"FREE","subjectId":"%s"}
                                """.formatted(subjectId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("FREE"))
                .andExpect(jsonPath("$.subject.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.cycle").isEmpty());
    }

    @Test
    void pauseAndResumeCountOnlyActiveTimerSegments() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Raciocínio Lógico");
        String created = startFreeSession(principal, subjectId);
        UUID sessionId = UUID.fromString(JsonPath.read(created, "$.id"));

        jdbcTemplate.update("""
                UPDATE study_session_timer_segment
                SET started_at = CURRENT_TIMESTAMP - INTERVAL '120 seconds'
                WHERE session_id = ? AND ended_at IS NULL
                """, sessionId);

        String paused = mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/pause", sessionId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int pausedSeconds = JsonPath.read(paused, "$.measuredSeconds");
        assertThat(pausedSeconds).isBetween(120, 122);

        mockMvc.perform(get("/api/study-sessions/current").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measuredSeconds").value(pausedSeconds));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/resume", sessionId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        jdbcTemplate.update("""
                UPDATE study_session_timer_segment
                SET started_at = CURRENT_TIMESTAMP - INTERVAL '20 seconds'
                WHERE session_id = ? AND ended_at IS NULL
                """, sessionId);

        String resumed = mockMvc.perform(get("/api/study-sessions/current").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int resumedSeconds = JsonPath.read(resumed, "$.measuredSeconds");
        assertThat(resumedSeconds).isBetween(pausedSeconds + 20, pausedSeconds + 22);
    }

    @Test
    void rejectsInvalidTransitionsAndHidesAnotherUsersSession() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID subjectId = createSubject(owner, "Informática");
        String created = startFreeSession(owner, subjectId);
        UUID sessionId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/study-sessions/current").with(user(otherUser)))
                .andExpect(status().isNoContent());
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/pause", sessionId)
                        .with(user(otherUser))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sessão de estudo não encontrada"));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/resume", sessionId)
                        .with(user(owner))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Transição de sessão inválida"));
    }

    @Test
    void rejectsInactiveCyclesAndContentsOutsideTheSelectedSubject() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID otherSubjectId = createSubject(principal, "Administração Pública");
        UUID otherContentId = createContent(principal, otherSubjectId, "Gestão por processos");
        UUID inactiveCycleId = createConfiguredCycle(principal, subjectId, "Ciclo guardado");

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"CYCLE","cycleId":"%s"}
                                """.formatted(inactiveCycleId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Ciclo não está ativo"));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":"FREE",
                                  "subjectId":"%s",
                                  "contentId":"%s"
                                }
                                """.formatted(subjectId, otherContentId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conteúdo não encontrado"));
    }

    @Test
    void concurrentStartsLeaveOnlyOneOpenSessionPerUser() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Contabilidade");
        Cookie csrfCookie = csrfCookie();
        int requests = 8;
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests);

        try {
            List<Future<Integer>> responses = IntStream.range(0, requests)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return mockMvc.perform(post("/api/study-sessions")
                                        .with(user(principal))
                                        .cookie(csrfCookie)
                                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"origin":"FREE","subjectId":"%s"}
                                                """.formatted(subjectId)))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : responses) {
                statuses.add(response.get(10, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409, 409, 409, 409, 409, 409, 409);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM study_session
                WHERE owner_id = ? AND status IN ('ACTIVE', 'PAUSED')
                """, Integer.class, principal.id())).isEqualTo(1);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_session_timer_segment");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM study_cycle_run");
        jdbcTemplate.update("DELETE FROM study_cycle_suggestion_subject");
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

    private UUID createConfiguredAndActiveCycle(
            IdentityPrincipal principal,
            UUID subjectId,
            String name) throws Exception {
        UUID cycleId = createConfiguredCycle(principal, subjectId, name);
        mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                        .with(user(principal))))
                .andExpect(status().isOk());
        return cycleId;
    }

    private UUID createConfiguredCycle(IdentityPrincipal principal, UUID subjectId, String name) throws Exception {
        String created = mockMvc.perform(withSpaCsrf(post("/api/study-cycles")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID cycleId = UUID.fromString(JsonPath.read(created, "$.id"));
        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "stages":[{"subjectId":"%s","targetMinutes":60}]
                                }
                                """.formatted(name, subjectId))))
                .andExpect(status().isOk());
        return cycleId;
    }

    private String startFreeSession(IdentityPrincipal principal, UUID subjectId) throws Exception {
        return mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"FREE","subjectId":"%s"}
                                """.formatted(subjectId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private MockHttpServletRequestBuilder withSpaCsrf(MockHttpServletRequestBuilder request) throws Exception {
        Cookie csrfCookie = csrfCookie();
        return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
    }
}
