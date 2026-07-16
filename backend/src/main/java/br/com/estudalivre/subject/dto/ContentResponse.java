package br.com.estudalivre.subject.dto;

import br.com.estudalivre.subject.model.Content;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentResponse(
        UUID id,
        UUID subjectId,
        String name,
        boolean archived,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ContentResponse from(Content content) {
        return new ContentResponse(
                content.id(),
                content.subjectId(),
                content.name(),
                content.archived(),
                content.createdAt(),
                content.updatedAt());
    }
}
