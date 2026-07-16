package br.com.estudalivre.studycycle.dto;

import java.util.UUID;

public record StudyCycleStageResponse(
        UUID id,
        int position,
        UUID subjectId,
        String subjectName,
        int targetMinutes,
        boolean longBlockWarning) {
}
