package br.com.estudalivre.studysession.dto;

import br.com.estudalivre.studysession.model.StudySession;
import br.com.estudalivre.studysession.model.StudySessionCredit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudySessionResponse(
        UUID id,
        String origin,
        String status,
        SubjectReference subject,
        ContentReference content,
        CycleReference cycle,
        OffsetDateTime startedAt,
        String notes,
        long measuredSeconds,
        Long effectiveSeconds,
        OffsetDateTime finishedAt,
        int version,
        ExerciseResultReference exerciseResult,
        List<CreditReference> credits,
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

    public record CreditReference(
            UUID runStageId,
            UUID cycleId,
            UUID runId,
            UUID cycleStageId,
            int stagePosition,
            long creditedSeconds) {
    }

    public record ExerciseResultReference(
            int questionsAttempted,
            int questionsCorrect,
            BigDecimal accuracyPercentage) {
    }

    public static StudySessionResponse from(StudySession session, List<StudySessionCredit> credits) {
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
                session.notes(),
                session.measuredSeconds(),
                session.effectiveSeconds(),
                session.finishedAt(),
                session.version(),
                session.exerciseResult() == null
                        ? null
                        : new ExerciseResultReference(
                                session.exerciseResult().questionsAttempted(),
                                session.exerciseResult().questionsCorrect(),
                                session.exerciseResult().accuracyPercentage()),
                credits.stream()
                        .map(credit -> new CreditReference(
                                credit.runStageId(),
                                credit.cycleId(),
                                credit.runId(),
                                credit.cycleStageId(),
                                credit.stagePosition(),
                                credit.creditedSeconds()))
                        .toList(),
                session.serverNow());
    }
}
