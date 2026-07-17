package br.com.estudalivre.studycycle.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.studycycle.dto.ActivateStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.CreateStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.CreateSuggestedStudyCycleRequest;
import br.com.estudalivre.studycycle.dto.StudyCycleResponse;
import br.com.estudalivre.studycycle.dto.UpdateStudyCycleRequest;
import br.com.estudalivre.studycycle.service.StudyCycleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-cycles")
public class StudyCycleController {

    private final StudyCycleService studyCycleService;

    public StudyCycleController(StudyCycleService studyCycleService) {
        this.studyCycleService = studyCycleService;
    }

    @PostMapping
    public ResponseEntity<StudyCycleResponse> create(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @Valid @RequestBody CreateStudyCycleRequest request) {
        StudyCycleResponse cycle = studyCycleService.create(principal.id(), request);
        return ResponseEntity.created(URI.create("/api/study-cycles/" + cycle.id())).body(cycle);
    }

    @PostMapping("/suggestions")
    public ResponseEntity<StudyCycleResponse> createSuggested(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @Valid @RequestBody CreateSuggestedStudyCycleRequest request) {
        StudyCycleResponse cycle = studyCycleService.createSuggested(principal.id(), request);
        return ResponseEntity.created(URI.create("/api/study-cycles/" + cycle.id())).body(cycle);
    }

    @GetMapping
    public List<StudyCycleResponse> list(@AuthenticationPrincipal IdentityPrincipal principal) {
        return studyCycleService.list(principal.id());
    }

    @GetMapping("/{id}")
    public StudyCycleResponse get(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return studyCycleService.get(principal.id(), id);
    }

    @PutMapping("/{id}")
    public StudyCycleResponse update(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudyCycleRequest request) {
        return studyCycleService.update(principal.id(), id, request);
    }

    @PutMapping("/{id}/suggestion")
    public StudyCycleResponse regenerateSuggestion(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateSuggestedStudyCycleRequest request) {
        return studyCycleService.regenerateSuggestion(principal.id(), id, request);
    }

    @PostMapping("/{id}/activate")
    public StudyCycleResponse activate(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) ActivateStudyCycleRequest request) {
        return studyCycleService.activate(
                principal.id(),
                id,
                request == null ? null : request.currentRunAction());
    }
}
