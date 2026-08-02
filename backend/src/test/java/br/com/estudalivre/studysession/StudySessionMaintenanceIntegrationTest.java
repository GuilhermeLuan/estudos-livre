package br.com.estudalivre.studysession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.testing.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StudySessionMaintenanceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired IdentityUserRepository identityUserRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM review_plan");
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
    void editsACompletedSessionAndRecalculatesHistoryAndSummary() throws Exception {
        IdentityPrincipal principal = createUser("edicao@example.com", "America/Sao_Paulo");
        UUID oldSubjectId = createSubject(principal, "Direito Constitucional");
        UUID newSubjectId = createSubject(principal, "Direito Administrativo");
        UUID contentId = createContent(principal, newSubjectId, "Atos administrativos");
        UUID sessionId = createManualSession(principal, oldSubjectId, 1800);

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", sessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "startedAtLocal":"2026-07-20T09:15:00",
                                  "effectiveSeconds":2700,
                                  "subjectId":"%s",
                                  "contentId":"%s",
                                  "notes":"Correção completa",
                                  "questionsAttempted":20,
                                  "questionsCorrect":15
                                }
                                """.formatted(newSubjectId, contentId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.startedAt").value("2026-07-20T12:15:00Z"))
                .andExpect(jsonPath("$.finishedAt").value("2026-07-20T13:00:00Z"))
                .andExpect(jsonPath("$.effectiveSeconds").value(2700))
                .andExpect(jsonPath("$.subject.id").value(newSubjectId.toString()))
                .andExpect(jsonPath("$.content.id").value(contentId.toString()))
                .andExpect(jsonPath("$.notes").value("Correção completa"))
                .andExpect(jsonPath("$.exerciseResult.questionsAttempted").value(20))
                .andExpect(jsonPath("$.exerciseResult.questionsCorrect").value(15));

        mockMvc.perform(get("/api/study-sessions/history").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].version").value(1));

        mockMvc.perform(get("/api/study-sessions/summary").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].subject.id").value(newSubjectId.toString()))
                .andExpect(jsonPath("$.subjects[0].effectiveSeconds").value(2700))
                .andExpect(jsonPath("$.subjects[0].questionsAttempted").value(20))
                .andExpect(jsonPath("$.subjects[0].questionsCorrect").value(15))
                .andExpect(jsonPath("$.subjects[0].accuracyPercentage").value(75.0))
                .andExpect(jsonPath("$.contents.length()").value(1))
                .andExpect(jsonPath("$.contents[0].content.id").value(contentId.toString()))
                .andExpect(jsonPath("$.contents[0].effectiveSeconds").value(2700));
    }

    @Test
    void rebuildsTheAffectedCycleRunWhenACompletedSessionChanges() throws Exception {
        IdentityPrincipal principal = createUser("reconstrucao@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Matemática");
        UUID cycleId = createActiveCycle(principal, "Ciclo principal", subjectId, 30);
        UUID sessionId = createManualSession(principal, subjectId, 1200);

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "startedAtLocal":"2026-07-19T08:00:00",
                                  "effectiveSeconds":600,
                                  "subjectId":"%s"
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits.length()").value(1))
                .andExpect(jsonPath("$.credits[0].creditedSeconds").value(600));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].creditedSeconds").value(600));
    }

    @Test
    void rebuildsMultipleRunsWithoutReplacingTheirSnapshots() throws Exception {
        IdentityPrincipal principal = createUser("voltas@example.com", "America/Sao_Paulo");
        UUID cycleSubjectId = createSubject(principal, "Matemática");
        UUID unrelatedSubjectId = createSubject(principal, "Português");
        UUID cycleId = createActiveCycle(principal, "Ciclo histórico", cycleSubjectId, 30);
        UUID firstSessionId = createManualSessionAt(
                principal, cycleSubjectId, 1800, "2026-07-19T08:00:00");
        createManualSessionAt(principal, unrelatedSubjectId, 600, "2026-07-19T09:00:00");

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ciclo histórico","stages":[{"subjectId":"%s","targetMinutes":60}]}
                                """.formatted(cycleSubjectId))))
                .andExpect(status().isOk());

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", firstSessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "startedAtLocal":"2026-07-19T08:00:00",
                                  "effectiveSeconds":1200,
                                  "subjectId":"%s"
                                }
                                """.formatted(cycleSubjectId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/study-cycles/{id}/runs", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].status").value("ABANDONED"))
                .andExpect(jsonPath("$[0].stages[0].targetSeconds").value(1800))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(0))
                .andExpect(jsonPath("$[1].number").value(1))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].stages[0].targetSeconds").value(1800))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(1200));
    }

    @Test
    void rebuildsAgainAfterThePreviousReplayAbandonedALaterRun() throws Exception {
        IdentityPrincipal principal = createUser("segundo-replay@example.com", "America/Sao_Paulo");
        UUID cycleSubjectId = createSubject(principal, "Matemática Financeira");
        UUID unrelatedSubjectId = createSubject(principal, "Português");
        UUID cycleId = createActiveCycle(principal, "Ciclo repetível", cycleSubjectId, 30);
        UUID firstSessionId = createManualSessionAt(
                principal, cycleSubjectId, 1800, "2026-07-19T08:00:00");
        createManualSessionAt(principal, unrelatedSubjectId, 600, "2026-07-19T09:00:00");
        updateSession(principal, firstSessionId, 0, cycleSubjectId, 1200);

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", firstSessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(1, cycleSubjectId, 900))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.credits[0].creditedSeconds").value(900));

        mockMvc.perform(get("/api/study-cycles/{id}/runs", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ABANDONED"))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(0))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(900));
    }

    @Test
    void keepsAnExplicitlyAbandonedBoundaryBetweenProjectionSegments() throws Exception {
        IdentityPrincipal principal = createUser("fronteira@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Estatística");
        UUID firstCycleId = createActiveCycle(principal, "Ciclo anterior", subjectId, 30);
        UUID oldSessionId = createManualSessionAt(principal, subjectId, 900, "2026-07-19T08:00:00");
        UUID otherCycleId = createConfiguredCycle(principal, "Ciclo de interrupção", subjectId, 30);
        activateCycle(principal, otherCycleId, "ABANDON");
        activateCycle(principal, firstCycleId, "ABANDON");
        UUID laterSessionId = createManualSessionAt(principal, subjectId, 600, "2026-07-20T08:00:00");

        updateSession(principal, oldSessionId, 0, subjectId, 300);

        mockMvc.perform(get("/api/study-cycles/{id}/runs", firstCycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(600))
                .andExpect(jsonPath("$[1].number").value(1))
                .andExpect(jsonPath("$[1].status").value("ABANDONED"))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(300));
        Long laterCredit = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(credited_seconds), 0)
                FROM study_session_credit WHERE session_id = ?
                """, Long.class, laterSessionId);
        org.assertj.core.api.Assertions.assertThat(laterCredit).isEqualTo(600L);
    }

    @Test
    void keepsTheOriginalExplicitBoundaryWhenAnEarlierCompletedRunBecomesIncomplete() throws Exception {
        IdentityPrincipal principal = createUser("fronteira-completa@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Administração Geral");
        UUID firstCycleId = createActiveCycle(principal, "Ciclo com fronteira", subjectId, 30);
        UUID oldSessionId = createManualSessionAt(principal, subjectId, 1800, "2026-07-19T08:00:00");
        UUID otherCycleId = createConfiguredCycle(principal, "Ciclo intermediário", subjectId, 30);
        activateCycle(principal, otherCycleId, "ABANDON");
        activateCycle(principal, firstCycleId, "ABANDON");
        UUID laterSessionId = createManualSessionAt(principal, subjectId, 600, "2026-07-20T08:00:00");

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", oldSessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"startedAtLocal":"2026-07-19T08:00:00",
                                 "effectiveSeconds":900,"subjectId":"%s"}
                                """.formatted(subjectId))))
                .andExpect(status().isOk());
        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", laterSessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"startedAtLocal":"2026-07-20T08:00:00",
                                 "effectiveSeconds":300,"subjectId":"%s"}
                                """.formatted(subjectId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/study-cycles/{id}/runs", firstCycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].number").value(3))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(300))
                .andExpect(jsonPath("$[1].number").value(2))
                .andExpect(jsonPath("$[1].status").value("ABANDONED"))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(0))
                .andExpect(jsonPath("$[2].number").value(1))
                .andExpect(jsonPath("$[2].status").value("ABANDONED"))
                .andExpect(jsonPath("$[2].stages[0].creditedSeconds").value(900));
        UUID progressRunId = jdbcTemplate.queryForObject("""
                SELECT progress_cycle_run_id FROM study_session WHERE id = ?
                """, UUID.class, laterSessionId);
        Integer progressRunNumber = jdbcTemplate.queryForObject("""
                SELECT run_number FROM study_cycle_run WHERE id = ?
                """, Integer.class, progressRunId);
        org.assertj.core.api.Assertions.assertThat(progressRunNumber).isEqualTo(3);
    }

    @Test
    void rejectsMaintenanceWhileTheAffectedCycleHasAnOpenCycleSession() throws Exception {
        IdentityPrincipal principal = createUser("ciclo-aberto@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Economia");
        UUID cycleId = createActiveCycle(principal, "Ciclo bloqueado", subjectId, 30);
        UUID completedId = createManualSession(principal, subjectId, 600);
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"CYCLE\",\"cycleId\":\"%s\"}".formatted(cycleId))))
                .andExpect(status().isCreated());

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", completedId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(0, subjectId, 1200))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://estudalivre.local/problems/study-session-conflict"));

        mockMvc.perform(get("/api/study-sessions/history").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(0))
                .andExpect(jsonPath("$[0].effectiveSeconds").value(600));
    }

    @RepeatedTest(5)
    void serializesCreditApplicationAndProjectionRebuildForTheSameCycle() throws Exception {
        IdentityPrincipal principal = createUser("lock-projecao@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Contabilidade");
        UUID cycleId = createActiveCycle(principal, "Ciclo concorrente", subjectId, 30);
        UUID existingId = createManualSession(principal, subjectId, 900);
        Cookie csrf = csrfCookie();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> update = executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(put("/api/study-sessions/{id}", existingId)
                                .with(user(principal)).cookie(csrf)
                                .header("X-XSRF-TOKEN", csrf.getValue())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody(0, subjectId, 1200)))
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> create = executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/study-sessions/manual")
                                .with(user(principal)).cookie(csrf)
                                .header("X-XSRF-TOKEN", csrf.getValue())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"startedAtLocal":"2026-07-21T08:00:00",
                                         "effectiveSeconds":600,"subjectId":"%s"}
                                        """.formatted(subjectId)))
                        .andReturn().getResponse().getStatus();
            });
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(update.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            org.assertj.core.api.Assertions.assertThat(create.get(10, TimeUnit.SECONDS)).isEqualTo(201);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        mockMvc.perform(get("/api/study-cycles/{id}/runs", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(0))
                .andExpect(jsonPath("$[1].number").value(1))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(1800));
    }

    @Test
    void deletesACompletedSessionAndKeepsItsReviewPlanIndependent() throws Exception {
        IdentityPrincipal principal = createUser("exclusao@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Civil");
        UUID contentId = createContent(principal, subjectId, "Obrigações");
        UUID cycleId = createActiveCycle(principal, "Ciclo jurídico", subjectId, 30);
        UUID sessionId = startCycleSession(principal, cycleId, contentId);
        finishSession(principal, sessionId, 900, true);
        String plans = mockMvc.perform(get("/api/review-plans").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(plans, "$[0].id"));

        mockMvc.perform(withSpaCsrf(delete("/api/study-sessions/{id}", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/study-sessions/history").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/study-sessions/summary").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subjects.length()").value(0));
        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stages[0].creditedSeconds").value(0));
        mockMvc.perform(get("/api/review-plans/{id}", planId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.occurrences.length()").value(6));
    }

    @Test
    void deletesACompletedSessionAcrossMultipleRunsAndPreservesSnapshots() throws Exception {
        IdentityPrincipal principal = createUser("delete-voltas@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Tributário");
        UUID cycleId = createActiveCycle(principal, "Ciclo com voltas", subjectId, 30);
        UUID firstRunSessionId = createManualSessionAt(
                principal, subjectId, 1800, "2026-07-19T08:00:00");
        UUID secondRunSessionId = createManualSessionAt(
                principal, subjectId, 600, "2026-07-19T09:00:00");

        mockMvc.perform(withSpaCsrf(delete("/api/study-sessions/{id}", firstRunSessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/study-sessions/history").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondRunSessionId.toString()))
                .andExpect(jsonPath("$[0].credits[0].creditedSeconds").value(600));
        mockMvc.perform(get("/api/study-cycles/{id}/runs", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].status").value("ABANDONED"))
                .andExpect(jsonPath("$[0].stages[0].targetSeconds").value(1800))
                .andExpect(jsonPath("$[0].stages[0].creditedSeconds").value(0))
                .andExpect(jsonPath("$[1].number").value(1))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].stages[0].targetSeconds").value(1800))
                .andExpect(jsonPath("$[1].stages[0].creditedSeconds").value(600));
    }

    @Test
    void versionsTheLegacyExerciseCorrectionAndRejectsAStaleRetry() throws Exception {
        IdentityPrincipal principal = createUser("legado@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Raciocínio Lógico");
        UUID sessionId = createManualSession(principal, subjectId, 600);

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}/exercise-result", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"questionsAttempted":10,"questionsCorrect":8}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}/exercise-result", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"questionsAttempted":10,"questionsCorrect":2}
                                """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://estudalivre.local/problems/study-session-conflict"));

        mockMvc.perform(get("/api/study-sessions/summary").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].questionsCorrect").value(8));
    }

    @Test
    void rejectsStaleForeignAndOpenSessionMaintenanceWithoutEffects() throws Exception {
        IdentityPrincipal owner = createUser("dona-manutencao@example.com", "America/Sao_Paulo");
        IdentityPrincipal other = createUser("outra-manutencao@example.com", "America/Sao_Paulo");
        UUID ownerSubjectId = createSubject(owner, "Arquivologia");
        UUID otherSubjectId = createSubject(other, "Informática");
        UUID finishedId = createManualSession(owner, ownerSubjectId, 600);

        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", finishedId)
                        .with(user(other)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(0, otherSubjectId, 1200))))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", finishedId)
                        .with(user(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(7, ownerSubjectId, 1200))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://estudalivre.local/problems/study-session-conflict"));

        String open = mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"FREE\",\"subjectId\":\"%s\"}".formatted(ownerSubjectId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID openId = UUID.fromString(JsonPath.read(open, "$.id"));
        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", openId)
                        .with(user(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(0, ownerSubjectId, 1200))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/study-sessions/history").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(0))
                .andExpect(jsonPath("$[0].effectiveSeconds").value(600));
    }

    private String updateBody(int expectedVersion, UUID subjectId, long seconds) {
        return """
                {"expectedVersion":%d,"startedAtLocal":"2026-07-20T08:00:00",
                 "effectiveSeconds":%d,"subjectId":"%s"}
                """.formatted(expectedVersion, seconds, subjectId);
    }

    private void updateSession(
            IdentityPrincipal principal, UUID sessionId, int expectedVersion, UUID subjectId, long seconds)
            throws Exception {
        mockMvc.perform(withSpaCsrf(put("/api/study-sessions/{id}", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(expectedVersion, subjectId, seconds))))
                .andExpect(status().isOk());
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
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID createContent(IdentityPrincipal principal, UUID subjectId, String name) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/subjects/{subjectId}/contents", subjectId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID createManualSession(IdentityPrincipal principal, UUID subjectId, long seconds) throws Exception {
        return createManualSessionAt(principal, subjectId, seconds, "2026-07-19T08:00:00");
    }

    private UUID createManualSessionAt(
            IdentityPrincipal principal, UUID subjectId, long seconds, String startedAtLocal) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/study-sessions/manual")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startedAtLocal":"%s","effectiveSeconds":%d,"subjectId":"%s"}
                                """.formatted(startedAtLocal, seconds, subjectId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID createActiveCycle(
            IdentityPrincipal principal, String name, UUID subjectId, int targetMinutes) throws Exception {
        UUID cycleId = createConfiguredCycle(principal, name, subjectId, targetMinutes);
        activateCycle(principal, cycleId, null);
        return cycleId;
    }

    private UUID createConfiguredCycle(
            IdentityPrincipal principal, String name, UUID subjectId, int targetMinutes) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/study-cycles")
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID cycleId = UUID.fromString(JsonPath.read(body, "$.id"));
        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","stages":[{"subjectId":"%s","targetMinutes":%d}]}
                """.formatted(name, subjectId, targetMinutes))))
                .andExpect(status().isOk());
        return cycleId;
    }

    private void activateCycle(IdentityPrincipal principal, UUID cycleId, String action) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/study-cycles/{id}/activate", cycleId)
                .with(user(principal));
        if (action != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentRunAction\":\"%s\"}".formatted(action));
        }
        mockMvc.perform(withSpaCsrf(request)).andExpect(status().isOk());
    }

    private UUID startCycleSession(
            IdentityPrincipal principal, UUID cycleId, UUID contentId) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"CYCLE","cycleId":"%s","contentId":"%s"}
                                """.formatted(cycleId, contentId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private void finishSession(
            IdentityPrincipal principal, UUID sessionId, long seconds, boolean scheduleReviews) throws Exception {
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"effectiveSeconds":%d,"expectedVersion":0,"scheduleReviews":%s}
                                """.formatted(seconds, scheduleReviews))))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder withSpaCsrf(MockHttpServletRequestBuilder request) throws Exception {
        Cookie csrfCookie = csrfCookie();
        return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
