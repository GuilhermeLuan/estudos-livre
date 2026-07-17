package br.com.estudalivre.studysession.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudySession(
        UUID id,
        UUID ownerId,
        String origin,
        String status,
        UUID subjectId,
        String subjectName,
        UUID contentId,
        String contentName,
        UUID cycleId,
        String cycleName,
        UUID cycleRunId,
        Integer runNumber,
        UUID cycleStageId,
        Integer stagePosition,
        Integer targetMinutes,
        OffsetDateTime startedAt,
        long measuredSeconds,
        Long effectiveSeconds,
        OffsetDateTime finishedAt,
        int version,
        OffsetDateTime serverNow) {
}
