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
class SuggestedStudyCycleIntegrationTest {

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
    void ownerCreatesAndReadsAnExplainableSuggestedCycle() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID portugueseId = createSubject(principal, "Português");
        UUID lawId = createSubject(principal, "Direito");
        UUID technologyId = createSubject(principal, "Tecnologia");

        String response = mockMvc.perform(withSpaCsrf(post("/api/study-cycles/suggestions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ciclo sugerido pós-edital",
                                  "subjects":[
                                    {"subjectId":"%s","questionCount":20,"weight":2,"difficulty":"EASY"},
                                    {"subjectId":"%s","questionCount":10,"weight":1,"difficulty":"HARD"},
                                    {"subjectId":"%s","questionCount":10,"weight":1,"difficulty":"EASY"}
                                  ]
                                }
                                """.formatted(portugueseId, lawId, technologyId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("SUGGESTED"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalMinutes").value(600))
                .andExpect(jsonPath("$.suggestion.totalMinutes").value(600))
                .andExpect(jsonPath("$.suggestion.durationRule").value("2h por matéria, limitado entre 10h e 30h"))
                .andExpect(jsonPath("$.suggestion.priorityRule").value("questões × peso × dificuldade"))
                .andExpect(jsonPath("$.suggestion.subjects[0].subjectName").value("Português"))
                .andExpect(jsonPath("$.suggestion.subjects[0].priority").value(40.00))
                .andExpect(jsonPath("$.suggestion.subjects[0].allocatedMinutes").value(320))
                .andExpect(jsonPath("$.suggestion.subjects[0].appearanceCount").value(2))
                .andExpect(jsonPath("$.stages.length()").value(4))
                .andExpect(jsonPath("$.stages[0].subjectName").value("Português"))
                .andExpect(jsonPath("$.stages[1].subjectName").value("Direito"))
                .andExpect(jsonPath("$.stages[2].subjectName").value("Português"))
                .andExpect(jsonPath("$.stages[3].subjectName").value("Tecnologia"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cycleId = UUID.fromString(JsonPath.read(response, "$.id"));
        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SUGGESTED"))
                .andExpect(jsonPath("$.suggestion.subjects[0].questionCount").value(20))
                .andExpect(jsonPath("$.suggestion.subjects[0].weight").value(2))
                .andExpect(jsonPath("$.suggestion.subjects[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.suggestion.subjects[1].priority").value(15.00))
                .andExpect(jsonPath("$.suggestion.subjects[2].allocatedMinutes").value(125));
    }

    @Test
    void editingGeneratedStagesTurnsTheSuggestionIntoACustomCycle() throws Exception {
        IdentityPrincipal principal = createUser("pessoa@example.com");
        UUID subjectId = createSubject(principal, "Português");
        String creation = mockMvc.perform(withSpaCsrf(post("/api/study-cycles/suggestions")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Sugestão inicial",
                                  "subjects":[
                                    {"subjectId":"%s","questionCount":20,"weight":2,"difficulty":"MEDIUM"}
                                  ]
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID cycleId = UUID.fromString(JsonPath.read(creation, "$.id"));

        mockMvc.perform(withSpaCsrf(put("/api/study-cycles/{id}", cycleId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Meu ciclo ajustado",
                                  "stages":[{"subjectId":"%s","targetMinutes":90}]
                                }
                                """.formatted(subjectId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CUSTOM"))
                .andExpect(jsonPath("$.suggestion").doesNotExist())
                .andExpect(jsonPath("$.totalMinutes").value(90));

        mockMvc.perform(get("/api/study-cycles/{id}", cycleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CUSTOM"))
                .andExpect(jsonPath("$.suggestion").doesNotExist());
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
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
