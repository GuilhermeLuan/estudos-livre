package br.com.estudalivre.studysession.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;

public record DeleteStudySessionRequest(@NotNull @PositiveOrZero Integer expectedVersion) {
}
