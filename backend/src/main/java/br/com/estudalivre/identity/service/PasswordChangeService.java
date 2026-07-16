package br.com.estudalivre.identity.service;

import br.com.estudalivre.identity.controller.ChangePasswordRequest;
import br.com.estudalivre.identity.repository.IdentityUser;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@Service
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PasswordChangeService {

    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentitySessionService identitySessionService;

    public PasswordChangeService(
            IdentityUserRepository identityUserRepository,
            PasswordEncoder passwordEncoder,
            IdentitySessionService identitySessionService) {
        this.identityUserRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.identitySessionService = identitySessionService;
    }

    @Transactional
    public void change(UUID userId, String principalName, String currentSessionId, ChangePasswordRequest request) {
        IdentityUser user = identityUserRepository.findById(userId)
                .orElseThrow(CurrentPasswordIncorrectException::new);
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new CurrentPasswordIncorrectException();
        }

        identityUserRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        identitySessionService.invalidateOtherSessions(principalName, currentSessionId);
    }
}
