package br.com.estudalivre.studysession.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.studysession.dto.StartStudySessionRequest;
import br.com.estudalivre.studysession.dto.CreateManualStudySessionRequest;
import br.com.estudalivre.studysession.dto.FinishStudySessionRequest;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
import br.com.estudalivre.studysession.dto.UpdateExerciseResultRequest;
import br.com.estudalivre.studysession.dto.ExerciseSummaryResponse;
import br.com.estudalivre.studysession.service.StudySessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

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

    @PostMapping("/manual")
    public ResponseEntity<StudySessionResponse> createManual(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @Valid @RequestBody CreateManualStudySessionRequest request) {
        StudySessionResponse session = studySessionService.createManual(
                principal.id(), principal.timeZone(), request);
        return ResponseEntity.created(URI.create("/api/study-sessions/" + session.id())).body(session);
    }

    @GetMapping("/history")
    public List<StudySessionResponse> history(
            @AuthenticationPrincipal IdentityPrincipal principal) {
        return studySessionService.history(principal.id());
    }

    @GetMapping("/exercise-summary")
    public ExerciseSummaryResponse exerciseSummary(
            @AuthenticationPrincipal IdentityPrincipal principal) {
        return studySessionService.exerciseSummary(principal.id());
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

    @PostMapping("/{id}/finish")
    public StudySessionResponse finish(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody FinishStudySessionRequest request) {
        return studySessionService.finish(principal.id(), principal.timeZone(), id, request);
    }

    @PutMapping("/{id}/exercise-result")
    public StudySessionResponse updateExerciseResult(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseResultRequest request) {
        return studySessionService.updateExerciseResult(principal.id(), id, request);
    }
}
