package br.com.estudalivre.review;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class ReviewSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserRepository identityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM review_occurrence");
        jdbcTemplate.update("DELETE FROM review_plan");
        jdbcTemplate.update("DELETE FROM study_cycle_run");
        jdbcTemplate.update("DELETE FROM study_cycle_suggestion_subject");
        jdbcTemplate.update("DELETE FROM study_cycle_stage");
        jdbcTemplate.update("DELETE FROM study_cycle");
        jdbcTemplate.update("DELETE FROM content");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void executesATodaysReviewWithTheExistingTimerFlow() throws Exception {
        IdentityPrincipal principal = createUser("revisora@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Constitucional");
        UUID contentId = createContent(principal, subjectId, "Controle concentrado");
        UUID occurrenceId = createTodaysReview(principal, subjectId, contentId);

        String started = mockMvc.perform(withSpaCsrf(post("/api/reviews/{id}/start", occurrenceId)
                        .with(user(principal))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("REVIEW"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subject.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.content.id").value(contentId.toString()))
                .andExpect(jsonPath("$.cycle").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID sessionId = UUID.fromString(JsonPath.read(started, "$.id"));

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/pause", sessionId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/resume", sessionId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":600,
                                  "expectedVersion":0,
                                  "scheduleReviews":false
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.effectiveSeconds").value(600));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM review_occurrence WHERE id = ?",
                String.class,
                occurrenceId)).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_session_id FROM review_occurrence WHERE id = ?",
                UUID.class,
                occurrenceId)).isEqualTo(sessionId);
        mockMvc.perform(get("/api/study-sessions/current").with(user(principal)))
                .andExpect(status().isNoContent());
    }

    @Test
    void creditsTheEffectiveReviewDurationToTheActiveCycle() throws Exception {
        IdentityPrincipal principal = createUser("ciclo-review@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Matemática");
        UUID contentId = createContent(principal, subjectId, "Razões e proporções");
        UUID occurrenceId = createTodaysReview(principal, subjectId, contentId);
        UUID cycleId = createActiveCycle(principal, "Ciclo quantitativo", subjectId, 30);

        UUID sessionId = startReview(principal, occurrenceId);
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":900,
                                  "expectedVersion":0,
                                  "scheduleReviews":false
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits.length()").value(1))
                .andExpect(jsonPath("$.credits[0].creditedSeconds").value(900));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].creditedSeconds").value(900));
    }

    @Test
    void completesTheLatestOverdueOccurrenceAndKeepsFutureDatesAnchored() throws Exception {
        IdentityPrincipal principal = createUser("atrasadas@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Língua Portuguesa");
        UUID contentId = createContent(principal, subjectId, "Regência verbal");
        createTodaysReview(principal, subjectId, contentId);
        UUID planId = jdbcTemplate.queryForObject(
                "SELECT id FROM review_plan WHERE owner_id = ?",
                UUID.class,
                principal.id());
        LocalDate today = LocalDate.now(ZoneId.of(principal.timeZone()));
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE plan_id = ? AND interval_days = 1",
                today.minusDays(10),
                planId);
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE plan_id = ? AND interval_days = 7",
                today.minusDays(3),
                planId);
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE plan_id = ? AND interval_days = 30",
                today.plusDays(5),
                planId);
        UUID oldestOccurrenceId = jdbcTemplate.queryForObject(
                "SELECT id FROM review_occurrence WHERE plan_id = ? AND interval_days = 1",
                UUID.class,
                planId);
        UUID latestOverdueId = jdbcTemplate.queryForObject(
                "SELECT id FROM review_occurrence WHERE plan_id = ? AND interval_days = 7",
                UUID.class,
                planId);
        LocalDate futureDate = jdbcTemplate.queryForObject(
                "SELECT due_date FROM review_occurrence WHERE plan_id = ? AND interval_days = 30",
                LocalDate.class,
                planId);

        UUID sessionId = startReview(principal, oldestOccurrenceId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT review_occurrence_id FROM study_session WHERE id = ?",
                UUID.class,
                sessionId)).isEqualTo(latestOverdueId);
        finishReview(principal, sessionId, 600);

        assertThat(jdbcTemplate.queryForList(
                "SELECT interval_days, status FROM review_occurrence WHERE plan_id = ? ORDER BY interval_days",
                planId))
                .extracting(row -> row.get("interval_days") + ":" + row.get("status"))
                .containsExactly(
                        "1:SKIPPED",
                        "7:COMPLETED",
                        "30:SCHEDULED",
                        "60:SCHEDULED",
                        "90:SCHEDULED",
                        "120:SCHEDULED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT due_date FROM review_occurrence WHERE plan_id = ? AND interval_days = 30",
                LocalDate.class,
                planId)).isEqualTo(futureDate);
        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].dueDate").value(futureDate.toString()));
    }

    @Test
    void concurrentFinishesResolveAndCreditTheReviewOnlyOnce() throws Exception {
        IdentityPrincipal principal = createUser("finish-review@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Contabilidade");
        UUID contentId = createContent(principal, subjectId, "Balanço patrimonial");
        UUID occurrenceId = createTodaysReview(principal, subjectId, contentId);
        createActiveCycle(principal, "Ciclo contábil", subjectId, 30);
        UUID sessionId = startReview(principal, occurrenceId);
        Cookie csrfCookie = csrfCookie();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Integer>> responses = List.of(
                    executor.submit(() -> finishConcurrently(
                            principal, sessionId, csrfCookie, ready, start)),
                    executor.submit(() -> finishConcurrently(
                            principal, sessionId, csrfCookie, ready, start)));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(
                    responses.get(0).get(10, TimeUnit.SECONDS),
                    responses.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactly(200, 200);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM study_session_credit WHERE session_id = ?",
                Integer.class,
                sessionId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM review_occurrence WHERE id = ?",
                String.class,
                occurrenceId)).isEqualTo("COMPLETED");
    }

    @Test
    void rejectsFutureForeignAndAlreadyExecutedOccurrences() throws Exception {
        IdentityPrincipal owner = createUser("dona-review@example.com", "America/Sao_Paulo");
        IdentityPrincipal other = createUser("outra-review@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(owner, "Administração Pública");
        UUID contentId = createContent(owner, subjectId, "Governança");
        UUID occurrenceId = createTodaysReview(owner, subjectId, contentId);
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = due_date + INTERVAL '1 day' WHERE id = ?",
                occurrenceId);

        expectReviewUnavailable(owner, occurrenceId);
        expectReviewUnavailable(other, occurrenceId);

        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = due_date - INTERVAL '1 day' WHERE id = ?",
                occurrenceId);
        UUID sessionId = startReview(owner, occurrenceId);
        finishReview(owner, sessionId, 600);
        expectReviewUnavailable(owner, occurrenceId);
    }

    private UUID createTodaysReview(
            IdentityPrincipal principal,
            UUID subjectId,
            UUID contentId) throws Exception {
        UUID sourceSessionId = startContentSession(principal, subjectId, contentId);
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sourceSessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":600,
                                  "expectedVersion":0,
                                  "scheduleReviews":true
                                }
                                """)))
                .andExpect(status().isOk());
        UUID occurrenceId = jdbcTemplate.queryForObject(
                """
                SELECT occurrence.id
                FROM review_occurrence occurrence
                JOIN review_plan plan ON plan.id = occurrence.plan_id
                WHERE plan.owner_id = ? AND occurrence.interval_days = 1
                """,
                UUID.class,
                principal.id());
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE id = ?",
                LocalDate.now(ZoneId.of(principal.timeZone())),
                occurrenceId);
        return occurrenceId;
    }

    private UUID startReview(IdentityPrincipal principal, UUID occurrenceId) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/reviews/{id}/start", occurrenceId)
                        .with(user(principal))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private void expectReviewUnavailable(
            IdentityPrincipal principal,
            UUID occurrenceId) throws Exception {
        mockMvc.perform(withSpaCsrf(post("/api/reviews/{id}/start", occurrenceId)
                        .with(user(principal))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Revisão indisponível"));
    }

    private int finishConcurrently(
            IdentityPrincipal principal,
            UUID sessionId,
            Cookie csrfCookie,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":600,
                                  "expectedVersion":0,
                                  "scheduleReviews":false
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void finishReview(
            IdentityPrincipal principal,
            UUID sessionId,
            long effectiveSeconds) throws Exception {
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":%d,
                                  "expectedVersion":0,
                                  "scheduleReviews":false
                                }
                                """.formatted(effectiveSeconds))))
                .andExpect(status().isOk());
    }

    private UUID createActiveCycle(
            IdentityPrincipal principal,
            String name,
            UUID subjectId,
            int targetMinutes) throws Exception {
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
                                  "stages":[{"subjectId":"%s","targetMinutes":%d}]
                                }
                                """.formatted(name, subjectId, targetMinutes))))
                .andExpect(status().isOk());
        mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                        .with(user(principal))))
                .andExpect(status().isOk());
        return cycleId;
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

    private UUID startContentSession(
            IdentityPrincipal principal,
            UUID subjectId,
            UUID contentId) throws Exception {
        String body = mockMvc.perform(withSpaCsrf(post("/api/study-sessions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin":"FREE",
                                  "subjectId":"%s",
                                  "contentId":"%s"
                                }
                                """.formatted(subjectId, contentId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
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
