package br.com.estudalivre.identity.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record IdentityProperties(boolean registrationEnabled, URI baseUrl) {
}
