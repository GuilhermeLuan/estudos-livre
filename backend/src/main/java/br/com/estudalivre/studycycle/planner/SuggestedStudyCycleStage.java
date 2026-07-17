package br.com.estudalivre.studycycle.planner;

import java.util.UUID;

public record SuggestedStudyCycleStage(
        UUID subjectId,
        String subjectName,
        int targetMinutes) {
}
