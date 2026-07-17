package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateExerciseResultRequest(
        @PositiveOrZero Integer questionsAttempted,
        @PositiveOrZero Integer questionsCorrect) {
}
