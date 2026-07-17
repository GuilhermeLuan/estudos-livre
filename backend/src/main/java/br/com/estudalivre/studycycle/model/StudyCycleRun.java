package br.com.estudalivre.studycycle.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudyCycleRun(
        UUID id,
        UUID cycleId,
        int number,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
