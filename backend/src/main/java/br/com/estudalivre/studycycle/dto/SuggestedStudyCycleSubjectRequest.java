package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.planner.StudyCycleDifficulty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record SuggestedStudyCycleSubjectRequest(
        @NotNull UUID subjectId,
        @Positive int questionCount,
        @Positive int weight,
        @NotNull StudyCycleDifficulty difficulty) {
}
