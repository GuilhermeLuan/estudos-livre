package br.com.estudalivre.identity.controller;

import br.com.estudalivre.identity.config.IdentityProperties;
import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.service.IdentityBootstrapService;
import br.com.estudalivre.identity.service.IdentityAccountService;
import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.identity.service.RegistrationClosedException;
import br.com.estudalivre.identity.service.PasswordChangeService;
import br.com.estudalivre.identity.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/auth")
public class IdentityAuthenticationController {

    private final IdentityUserRepository identityUserRepository;
    private final IdentityBootstrapService identityBootstrapService;
    private final IdentityProperties identityProperties;
    private final IdentityAccountService identityAccountService;
    private final PasswordChangeService passwordChangeService;
    private final PasswordResetService passwordResetService;

    public IdentityAuthenticationController(
            IdentityUserRepository identityUserRepository,
            IdentityBootstrapService identityBootstrapService,
            IdentityProperties identityProperties,
            IdentityAccountService identityAccountService,
            PasswordChangeService passwordChangeService,
            PasswordResetService passwordResetService) {
        this.identityUserRepository = identityUserRepository;
        this.identityBootstrapService = identityBootstrapService;
        this.identityProperties = identityProperties;
        this.identityAccountService = identityAccountService;
        this.passwordChangeService = passwordChangeService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/bootstrap-status")
    public BootstrapStatusResponse bootstrapStatus() {
        return new BootstrapStatusResponse(
                !identityUserRepository.existsAny(),
                identityProperties.registrationEnabled());
    }

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public void bootstrap(@Valid @RequestBody BootstrapAccountRequest request) {
        identityBootstrapService.createFirstAccount(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody BootstrapAccountRequest request) {
        if (!identityProperties.registrationEnabled()) {
            throw new RegistrationClosedException();
        }
        identityAccountService.create(request);
    }

    @GetMapping("/me")
    public CurrentIdentityResponse currentIdentity(@AuthenticationPrincipal IdentityPrincipal principal) {
        return new CurrentIdentityResponse(principal.id(), principal.email(), principal.timeZone());
    }

    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal IdentityPrincipal principal,
            HttpSession session,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordChangeService.change(principal.id(), principal.email(), session.getId(), request);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
    }
}
