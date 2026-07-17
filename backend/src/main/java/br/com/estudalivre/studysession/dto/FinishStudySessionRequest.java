package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FinishStudySessionRequest(
        @NotNull @PositiveOrZero Long effectiveSeconds,
        @NotNull @PositiveOrZero Integer expectedVersion,
        @PositiveOrZero Integer questionsAttempted,
        @PositiveOrZero Integer questionsCorrect,
        Boolean scheduleReviews) {
}
