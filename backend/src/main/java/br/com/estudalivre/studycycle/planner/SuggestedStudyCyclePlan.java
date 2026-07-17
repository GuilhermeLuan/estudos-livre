package br.com.estudalivre.studycycle.planner;

import java.util.List;

public record SuggestedStudyCyclePlan(
        int totalMinutes,
        List<SuggestedStudyCycleSubject> subjects,
        List<SuggestedStudyCycleStage> stages) {
}
