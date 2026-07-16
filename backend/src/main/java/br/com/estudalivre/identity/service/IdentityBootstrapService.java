package br.com.estudalivre.identity.service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

import br.com.estudalivre.identity.controller.BootstrapAccountRequest;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityBootstrapService {

    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityBootstrapService(
            IdentityUserRepository identityUserRepository,
            PasswordEncoder passwordEncoder) {
        this.identityUserRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createFirstAccount(BootstrapAccountRequest request) {
        identityUserRepository.lockBootstrap();
        if (identityUserRepository.existsAny()) {
            throw new BootstrapAlreadyCompletedException();
        }

        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);
        String validatedTimeZone = validateTimeZone(request.timeZone());

        identityUserRepository.create(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                validatedTimeZone);
    }

    private String validateTimeZone(String timeZone) {
        try {
            return ZoneId.of(timeZone.strip()).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("O fuso horário deve ser um identificador IANA válido.", exception);
        }
    }
}
