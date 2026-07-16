package br.com.estudalivre.studycycle.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudyCycle(
        UUID id,
        UUID ownerId,
        String name,
        String mode,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
