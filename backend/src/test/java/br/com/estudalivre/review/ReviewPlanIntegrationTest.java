package br.com.estudalivre.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
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
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ReviewPlanIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM content");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void finishingAContentSessionCreatesTheCanonicalReviewQueueFromItsLocalDate() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Constitucional");
        UUID contentId = createContent(principal, subjectId, "Controle de constitucionalidade");
        UUID sessionId = startContentSession(principal, subjectId, contentId);
        jdbcTemplate.update(
                "UPDATE study_session SET started_at = ? WHERE id = ?",
                OffsetDateTime.parse("2027-01-01T02:30:00Z"),
                sessionId);

        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
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

        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].subjectName").value("Direito Constitucional"))
                .andExpect(jsonPath("$[0].contentName").value("Controle de constitucionalidade"))
                .andExpect(jsonPath("$[0].initialStudyDate").value("2026-12-31"))
                .andExpect(jsonPath("$[0].intervalDays").value(1))
                .andExpect(jsonPath("$[0].dueDate").value("2027-01-01"))
                .andExpect(jsonPath("$[1].intervalDays").value(7))
                .andExpect(jsonPath("$[1].dueDate").value("2027-01-07"))
                .andExpect(jsonPath("$[2].dueDate").value("2027-01-30"))
                .andExpect(jsonPath("$[3].dueDate").value("2027-03-01"))
                .andExpect(jsonPath("$[4].dueDate").value("2027-03-31"))
                .andExpect(jsonPath("$[5].dueDate").value("2027-04-30"));
    }

    @Test
    void ownerCanOpenTheFullReviewPlanCreatedFromTheQueue() throws Exception {
        IdentityPrincipal principal = createUser("plano-completo@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Tributário");
        UUID contentId = createContent(principal, subjectId, "Crédito tributário");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));

        mockMvc.perform(get("/api/review-plans/{planId}", planId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.subject.name").value("Direito Tributário"))
                .andExpect(jsonPath("$.content.name").value("Crédito tributário"))
                .andExpect(jsonPath("$.occurrences.length()").value(6))
                .andExpect(jsonPath("$.occurrences[0].intervalDays").value(1))
                .andExpect(jsonPath("$.occurrences[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.occurrences[5].intervalDays").value(120));
    }

    @Test
    void ownerCanListReviewPlansWithMaintenanceCounts() throws Exception {
        IdentityPrincipal principal = createUser("lista-planos@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Contabilidade Geral");
        UUID contentId = createContent(principal, subjectId, "Demonstrações contábeis");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);

        mockMvc.perform(get("/api/review-plans").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].version").value(0))
                .andExpect(jsonPath("$[0].subjectName").value("Contabilidade Geral"))
                .andExpect(jsonPath("$[0].contentName").value("Demonstrações contábeis"))
                .andExpect(jsonPath("$[0].scheduledCount").value(6))
                .andExpect(jsonPath("$[0].completedCount").value(0))
                .andExpect(jsonPath("$[0].skippedCount").value(0))
                .andExpect(jsonPath("$[0].canceledCount").value(0));
    }

    @Test
    void ownerCanRescheduleAFutureOccurrenceUsingTheCurrentPlanVersion() throws Exception {
        IdentityPrincipal principal = createUser("reagendamento@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Processo Civil");
        UUID contentId = createContent(principal, subjectId, "Recursos");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));
        UUID occurrenceId = UUID.fromString(JsonPath.read(queue, "$[0].occurrenceId"));
        LocalDate newDueDate = LocalDate.now(ZoneId.of(principal.timeZone())).plusDays(3);

        mockMvc.perform(withSpaCsrf(put("/api/review-plans/{planId}/schedule", planId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "occurrences":[
                                    {"occurrenceId":"%s","dueDate":"%s"}
                                  ]
                                }
                                """.formatted(occurrenceId, newDueDate))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.occurrences[0].dueDate").value(newDueDate.toString()));

        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.occurrenceId == '%s')].dueDate".formatted(occurrenceId))
                        .value(newDueDate.toString()));
    }

    @Test
    void cancelingAPlanRemovesOnlyItsPendingOccurrencesFromTheQueue() throws Exception {
        IdentityPrincipal principal = createUser("cancelamento@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Penal");
        UUID contentId = createContent(principal, subjectId, "Teoria do crime");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));

        mockMvc.perform(withSpaCsrf(post("/api/review-plans/{planId}/cancel", planId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.occurrences[*].status").value(everyItem(equalTo("CANCELED"))));

        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/review-plans").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CANCELED"))
                .andExpect(jsonPath("$[0].canceledCount").value(6));
    }

    @Test
    void reactivatingAPlanRestoresCanceledOccurrencesWithoutDuplicatingThem() throws Exception {
        IdentityPrincipal principal = createUser("reativacao@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Informática");
        UUID contentId = createContent(principal, subjectId, "Segurança da informação");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));
        List<String> originalOccurrenceIds = JsonPath.read(queue, "$[*].occurrenceId");
        mockMvc.perform(withSpaCsrf(post("/api/review-plans/{planId}/cancel", planId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}")))
                .andExpect(status().isOk());

        String reactivated = mockMvc.perform(withSpaCsrf(post("/api/review-plans/{planId}/reactivate", planId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.occurrences.length()").value(6))
                .andExpect(jsonPath("$.occurrences[*].status").value(everyItem(equalTo("SCHEDULED"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<List<String>>read(reactivated, "$.occurrences[*].id"))
                .containsExactlyInAnyOrderElementsOf(originalOccurrenceIds);
        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void concurrentScheduleEditsReturnConflictInsteadOfLosingAnUpdate() throws Exception {
        IdentityPrincipal principal = createUser("concorrencia-plano@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Constitucional");
        UUID contentId = createContent(principal, subjectId, "Poder constituinte");
        finishSession(principal, startContentSession(principal, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));
        UUID occurrenceId = UUID.fromString(JsonPath.read(queue, "$[0].occurrenceId"));
        LocalDate today = LocalDate.now(ZoneId.of(principal.timeZone()));
        LocalDate firstDate = today.plusDays(3);
        LocalDate secondDate = today.plusDays(4);
        Cookie csrfCookie = csrfCookie();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Integer>> responses = List.of(
                    executor.submit(() -> rescheduleConcurrently(
                            principal, planId, occurrenceId, firstDate, csrfCookie, ready, start)),
                    executor.submit(() -> rescheduleConcurrently(
                            principal, planId, occurrenceId, secondDate, csrfCookie, ready, start)));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(
                    responses.get(0).get(10, TimeUnit.SECONDS),
                    responses.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        String detail = mockMvc.perform(get("/api/review-plans/{planId}", planId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<String>read(detail, "$.occurrences[0].dueDate"))
                .isIn(firstDate.toString(), secondDate.toString());
    }

    @Test
    void bulkEditingNeverChangesCompletedOrSkippedOccurrences() throws Exception {
        IdentityPrincipal principal = createUser("historico-imutavel@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Administração Financeira");
        UUID contentId = createContent(principal, subjectId, "Orçamento público");
        UUID sourceSessionId = startContentSession(principal, subjectId, contentId);
        jdbcTemplate.update(
                "UPDATE study_session SET started_at = ? WHERE id = ?",
                OffsetDateTime.now(ZoneId.of("UTC")).minusDays(10),
                sourceSessionId);
        finishSession(principal, sourceSessionId, true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));
        UUID requestedOccurrenceId = UUID.fromString(JsonPath.read(queue, "$[0].occurrenceId"));
        String reviewSession = mockMvc.perform(withSpaCsrf(post("/api/reviews/{occurrenceId}/start", requestedOccurrenceId)
                        .with(user(principal))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID reviewSessionId = UUID.fromString(JsonPath.read(reviewSession, "$.id"));
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", reviewSessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":600,
                                  "expectedVersion":0,
                                  "scheduleReviews":false
                                }
                                """)))
                .andExpect(status().isOk());
        String detail = mockMvc.perform(get("/api/review-plans/{planId}", planId).with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID completedId = UUID.fromString(JsonPath.<List<String>>read(
                detail, "$.occurrences[?(@.status == 'COMPLETED')].id").getFirst());
        UUID futureId = UUID.fromString(JsonPath.<List<String>>read(
                detail, "$.occurrences[?(@.status == 'SCHEDULED')].id").getFirst());
        String originalFutureDate = JsonPath.<List<String>>read(
                detail, "$.occurrences[?(@.id == '%s')].dueDate".formatted(futureId)).getFirst();
        LocalDate today = LocalDate.now(ZoneId.of(principal.timeZone()));

        mockMvc.perform(withSpaCsrf(put("/api/review-plans/{planId}/schedule", planId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "occurrences":[
                                    {"occurrenceId":"%s","dueDate":"%s"},
                                    {"occurrenceId":"%s","dueDate":"%s"}
                                  ]
                                }
                                """.formatted(
                                        completedId, today.plusDays(40),
                                        futureId, today.plusDays(45)))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/review-plans/{planId}", planId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.occurrences[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.occurrences[1].status").value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.occurrences[?(@.id == '%s')].dueDate".formatted(futureId))
                        .value(originalFutureDate));
    }

    @Test
    void anotherUserCannotReadOrChangeAReviewPlan() throws Exception {
        IdentityPrincipal owner = createUser("dono-plano@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(owner, "Língua Portuguesa");
        UUID contentId = createContent(owner, subjectId, "Regência verbal");
        finishSession(owner, startContentSession(owner, subjectId, contentId), true);
        String queue = mockMvc.perform(get("/api/reviews").with(user(owner)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(queue, "$[0].planId"));
        UUID occurrenceId = UUID.fromString(JsonPath.read(queue, "$[0].occurrenceId"));
        IdentityPrincipal other = createUser("intruso-plano@example.com", "America/Sao_Paulo");

        mockMvc.perform(get("/api/review-plans/{planId}", planId).with(user(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(put("/api/review-plans/{planId}/schedule", planId)
                        .with(user(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "occurrences":[
                                    {"occurrenceId":"%s","dueDate":"%s"}
                                  ]
                                }
                                """.formatted(occurrenceId, LocalDate.now().plusDays(5)))))
                .andExpect(status().isNotFound());
        mockMvc.perform(withSpaCsrf(post("/api/review-plans/{planId}/cancel", planId)
                        .with(user(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void leavesTheReviewQueueEmptyWhenSchedulingIsDisabled() throws Exception {
        IdentityPrincipal principal = createUser("sem-plano@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Administração Pública");
        UUID contentId = createContent(principal, subjectId, "Governança pública");
        UUID sessionId = startContentSession(principal, subjectId, contentId);

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
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void concurrentConfirmationsForTheSameContentReuseOnePlanAndItsOccurrences() throws Exception {
        IdentityPrincipal principal = createUser("concorrencia@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID contentId = createContent(principal, subjectId, "Atos administrativos");
        UUID firstSessionId = startContentSession(principal, subjectId, contentId);
        finishSession(principal, firstSessionId, false);
        UUID secondSessionId = startContentSession(principal, subjectId, contentId);
        Cookie csrfCookie = csrfCookie();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Integer>> responses = List.of(
                    executor.submit(() -> finishConcurrently(
                            principal, firstSessionId, 1, csrfCookie, ready, start)),
                    executor.submit(() -> finishConcurrently(
                            principal, secondSessionId, 0, csrfCookie, ready, start)));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(
                    responses.get(0).get(10, TimeUnit.SECONDS),
                    responses.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 200);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        String queue = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> planIds = JsonPath.read(queue, "$[*].planId");
        assertThat(planIds).containsOnly(planIds.getFirst());
    }

    @Test
    void classifiesAndOrdersOnlyTheOwnersQueueUsingTheirLocalDate() throws Exception {
        String timeZone = "Pacific/Kiritimati";
        IdentityPrincipal owner = createUser("dona@example.com", timeZone);
        UUID subjectId = createSubject(owner, "Língua Portuguesa");
        UUID contentId = createContent(owner, subjectId, "Concordância verbal");
        UUID sessionId = startContentSession(owner, subjectId, contentId);
        finishSession(owner, sessionId, true);
        UUID planId = jdbcTemplate.queryForObject(
                "SELECT id FROM review_plan WHERE owner_id = ?",
                UUID.class,
                owner.id());
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE plan_id = ? AND interval_days = 1",
                today.minusDays(2),
                planId);
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE plan_id = ? AND interval_days = 7",
                today,
                planId);
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = due_date + INTERVAL '1 year' WHERE plan_id = ? AND interval_days > 7",
                planId);

        IdentityPrincipal other = createUser("outra@example.com", "America/Sao_Paulo");
        UUID otherSubjectId = createSubject(other, "Contabilidade");
        UUID otherContentId = createContent(other, otherSubjectId, "Balanço patrimonial");
        finishSession(other, startContentSession(other, otherSubjectId, otherContentId), true);

        mockMvc.perform(get("/api/reviews").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].contentName").value("Concordância verbal"))
                .andExpect(jsonPath("$[0].dueDate").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$[0].timing").value("OVERDUE"))
                .andExpect(jsonPath("$[1].dueDate").value(today.toString()))
                .andExpect(jsonPath("$[1].timing").value("TODAY"))
                .andExpect(jsonPath("$[2].timing").value("FUTURE"));
    }

    @Test
    void keepsTheAnchoredPlanAfterTheSourceSessionChangesOrIsDeleted() throws Exception {
        IdentityPrincipal principal = createUser("independente@example.com", "America/Sao_Paulo");
        UUID subjectId = createSubject(principal, "Raciocínio Lógico");
        UUID contentId = createContent(principal, subjectId, "Lógica proposicional");
        UUID sessionId = startContentSession(principal, subjectId, contentId);
        finishSession(principal, sessionId, true);
        String before = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String initialStudyDate = JsonPath.read(before, "$[0].initialStudyDate");
        List<String> dueDates = JsonPath.read(before, "$[*].dueDate");

        jdbcTemplate.update(
                "UPDATE study_session SET started_at = ? WHERE id = ?",
                OffsetDateTime.parse("2030-01-01T12:00:00Z"),
                sessionId);
        jdbcTemplate.update("DELETE FROM study_session WHERE id = ?", sessionId);

        String after = mockMvc.perform(get("/api/reviews").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].initialStudyDate").value(initialStudyDate))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<List<String>>read(after, "$[*].dueDate")).containsExactlyElementsOf(dueDates);
    }

    private int finishConcurrently(
            IdentityPrincipal principal,
            UUID sessionId,
            int expectedVersion,
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
                                  "expectedVersion":%d,
                                  "scheduleReviews":true
                                }
                                """.formatted(expectedVersion)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int rescheduleConcurrently(
            IdentityPrincipal principal,
            UUID planId,
            UUID occurrenceId,
            LocalDate dueDate,
            Cookie csrfCookie,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(put("/api/review-plans/{planId}/schedule", planId)
                        .with(user(principal))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "occurrences":[
                                    {"occurrenceId":"%s","dueDate":"%s"}
                                  ]
                                }
                                """.formatted(occurrenceId, dueDate)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void finishSession(
            IdentityPrincipal principal,
            UUID sessionId,
            boolean scheduleReviews) throws Exception {
        mockMvc.perform(withSpaCsrf(post("/api/study-sessions/{id}/finish", sessionId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveSeconds":600,
                                  "expectedVersion":0,
                                  "scheduleReviews":%s
                                }
                                """.formatted(scheduleReviews))))
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
