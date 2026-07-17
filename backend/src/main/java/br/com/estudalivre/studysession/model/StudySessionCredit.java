package br.com.estudalivre.studysession.model;

import java.util.UUID;

public record StudySessionCredit(
        UUID runStageId,
        UUID cycleId,
        UUID runId,
        UUID cycleStageId,
        int stagePosition,
        long creditedSeconds) {
}
