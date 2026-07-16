package br.com.estudalivre;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.estudalivre.identity.command.PasswordResetLinkCommand;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.service.PasswordResetLinkService;
import br.com.estudalivre.testing.IntegrationTest;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.WebApplicationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = "app.base-url=https://estudos.example")
class PasswordResetLinkCommandIntegrationTest {

    @Autowired
    private PasswordResetLinkService passwordResetLinkService;

    @Autowired
    private IdentityUserRepository identityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createUser() {
        jdbcTemplate.update("DELETE FROM password_reset_token");
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM identity_user");
        identityUserRepository.create(
                UUID.randomUUID(),
                "pessoa@example.com",
                passwordEncoder.encode("uma frase senha segura"),
                "America/Sao_Paulo");
    }

    @Test
    void resetCommandUsesANonWebApplication() {
        assertThat(EstudaLivreApplication.applicationFor(
                new String[]{"generate-password-reset-link", "--email=pessoa@example.com"})
                .getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
    }

    @Test
    void runnerPrintsTheConfiguredResetLink() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        PasswordResetLinkCommand command = new PasswordResetLinkCommand(
                passwordResetLinkService,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));

        command.run(new DefaultApplicationArguments(
                "generate-password-reset-link", "--email=pessoa@example.com"));

        assertThat(command.getExitCode()).isZero();
        assertThat(output.toString(StandardCharsets.UTF_8))
                .startsWith("https://estudos.example/redefinir-senha?token=");
        assertThat(error.toString(StandardCharsets.UTF_8)).isBlank();
    }

    @Test
    void runnerReturnsANonZeroExitCodeForInvalidInput() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        PasswordResetLinkCommand command = new PasswordResetLinkCommand(
                passwordResetLinkService,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));

        command.run(new DefaultApplicationArguments("generate-password-reset-link"));

        assertThat(command.getExitCode()).isEqualTo(1);
        assertThat(output.toString(StandardCharsets.UTF_8)).isBlank();
        assertThat(error.toString(StandardCharsets.UTF_8)).contains("--email=pessoa@example.com");
    }
}
