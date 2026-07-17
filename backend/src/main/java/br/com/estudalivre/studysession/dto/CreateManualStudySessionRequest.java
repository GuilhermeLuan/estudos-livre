package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateManualStudySessionRequest(
        @NotNull LocalDateTime startedAtLocal,
        @Positive long effectiveSeconds,
        @NotNull UUID subjectId,
        UUID contentId,
        @Size(max = 4000) String notes) {
}
