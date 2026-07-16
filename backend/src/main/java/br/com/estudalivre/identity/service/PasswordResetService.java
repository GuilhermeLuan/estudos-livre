package br.com.estudalivre.identity.service;

import br.com.estudalivre.identity.controller.ResetPasswordRequest;
import br.com.estudalivre.identity.repository.PasswordResetToken;
import br.com.estudalivre.identity.repository.PasswordResetTokenRepository;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@Service
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentitySessionService identitySessionService;
    private final Clock clock;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            IdentityUserRepository identityUserRepository,
            PasswordEncoder passwordEncoder,
            IdentitySessionService identitySessionService,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.identityUserRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.identitySessionService = identitySessionService;
        this.clock = clock;
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        String tokenHash = sha256(request.token());
        PasswordResetToken token = tokenRepository.lockUsable(tokenHash, clock.instant())
                .orElseThrow(PasswordResetTokenInvalidException::new);

        identityUserRepository.updatePassword(token.userId(), passwordEncoder.encode(request.newPassword()));
        tokenRepository.delete(tokenHash);
        identitySessionService.invalidateAllSessions(token.email());
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }
}
