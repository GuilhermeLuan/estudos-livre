package br.com.estudalivre.review.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.review.dto.ReviewOccurrenceResponse;
import br.com.estudalivre.review.service.ReviewService;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
import br.com.estudalivre.studysession.service.StudySessionService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final StudySessionService studySessionService;

    public ReviewController(
            ReviewService reviewService,
            StudySessionService studySessionService) {
        this.reviewService = reviewService;
        this.studySessionService = studySessionService;
    }

    @GetMapping
    public List<ReviewOccurrenceResponse> list(@AuthenticationPrincipal IdentityPrincipal principal) {
        return reviewService.list(principal.id(), principal.timeZone());
    }

    @PostMapping("/{occurrenceId}/start")
    public ResponseEntity<StudySessionResponse> start(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID occurrenceId) {
        UUID sessionId = reviewService.start(principal.id(), principal.timeZone(), occurrenceId);
        StudySessionResponse session = studySessionService.get(principal.id(), sessionId);
        return ResponseEntity.created(URI.create("/api/study-sessions/" + session.id())).body(session);
    }
}
