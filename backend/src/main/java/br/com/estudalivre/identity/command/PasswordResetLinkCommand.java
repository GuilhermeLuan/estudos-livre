package br.com.estudalivre.identity.command;

import br.com.estudalivre.identity.service.PasswordResetLinkService;
import java.io.PrintStream;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class PasswordResetLinkCommand implements ApplicationRunner, ExitCodeGenerator {

    public static final String COMMAND = "generate-password-reset-link";

    private final PasswordResetLinkService passwordResetLinkService;
    private final PrintStream output;
    private final PrintStream error;
    private int exitCode;

    @Autowired
    public PasswordResetLinkCommand(PasswordResetLinkService passwordResetLinkService) {
        this(passwordResetLinkService, System.out, System.err);
    }

    public PasswordResetLinkCommand(
            PasswordResetLinkService passwordResetLinkService,
            PrintStream output,
            PrintStream error) {
        this.passwordResetLinkService = passwordResetLinkService;
        this.output = output;
        this.error = error;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!arguments.getNonOptionArgs().contains(COMMAND)) {
            return;
        }

        try {
            List<String> emails = arguments.getOptionValues("email");
            if (emails == null || emails.size() != 1 || emails.getFirst().isBlank()) {
                throw new IllegalArgumentException("Informe exatamente um e-mail com --email=pessoa@example.com.");
            }
            output.println(passwordResetLinkService.generateLink(emails.getFirst()));
        } catch (RuntimeException exception) {
            exitCode = 1;
            error.println(exception.getMessage());
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
