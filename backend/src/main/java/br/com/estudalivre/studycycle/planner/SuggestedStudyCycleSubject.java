package br.com.estudalivre.studycycle.planner;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedStudyCycleSubject(
        UUID subjectId,
        String subjectName,
        int questionCount,
        int weight,
        StudyCycleDifficulty difficulty,
        BigDecimal priority,
        int allocatedMinutes,
        int appearanceCount) {
}
