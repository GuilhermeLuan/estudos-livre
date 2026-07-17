package br.com.estudalivre.studysession.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public record ExerciseResult(int questionsAttempted, int questionsCorrect) {

    public ExerciseResult {
        if (questionsAttempted <= 0) {
            throw new IllegalArgumentException("Questions attempted must be greater than zero");
        }
        if (questionsCorrect < 0) {
            throw new IllegalArgumentException("Questions correct cannot be negative");
        }
        if (questionsCorrect > questionsAttempted) {
            throw new IllegalArgumentException("Questions correct cannot exceed questions attempted");
        }
    }

    public static Optional<ExerciseResult> optional(Integer questionsAttempted, Integer questionsCorrect) {
        if (questionsAttempted == null && questionsCorrect == null) {
            return Optional.empty();
        }
        if (questionsAttempted != null && questionsAttempted == 0
                && (questionsCorrect == null || questionsCorrect == 0)) {
            return Optional.empty();
        }
        if (questionsAttempted == null || questionsCorrect == null) {
            throw new IllegalArgumentException("Questions attempted and correct must be informed together");
        }
        return Optional.of(new ExerciseResult(questionsAttempted, questionsCorrect));
    }

    public BigDecimal accuracyPercentage() {
        return accuracyPercentage(questionsAttempted, questionsCorrect);
    }

    public static BigDecimal accuracyPercentage(long questionsAttempted, long questionsCorrect) {
        if (questionsAttempted <= 0 || questionsCorrect < 0 || questionsCorrect > questionsAttempted) {
            throw new IllegalArgumentException("Invalid aggregate exercise counts");
        }
        return BigDecimal.valueOf(questionsCorrect)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questionsAttempted), 1, RoundingMode.HALF_UP);
    }
}
