package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.model.StudyCycleRun;
import br.com.estudalivre.studycycle.model.StudyCycleRunStage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudyCycleRunHistoryResponse(
        UUID id,
        int number,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<StageSnapshot> stages) {

    public static StudyCycleRunHistoryResponse from(
            StudyCycleRun run,
            List<StudyCycleRunStage> stages) {
        return new StudyCycleRunHistoryResponse(
                run.id(),
                run.number(),
                run.status(),
                run.startedAt(),
                run.endedAt(),
                stages.stream().map(StageSnapshot::from).toList());
    }

    public record StageSnapshot(
            UUID id,
            UUID sourceStageId,
            int position,
            UUID subjectId,
            String subjectName,
            long targetSeconds,
            long creditedSeconds,
            boolean completed) {

        private static StageSnapshot from(StudyCycleRunStage stage) {
            return new StageSnapshot(
                    stage.id(),
                    stage.sourceStageId(),
                    stage.position(),
                    stage.subjectId(),
                    stage.subjectName(),
                    stage.targetSeconds(),
                    stage.creditedSeconds(),
                    stage.creditedSeconds() >= stage.targetSeconds());
        }
    }
}
