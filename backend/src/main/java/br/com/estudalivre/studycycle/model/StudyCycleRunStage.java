package br.com.estudalivre.studycycle.model;

import java.util.UUID;

public record StudyCycleRunStage(
        UUID id,
        UUID runId,
        UUID cycleId,
        UUID sourceStageId,
        UUID subjectId,
        String subjectName,
        int position,
        long targetSeconds,
        long creditedSeconds) {

    public long remainingSeconds() {
        return Math.max(0, targetSeconds - creditedSeconds);
    }
}
