package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;

public record UpdateExerciseResultRequest(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @PositiveOrZero Integer questionsAttempted,
        @PositiveOrZero Integer questionsCorrect) {
}
