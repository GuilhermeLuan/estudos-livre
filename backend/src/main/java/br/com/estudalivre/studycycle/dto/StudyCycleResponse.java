package br.com.estudalivre.studycycle.dto;

import br.com.estudalivre.studycycle.model.StudyCycle;
import br.com.estudalivre.studycycle.model.StudyCycleStage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudyCycleResponse(
        UUID id,
        String name,
        String mode,
        String status,
        int totalMinutes,
        boolean activatable,
        List<StudyCycleStageResponse> stages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static StudyCycleResponse from(StudyCycle cycle, List<StudyCycleStage> stages) {
        List<StudyCycleStageResponse> stageResponses = stages.stream()
                .map(stage -> new StudyCycleStageResponse(
                        stage.id(),
                        stage.position(),
                        stage.subjectId(),
                        stage.subjectName(),
                        stage.targetMinutes(),
                        stage.longBlockWarning()))
                .toList();
        return new StudyCycleResponse(
                cycle.id(),
                cycle.name(),
                cycle.mode(),
                cycle.status(),
                stages.stream().mapToInt(StudyCycleStage::targetMinutes).sum(),
                !stages.isEmpty(),
                stageResponses,
                cycle.createdAt(),
                cycle.updatedAt());
    }
}
