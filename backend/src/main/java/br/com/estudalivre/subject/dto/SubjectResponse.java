package br.com.estudalivre.subject.dto;

import br.com.estudalivre.subject.model.Subject;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubjectResponse(
        UUID id,
        String name,
        boolean archived,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.id(),
                subject.name(),
                subject.archived(),
                subject.createdAt(),
                subject.updatedAt());
    }
}
