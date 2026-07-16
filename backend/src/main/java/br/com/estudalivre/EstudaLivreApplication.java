package br.com.estudalivre;

import br.com.estudalivre.identity.command.PasswordResetLinkCommand;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EstudaLivreApplication {

    public static void main(String[] args) {
        SpringApplication application = applicationFor(args);
        ConfigurableApplicationContext context = application.run(args);
        if (isPasswordResetCommand(args)) {
            System.exit(SpringApplication.exit(context));
        }
    }

    public static SpringApplication applicationFor(String[] args) {
        SpringApplication application = new SpringApplication(EstudaLivreApplication.class);
        if (isPasswordResetCommand(args)) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }
        return application;
    }

    private static boolean isPasswordResetCommand(String[] args) {
        return Arrays.asList(args).contains(PasswordResetLinkCommand.COMMAND);
    }
}
