package br.com.estudalivre.identity.service;

import br.com.estudalivre.identity.config.IdentityProperties;
import br.com.estudalivre.identity.repository.IdentityUser;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.repository.PasswordResetTokenRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetLinkService {

    private final PasswordResetTokenRepository tokenRepository;
    private final IdentityUserRepository identityUserRepository;
    private final Clock clock;
    private final IdentityProperties identityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetLinkService(
            PasswordResetTokenRepository tokenRepository,
            IdentityUserRepository identityUserRepository,
            Clock clock,
            IdentityProperties identityProperties) {
        this.tokenRepository = tokenRepository;
        this.identityUserRepository = identityUserRepository;
        this.clock = clock;
        this.identityProperties = identityProperties;
    }

    @Transactional
    public String generateLink(String email) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        IdentityUser user = identityUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(IdentityEmailNotFoundException::new);

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant createdAt = clock.instant();

        tokenRepository.deleteByUserId(user.id());
        tokenRepository.create(
                PasswordResetService.sha256(rawToken),
                user.id(),
                createdAt,
                createdAt.plus(Duration.ofMinutes(30)));

        String baseUrl = identityProperties.baseUrl().toString().replaceFirst("/+$", "");
        return baseUrl + "/redefinir-senha?token=" + rawToken;
    }
}
