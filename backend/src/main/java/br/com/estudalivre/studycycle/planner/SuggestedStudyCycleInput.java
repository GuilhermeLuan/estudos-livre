package br.com.estudalivre.studycycle.planner;

import java.util.UUID;

public record SuggestedStudyCycleInput(
        UUID subjectId,
        String subjectName,
        int questionCount,
        int weight,
        StudyCycleDifficulty difficulty) {
}
