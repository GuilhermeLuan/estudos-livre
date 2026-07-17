package br.com.estudalivre.studycycle;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
class StudyCycleActivationIntegrationTest {

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
    void firstActivationCreatesACursorlessRun() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID cycleId = createConfiguredCycle(principal, subjectId, "Reta final");

        mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                        .with(user(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cycleId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRun.number").value(1))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentRun.currentStagePosition").doesNotExist());

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRun.number").value(1))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentRun.currentStagePosition").doesNotExist());
    }

    @Test
    void switchingCyclesRequiresAnExplicitDecisionForTheCurrentRun() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID firstCycleId = createConfiguredCycle(principal, subjectId, "Reta final");
        UUID secondCycleId = createConfiguredCycle(principal, subjectId, "Manutenção");

        activate(principal, firstCycleId).andExpect(status().isOk());
        activate(principal, secondCycleId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Escolha como trocar de ciclo"));

        mockMvc.perform(get("/api/study-cycles").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status == 'ACTIVE')].id", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(firstCycleId), contains("ACTIVE")))
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(secondCycleId), contains("DRAFT")));
    }

    @Test
    void pausingAndReactivatingACycleResumesTheSameRun() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID pausedCycleId = createConfiguredCycle(principal, subjectId, "Reta final");
        UUID otherCycleId = createConfiguredCycle(principal, subjectId, "Manutenção");

        String firstActivation = activate(principal, pausedCycleId)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID originalRunId = UUID.fromString(JsonPath.read(firstActivation, "$.currentRun.id"));

        switchCycle(principal, otherCycleId, "PAUSE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/study-cycles/{id}", pausedCycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.currentRun.id").value(originalRunId.toString()))
                .andExpect(jsonPath("$.currentRun.status").value("PAUSED"));

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", pausedCycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Reta final ajustada",
                                  "stages":[{"subjectId":"%s","targetMinutes":90}]
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reta final ajustada"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.totalMinutes").value(90))
                .andExpect(jsonPath("$.currentRun.id").value(originalRunId.toString()))
                .andExpect(jsonPath("$.currentRun.status").value("PAUSED"));

        switchCycle(principal, pausedCycleId, "PAUSE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reta final ajustada"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRun.id").value(originalRunId.toString()))
                .andExpect(jsonPath("$.currentRun.number").value(1))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"));
    }

    @Test
    void abandoningTheCurrentRunStartsANewRunWhenTheCycleReturns() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID abandonedCycleId = createConfiguredCycle(principal, subjectId, "Reta final");
        UUID otherCycleId = createConfiguredCycle(principal, subjectId, "Manutenção");

        String firstActivation = activate(principal, abandonedCycleId)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID abandonedRunId = UUID.fromString(JsonPath.read(firstActivation, "$.currentRun.id"));

        switchCycle(principal, otherCycleId, "ABANDON")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/study-cycles/{id}", abandonedCycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.currentRun").doesNotExist());

        switchCycle(principal, abandonedCycleId, "PAUSE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRun.id").value(not(abandonedRunId.toString())))
                .andExpect(jsonPath("$.currentRun.number").value(2))
                .andExpect(jsonPath("$.currentRun.status").value("IN_PROGRESS"));
    }

    @Test
    void cycleWithoutStagesCannotBeActivated() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID cycleId = createCycle(principal, "Ainda vazio");

        activate(principal, cycleId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Ciclo ainda não pode ser ativado"));
    }

    @Test
    void userCannotActivateOrObserveAnotherUsersCycleRun() throws Exception {
        IdentityPrincipal owner = createUser("dona@example.com");
        IdentityPrincipal otherUser = createUser("outra@example.com");
        UUID subjectId = createSubject(owner, "Direito Administrativo");
        UUID ownerCycleId = createConfiguredCycle(owner, subjectId, "Ciclo privado");

        activate(otherUser, ownerCycleId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciclo não encontrado"));

        mockMvc.perform(get("/api/study-cycles").with(user(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/study-cycles/{id}", ownerCycleId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentRun").doesNotExist());
    }

    @Test
    void concurrentActivationsLeaveExactlyOneActiveCycleForTheUser() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Direito Administrativo");
        UUID firstCycleId = createConfiguredCycle(principal, subjectId, "Reta final");
        UUID secondCycleId = createConfiguredCycle(principal, subjectId, "Manutenção");
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
                        UUID cycleId = index % 2 == 0 ? firstCycleId : secondCycleId;
                        return mockMvc.perform(post("/api/study-cycles/{id}/activate", cycleId)
                                        .with(user(principal))
                                        .cookie(csrfCookie)
                                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"currentRunAction\":\"PAUSE\"}"))
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
            assertThat(statuses).containsOnly(200);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        mockMvc.perform(get("/api/study-cycles").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status == 'ACTIVE')].id", hasSize(1)));
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM study_cycle_run");
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

    private UUID createConfiguredCycle(IdentityPrincipal principal, UUID subjectId, String name) throws Exception {
        UUID cycleId = createCycle(principal, name);

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

    private org.springframework.test.web.servlet.ResultActions activate(
            IdentityPrincipal principal,
            UUID cycleId) throws Exception {
        return mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                .with(user(principal))));
    }

    private org.springframework.test.web.servlet.ResultActions switchCycle(
            IdentityPrincipal principal,
            UUID cycleId,
            String currentRunAction) throws Exception {
        return mockMvc.perform(withSpaCsrf(post("/api/study-cycles/{id}/activate", cycleId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentRunAction\":\"" + currentRunAction + "\"}")));
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
