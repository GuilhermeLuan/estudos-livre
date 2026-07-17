package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.planner.StudyCycleDifficulty;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCycleSubject;
import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedStudyCycleSubjectResponse(
        UUID subjectId,
        String subjectName,
        int questionCount,
        int weight,
        StudyCycleDifficulty difficulty,
        BigDecimal priority,
        int allocatedMinutes,
        int appearanceCount) {

    public static SuggestedStudyCycleSubjectResponse from(SuggestedStudyCycleSubject subject) {
        return new SuggestedStudyCycleSubjectResponse(
                subject.subjectId(),
                subject.subjectName(),
                subject.questionCount(),
                subject.weight(),
                subject.difficulty(),
                subject.priority(),
                subject.allocatedMinutes(),
                subject.appearanceCount());
    }
}
