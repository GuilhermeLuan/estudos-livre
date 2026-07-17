package br.com.estudalivre.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
