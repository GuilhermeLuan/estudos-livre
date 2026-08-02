package br.com.estudalivre.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.testing.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@TestPropertySource(properties = "app.registration-enabled=true")
class MultiUserIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_session_credit");
        jdbcTemplate.update("DELETE FROM study_session_exercise_result");
        jdbcTemplate.update("DELETE FROM study_session_timer_segment");
        jdbcTemplate.update("DELETE FROM study_session");
        jdbcTemplate.update("DELETE FROM review_occurrence");
        jdbcTemplate.update("DELETE FROM review_plan");
        jdbcTemplate.update("DELETE FROM study_cycle_suggestion_subject");
        jdbcTemplate.update("DELETE FROM study_cycle_run_stage");
        jdbcTemplate.update("DELETE FROM study_cycle_run");
        jdbcTemplate.update("DELETE FROM study_cycle_stage");
        jdbcTemplate.update("DELETE FROM study_cycle");
        jdbcTemplate.update("DELETE FROM content");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("DELETE FROM password_reset_token");
        jdbcTemplate.update("DELETE FROM identity_user");
    }

    @Test
    void publicRegistrationKeepsAuthenticatedIdentitiesSeparate() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        org.assertj.core.api.Assertions.assertThat(first.session().getValue())
                .isNotEqualTo(second.session().getValue());
        org.assertj.core.api.Assertions.assertThat(first.csrf().getValue())
                .isNotEqualTo(second.csrf().getValue());
    }

    @Test
    void studyCollectionsBelongOnlyToTheCreatingAccount() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        UUID subjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID contentId = createContent(first.session(), first.csrf(), subjectId, "Direitos Fundamentais");
        UUID cycleId = createCycle(first.session(), first.csrf(), "Ciclo da primeira conta");

        mockMvc.perform(get("/api/subjects").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(subjectId.toString()))
                .andExpect(jsonPath("$[0].name").value("Direito Constitucional"));
        mockMvc.perform(get("/api/subjects/{id}", subjectId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.name").value("Direito Constitucional"));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents", subjectId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(contentId.toString()))
                .andExpect(jsonPath("$[0].name").value("Direitos Fundamentais"));
        mockMvc.perform(get("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                        .cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contentId.toString()))
                .andExpect(jsonPath("$.subjectId").value(subjectId.toString()))
                .andExpect(jsonPath("$.name").value("Direitos Fundamentais"));

        mockMvc.perform(get("/api/study-cycles").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(cycleId.toString()))
                .andExpect(jsonPath("$[0].name").value("Ciclo da primeira conta"));
        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cycleId.toString()))
                .andExpect(jsonPath("$.name").value("Ciclo da primeira conta"));

        mockMvc.perform(get("/api/subjects").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/study-cycles").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/subjects/{subjectId}/contents", subjectId).cookie(second.session()))
                .andExpect(status().isNotFound());
    }

    @Test
    void knownForeignIdsCannotBeReadOrMutated() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        UUID subjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID contentId = createContent(first.session(), first.csrf(), subjectId, "Direitos Fundamentais");
        UUID cycleId = createCycle(first.session(), first.csrf(), "Ciclo da primeira conta");

        mockMvc.perform(get("/api/subjects/{id}", subjectId).cookie(second.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));
        mockMvc.perform(withSessionAndCsrf(
                        put("/api/subjects/{id}", subjectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Nome indevido\"}"),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(get("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                        .cookie(second.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conteúdo não encontrado"));
        mockMvc.perform(withSessionAndCsrf(
                        put("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Conteúdo indevido\"}"),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conteúdo não encontrado"));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).cookie(second.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));
        mockMvc.perform(withSessionAndCsrf(
                        put("/api/study-cycles/{id}", cycleId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Ciclo indevido",
                                          "stages":[{"subjectId":"%s","targetMinutes":5}]
                                        }
                                        """.formatted(subjectId)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));

        mockMvc.perform(get("/api/subjects/{id}", subjectId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Direito Constitucional"))
                .andExpect(jsonPath("$.archived").value(false));
        mockMvc.perform(get("/api/subjects/{subjectId}/contents/{contentId}", subjectId, contentId)
                        .cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Direitos Fundamentais"))
                .andExpect(jsonPath("$.archived").value(false));
        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ciclo da primeira conta"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void studySessionsResultsAndReviewsRemainPrivateWithIndependentOpenSessions() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        UUID firstSubjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID firstContentId = createContent(first.session(), first.csrf(), firstSubjectId, "Direitos Fundamentais");
        UUID secondSubjectId = createSubject(second.session(), second.csrf(), "Direito Administrativo");
        UUID secondContentId = createContent(second.session(), second.csrf(), secondSubjectId, "Atos Administrativos");

        UUID firstStudySessionId = startFreeSession(
                first.session(), first.csrf(), firstSubjectId, firstContentId);
        UUID secondStudySessionId = startFreeSession(
                second.session(), second.csrf(), secondSubjectId, secondContentId);

        mockMvc.perform(get("/api/study-sessions/current").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstStudySessionId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.content.id").value(firstContentId.toString()));
        mockMvc.perform(get("/api/study-sessions/current").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondStudySessionId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.content.id").value(secondContentId.toString()));

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions/{id}/pause", firstStudySessionId),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sessão de estudo não encontrada"));
        mockMvc.perform(withSessionAndCsrf(
                        put("/api/study-sessions/{id}/exercise-result", firstStudySessionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"questionsAttempted\":10,\"questionsCorrect\":8}"),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Sessão de estudo não encontrada"));

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions/{id}/finish", firstStudySessionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "effectiveSeconds":600,
                                          "expectedVersion":0,
                                          "questionsAttempted":10,
                                          "questionsCorrect":8,
                                          "scheduleReviews":true
                                        }
                                        """),
                        first.session(),
                        first.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstStudySessionId.toString()))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.effectiveSeconds").value(600))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.exerciseResult.questionsAttempted").value(10))
                .andExpect(jsonPath("$.exerciseResult.questionsCorrect").value(8))
                .andExpect(jsonPath("$.exerciseResult.accuracyPercentage").value(80.0));

        mockMvc.perform(get("/api/study-sessions/history").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstStudySessionId.toString()))
                .andExpect(jsonPath("$[0].status").value("FINISHED"))
                .andExpect(jsonPath("$[0].exerciseResult.questionsAttempted").value(10))
                .andExpect(jsonPath("$[0].exerciseResult.questionsCorrect").value(8))
                .andExpect(jsonPath("$[0].exerciseResult.accuracyPercentage").value(80.0));

        String reviewPlans = mockMvc.perform(get("/api/review-plans").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].subjectName").value("Direito Constitucional"))
                .andExpect(jsonPath("$[0].contentName").value("Direitos Fundamentais"))
                .andExpect(jsonPath("$[0].scheduledCount").value(6))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(reviewPlans, "$[0].id"));

        mockMvc.perform(get("/api/reviews").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].planId").value(planId.toString()))
                .andExpect(jsonPath("$[0].contentId").value(firstContentId.toString()));
        mockMvc.perform(get("/api/review-plans/{planId}", planId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.occurrences.length()").value(6))
                .andExpect(jsonPath("$.content.id").value(firstContentId.toString()));

        mockMvc.perform(get("/api/study-sessions/current").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondStudySessionId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/study-sessions/history").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/reviews").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/review-plans").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/review-plans/{planId}", planId).cookie(second.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plano de revisão não encontrado"));
    }

    @Test
    void activeCyclesAndRunsRemainIndependentBetweenAccounts() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        UUID firstSubjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID firstCycleId = createCycle(first.session(), first.csrf(), "Ciclo da primeira conta");
        UUID secondSubjectId = createSubject(second.session(), second.csrf(), "Direito Administrativo");
        UUID secondCycleId = createCycle(second.session(), second.csrf(), "Ciclo da segunda conta");

        configureCycle(
                first.session(), first.csrf(), firstCycleId, firstSubjectId, "Ciclo da primeira conta");
        configureCycle(
                second.session(), second.csrf(), secondCycleId, secondSubjectId, "Ciclo da segunda conta");

        MvcResult firstActivation = activateCycle(first.session(), first.csrf(), firstCycleId);
        UUID firstRunId = UUID.fromString(
                JsonPath.read(firstActivation.getResponse().getContentAsString(), "$.currentRun.id"));
        MvcResult secondActivation = activateCycle(second.session(), second.csrf(), secondCycleId);
        UUID secondRunId = UUID.fromString(
                JsonPath.read(secondActivation.getResponse().getContentAsString(), "$.currentRun.id"));

        org.assertj.core.api.Assertions.assertThat(firstRunId).isNotEqualTo(secondRunId);

        mockMvc.perform(get("/api/study-cycles").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstCycleId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].currentRun.id").value(firstRunId.toString()))
                .andExpect(jsonPath("$[0].currentRun.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/study-cycles").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondCycleId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].currentRun.id").value(secondRunId.toString()))
                .andExpect(jsonPath("$[0].currentRun.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/study-cycles/{id}/runs", firstCycleId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstRunId.toString()))
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].stages[0].subjectId").value(firstSubjectId.toString()));
        mockMvc.perform(get("/api/study-cycles/{id}/runs", secondCycleId).cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondRunId.toString()))
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].stages[0].subjectId").value(secondSubjectId.toString()));

        mockMvc.perform(get("/api/study-cycles/{id}", firstCycleId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstCycleId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRun.id").value(firstRunId.toString()))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"));
    }

    @Test
    void foreignReferencesAndCycleCommandsAreRejectedAcrossAccounts() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        UUID firstSubjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID firstContentId = createContent(
                first.session(), first.csrf(), firstSubjectId, "Direitos Fundamentais");
        UUID firstCycleId = createCycle(first.session(), first.csrf(), "Ciclo da primeira conta");
        configureCycle(
                first.session(), first.csrf(), firstCycleId, firstSubjectId, "Ciclo da primeira conta");

        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");
        UUID secondSubjectId = createSubject(second.session(), second.csrf(), "Direito Administrativo");
        UUID secondCycleId = createCycle(second.session(), second.csrf(), "Ciclo da segunda conta");

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "origin":"FREE",
                                          "subjectId":"%s",
                                          "contentId":"%s"
                                        }
                                        """.formatted(firstSubjectId, firstContentId)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "origin":"FREE",
                                          "subjectId":"%s",
                                          "contentId":"%s"
                                        }
                                        """.formatted(secondSubjectId, firstContentId)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conteúdo não encontrado"));

        mockMvc.perform(get("/api/study-sessions/current").cookie(second.session()))
                .andExpect(status().isNoContent());

        mockMvc.perform(withSessionAndCsrf(
                        put("/api/study-cycles/{id}", secondCycleId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"Ciclo da segunda conta",
                                          "stages":[{"subjectId":"%s","targetMinutes":60}]
                                        }
                                        """.formatted(firstSubjectId)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Matéria não encontrada"));

        mockMvc.perform(get("/api/study-cycles/{id}", secondCycleId).cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.stages").isEmpty());

        mockMvc.perform(get("/api/study-cycles/{id}/runs", firstCycleId).cookie(second.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));
        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-cycles/{id}/activate", firstCycleId),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));

        activateCycle(first.session(), first.csrf(), firstCycleId);
    }

    @Test
    void foreignReviewPlansAndOccurrencesCannotBeChanged() throws Exception {
        AuthenticatedClient first = registerAndLogin("primeira@example.com", "America/Sao_Paulo");
        UUID firstSubjectId = createSubject(first.session(), first.csrf(), "Direito Constitucional");
        UUID firstContentId = createContent(
                first.session(), first.csrf(), firstSubjectId, "Direitos Fundamentais");
        UUID firstStudySessionId = startFreeSession(
                first.session(), first.csrf(), firstSubjectId, firstContentId);

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions/{id}/finish", firstStudySessionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "effectiveSeconds":600,
                                          "expectedVersion":0,
                                          "scheduleReviews":true
                                        }
                                        """),
                        first.session(),
                        first.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        String planDetail = mockMvc.perform(get("/api/review-plans").cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID planId = UUID.fromString(JsonPath.<String>read(planDetail, "$[0].id"));
        Integer planVersion = JsonPath.read(planDetail, "$[0].version");

        String detail = mockMvc.perform(get("/api/review-plans/{planId}", planId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(planVersion))
                .andExpect(jsonPath("$.occurrences.length()").value(6))
                .andExpect(jsonPath("$.occurrences[0].intervalDays").value(1))
                .andExpect(jsonPath("$.occurrences[1].intervalDays").value(7))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID dueOccurrenceId = UUID.fromString(JsonPath.<String>read(detail, "$.occurrences[0].id"));
        UUID futureOccurrenceId = UUID.fromString(JsonPath.<String>read(detail, "$.occurrences[1].id"));
        String futureDueDate = JsonPath.read(detail, "$.occurrences[1].dueDate");
        org.assertj.core.api.Assertions.assertThat(LocalDate.parse(futureDueDate))
                .isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo")));

        LocalDate dueDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        // A API pública só aceita datas futuras; SQL apenas prepara a ocorrência de hoje, sem verificar resultado.
        jdbcTemplate.update(
                "UPDATE review_occurrence SET due_date = ? WHERE id = ?", dueDate, dueOccurrenceId);
        String baselineDetail = mockMvc.perform(
                        get("/api/review-plans/{planId}", planId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences[0].id").value(dueOccurrenceId.toString()))
                .andExpect(jsonPath("$.occurrences[0].dueDate").value(dueDate.toString()))
                .andExpect(jsonPath("$.occurrences[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.occurrences[0].inProgress").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthenticatedClient second = registerAndLogin("segunda@example.com", "America/Recife");

        mockMvc.perform(withSessionAndCsrf(
                        put("/api/review-plans/{planId}/schedule", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "expectedVersion":%d,
                                          "occurrences":[
                                            {"occurrenceId":"%s","dueDate":"%s"}
                                          ]
                                        }
                                        """.formatted(planVersion, futureOccurrenceId, futureDueDate)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plano de revisão não encontrado"));
        mockMvc.perform(withSessionAndCsrf(
                        post("/api/review-plans/{planId}/cancel", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedVersion\":%d}".formatted(planVersion)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plano de revisão não encontrado"));
        mockMvc.perform(withSessionAndCsrf(
                        post("/api/review-plans/{planId}/reactivate", planId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedVersion\":%d}".formatted(planVersion)),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Plano de revisão não encontrado"));

        String unchangedDetail = mockMvc.perform(
                        get("/api/review-plans/{planId}", planId).cookie(first.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(planVersion))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(unchangedDetail).isEqualTo(baselineDetail);

        mockMvc.perform(withSessionAndCsrf(
                        post("/api/reviews/{occurrenceId}/start", dueOccurrenceId),
                        first.session(),
                        first.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("REVIEW"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subject.id").value(firstSubjectId.toString()))
                .andExpect(jsonPath("$.content.id").value(firstContentId.toString()));
        mockMvc.perform(withSessionAndCsrf(
                        post("/api/reviews/{occurrenceId}/start", dueOccurrenceId),
                        second.session(),
                        second.csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Revisão indisponível"));

        mockMvc.perform(get("/api/review-plans").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/reviews").cookie(second.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/study-sessions/current").cookie(second.session()))
                .andExpect(status().isNoContent());
    }

    private AuthenticatedClient registerAndLogin(String email, String timeZone) throws Exception {
        Cookie anonymousCsrf = csrfCookie();
        register(email, timeZone, anonymousCsrf)
                .andExpect(status().isCreated());

        MvcResult login = login(email, anonymousCsrf);
        Cookie session = login.getResponse().getCookie("SESSION");
        org.assertj.core.api.Assertions.assertThat(session).isNotNull();

        MvcResult authenticatedRequest = mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.timeZone").value(timeZone))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrf = authenticatedRequest.getResponse().getCookie("XSRF-TOKEN");
        org.assertj.core.api.Assertions.assertThat(csrf).isNotNull();

        return new AuthenticatedClient(session, csrf);
    }

    private void configureCycle(
            Cookie session, Cookie csrf, UUID cycleId, UUID subjectId, String name) throws Exception {
        mockMvc.perform(withSessionAndCsrf(
                        put("/api/study-cycles/{id}", cycleId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":"%s",
                                          "stages":[{"subjectId":"%s","targetMinutes":60}]
                                        }
                                        """.formatted(name, subjectId)),
                        session,
                        csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activatable").value(true))
                .andExpect(jsonPath("$.stages[0].subjectId").value(subjectId.toString()));
    }

    private MvcResult activateCycle(Cookie session, Cookie csrf, UUID cycleId) throws Exception {
        return mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-cycles/{id}/activate", cycleId),
                        session,
                        csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cycleId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRun.number").value(1))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentRun.currentStagePosition").value(1))
                .andReturn();
    }

    private UUID createSubject(Cookie session, Cookie csrf, String name) throws Exception {
        String body = mockMvc.perform(withSessionAndCsrf(
                        post("/api/subjects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)),
                        session,
                        csrf))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID createContent(Cookie session, Cookie csrf, UUID subjectId, String name) throws Exception {
        String body = mockMvc.perform(withSessionAndCsrf(
                        post("/api/subjects/{subjectId}/contents", subjectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)),
                        session,
                        csrf))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID createCycle(Cookie session, Cookie csrf, String name) throws Exception {
        String body = mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-cycles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)),
                        session,
                        csrf))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID startFreeSession(
            Cookie session, Cookie csrf, UUID subjectId, UUID contentId) throws Exception {
        String body = mockMvc.perform(withSessionAndCsrf(
                        post("/api/study-sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "origin":"FREE",
                                          "subjectId":"%s",
                                          "contentId":"%s"
                                        }
                                        """.formatted(subjectId, contentId)),
                        session,
                        csrf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.subject.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.content.id").value(contentId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private MockHttpServletRequestBuilder withSessionAndCsrf(
            MockHttpServletRequestBuilder request, Cookie session, Cookie csrf) {
        return request.cookie(session, csrf).header("X-XSRF-TOKEN", csrf.getValue());
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String email, String timeZone, Cookie csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "uma frase senha segura",
                          "timeZone": "%s"
                        }
                        """.formatted(email, timeZone)));
    }

    private MvcResult login(String email, Cookie csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", email)
                .param("password", "uma frase senha segura"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();
    }

    private Cookie csrfCookie() throws Exception {
        return mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
    }

    private record AuthenticatedClient(Cookie session, Cookie csrf) {
    }
}
