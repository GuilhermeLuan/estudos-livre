package br.com.estudalivre.subject.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Subject(
        UUID id,
        UUID ownerId,
        String name,
        OffsetDateTime archivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public boolean archived() {
        return archivedAt != null;
    }
}
