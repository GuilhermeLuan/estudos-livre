package br.com.estudalivre;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.estudalivre.testing.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SpaRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void directPasswordResetRouteLoadsTheReactApplication() throws Exception {
        mockMvc.perform(get("/redefinir-senha"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void directReviewsRouteLoadsTheReactApplication() throws Exception {
        mockMvc.perform(get("/revisoes").with(user("pessoa@example.com")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(forwardedUrl("/index.html"));
    }
}
