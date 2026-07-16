package br.com.estudalivre.subject.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.subject.dto.ContentNameRequest;
import br.com.estudalivre.subject.dto.ContentResponse;
import br.com.estudalivre.subject.model.SubjectStatus;
import br.com.estudalivre.subject.service.ContentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/subjects/{subjectId}/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping
    public ResponseEntity<ContentResponse> create(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @Valid @RequestBody ContentNameRequest request) {
        ContentResponse content = contentService.create(principal.id(), subjectId, request);
        return ResponseEntity.created(URI.create(
                "/api/subjects/" + subjectId + "/contents/" + content.id())).body(content);
    }

    @GetMapping
    public List<ContentResponse> list(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @RequestParam(defaultValue = "active") String status) {
        return contentService.list(principal.id(), subjectId, SubjectStatus.from(status));
    }

    @GetMapping("/{contentId}")
    public ContentResponse get(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @PathVariable UUID contentId) {
        return contentService.get(principal.id(), subjectId, contentId);
    }

    @PutMapping("/{contentId}")
    public ContentResponse update(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @PathVariable UUID contentId,
            @Valid @RequestBody ContentNameRequest request) {
        return contentService.update(principal.id(), subjectId, contentId, request);
    }

    @PostMapping("/{contentId}/archive")
    public ContentResponse archive(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @PathVariable UUID contentId) {
        return contentService.archive(principal.id(), subjectId, contentId);
    }

    @PostMapping("/{contentId}/restore")
    public ContentResponse restore(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID subjectId,
            @PathVariable UUID contentId) {
        return contentService.restore(principal.id(), subjectId, contentId);
    }
}
