package br.com.estudalivre.studysession.dto;

import br.com.estudalivre.studysession.model.StudySession;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudySessionResponse(
        UUID id,
        String origin,
        String status,
        SubjectReference subject,
        ContentReference content,
        CycleReference cycle,
        OffsetDateTime startedAt,
        long measuredSeconds,
        OffsetDateTime serverNow) {

    public record SubjectReference(UUID id, String name) {
    }

    public record ContentReference(UUID id, String name) {
    }

    public record CycleReference(
            UUID id,
            String name,
            UUID runId,
            int runNumber,
            UUID stageId,
            int stagePosition,
            int targetMinutes) {
    }

    public static StudySessionResponse from(StudySession session) {
        return new StudySessionResponse(
                session.id(),
                session.origin(),
                session.status(),
                new SubjectReference(session.subjectId(), session.subjectName()),
                session.contentId() == null
                        ? null
                        : new ContentReference(session.contentId(), session.contentName()),
                session.cycleId() == null
                        ? null
                        : new CycleReference(
                                session.cycleId(),
                                session.cycleName(),
                                session.cycleRunId(),
                                session.runNumber(),
                                session.cycleStageId(),
                                session.stagePosition(),
                                session.targetMinutes()),
                session.startedAt(),
                session.measuredSeconds(),
                session.serverNow());
    }
}
