package br.com.estudalivre.identity.controller;

import br.com.estudalivre.identity.repository.IdentityUserRepository;
import br.com.estudalivre.identity.service.IdentityBootstrapService;
import br.com.estudalivre.identity.service.IdentityPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class IdentityAuthenticationController {

    private final IdentityUserRepository identityUserRepository;
    private final IdentityBootstrapService identityBootstrapService;

    public IdentityAuthenticationController(
            IdentityUserRepository identityUserRepository,
            IdentityBootstrapService identityBootstrapService) {
        this.identityUserRepository = identityUserRepository;
        this.identityBootstrapService = identityBootstrapService;
    }

    @GetMapping("/bootstrap-status")
    public BootstrapStatusResponse bootstrapStatus() {
        return new BootstrapStatusResponse(!identityUserRepository.existsAny());
    }

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    public void bootstrap(@Valid @RequestBody BootstrapAccountRequest request) {
        identityBootstrapService.createFirstAccount(request);
    }

    @GetMapping("/me")
    public CurrentIdentityResponse currentIdentity(@AuthenticationPrincipal IdentityPrincipal principal) {
        return new CurrentIdentityResponse(principal.id(), principal.email(), principal.timeZone());
    }
}
