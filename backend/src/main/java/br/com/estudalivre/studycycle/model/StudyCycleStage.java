package br.com.estudalivre.studycycle.model;

import java.util.UUID;

public record StudyCycleStage(
        UUID id,
        UUID cycleId,
        UUID subjectId,
        String subjectName,
        int position,
        int targetMinutes) {

    public boolean longBlockWarning() {
        return targetMinutes > 180;
    }
}
