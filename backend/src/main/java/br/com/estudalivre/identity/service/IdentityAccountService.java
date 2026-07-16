package br.com.estudalivre.identity.service;

import br.com.estudalivre.identity.controller.BootstrapAccountRequest;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityAccountService {

    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityAccountService(
            IdentityUserRepository identityUserRepository,
            PasswordEncoder passwordEncoder) {
        this.identityUserRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void create(BootstrapAccountRequest request) {
        String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);
        String validatedTimeZone = validateTimeZone(request.timeZone());

        try {
            identityUserRepository.create(
                    UUID.randomUUID(),
                    normalizedEmail,
                    passwordEncoder.encode(request.password()),
                    validatedTimeZone);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateIdentityEmailException();
        }
    }

    private String validateTimeZone(String timeZone) {
        try {
            return ZoneId.of(timeZone.strip()).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("O fuso horário deve ser um identificador IANA válido.", exception);
        }
    }
}
