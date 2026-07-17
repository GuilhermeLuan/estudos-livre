package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.planner.SuggestedStudyCycleSubject;
import java.util.List;

public record StudyCycleSuggestionResponse(
        int totalMinutes,
        String durationRule,
        String priorityRule,
        List<SuggestedStudyCycleSubjectResponse> subjects) {

    private static final String DURATION_RULE = "2h por matéria, limitado entre 10h e 30h";
    private static final String PRIORITY_RULE = "questões × peso × dificuldade";

    public static StudyCycleSuggestionResponse from(
            int totalMinutes,
            List<SuggestedStudyCycleSubject> subjects) {
        return new StudyCycleSuggestionResponse(
                totalMinutes,
                DURATION_RULE,
                PRIORITY_RULE,
                subjects.stream().map(SuggestedStudyCycleSubjectResponse::from).toList());
    }
}
