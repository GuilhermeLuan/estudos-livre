package br.com.estudalivre.studysession.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.studysession.dto.StartStudySessionRequest;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
import br.com.estudalivre.studysession.service.StudySessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @GetMapping("/current")
    public ResponseEntity<StudySessionResponse> current(
            @AuthenticationPrincipal IdentityPrincipal principal) {
        return studySessionService.current(principal.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<StudySessionResponse> start(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @Valid @RequestBody StartStudySessionRequest request) {
        StudySessionResponse session = studySessionService.start(principal.id(), request);
        return ResponseEntity.created(URI.create("/api/study-sessions/" + session.id())).body(session);
    }

    @PostMapping("/{id}/pause")
    public StudySessionResponse pause(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return studySessionService.pause(principal.id(), id);
    }

    @PostMapping("/{id}/resume")
    public StudySessionResponse resume(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return studySessionService.resume(principal.id(), id);
    }
}
