package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.model.StudyCycleRun;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudyCycleRunResponse(
        UUID id,
        int number,
        String status,
        OffsetDateTime startedAt,
        int currentStagePosition) {

    public static StudyCycleRunResponse from(StudyCycleRun run, int currentStagePosition) {
        return new StudyCycleRunResponse(
                run.id(),
                run.number(),
                run.status(),
                run.startedAt(),
                currentStagePosition);
    }
}
