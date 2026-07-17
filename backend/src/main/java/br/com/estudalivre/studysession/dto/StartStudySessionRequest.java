package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartStudySessionRequest(
        @NotNull StudySessionOrigin origin,
        UUID subjectId,
        UUID contentId,
        UUID cycleId) {
}
