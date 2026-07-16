package br.com.estudalivre.subject.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Content(
        UUID id,
        UUID subjectId,
        String name,
        OffsetDateTime archivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public boolean archived() {
        return archivedAt != null;
    }
}
