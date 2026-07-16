package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.model.StudyCycleRun;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudyCycleRunResponse(
        UUID id,
        int number,
        int currentStagePosition,
        OffsetDateTime startedAt) {

    public static StudyCycleRunResponse from(StudyCycleRun run) {
        return new StudyCycleRunResponse(
                run.id(),
                run.number(),
                run.currentStagePosition(),
                run.startedAt());
    }
}
