package br.com.estudalivre.subject.controller;

import br.com.estudalivre.identity.service.IdentityPrincipal;
import br.com.estudalivre.subject.dto.SubjectNameRequest;
import br.com.estudalivre.subject.dto.SubjectResponse;
import br.com.estudalivre.subject.service.SubjectService;
import br.com.estudalivre.subject.model.SubjectStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> create(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @Valid @RequestBody SubjectNameRequest request) {
        SubjectResponse subject = subjectService.create(principal.id(), request);
        return ResponseEntity.created(URI.create("/api/subjects/" + subject.id())).body(subject);
    }

    @GetMapping
    public List<SubjectResponse> list(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @RequestParam(defaultValue = "active") String status) {
        return subjectService.list(principal.id(), SubjectStatus.from(status));
    }

    @GetMapping("/{id}")
    public SubjectResponse get(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return subjectService.get(principal.id(), id);
    }

    @PutMapping("/{id}")
    public SubjectResponse update(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SubjectNameRequest request) {
        return subjectService.update(principal.id(), id, request);
    }

    @PostMapping("/{id}/archive")
    public SubjectResponse archive(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return subjectService.archive(principal.id(), id);
    }

    @PostMapping("/{id}/restore")
    public SubjectResponse restore(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @PathVariable UUID id) {
        return subjectService.restore(principal.id(), id);
    }
}
