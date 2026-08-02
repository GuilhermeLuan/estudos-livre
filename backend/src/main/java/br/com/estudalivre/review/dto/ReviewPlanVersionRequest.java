package br.com.estudalivre.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReviewPlanVersionRequest(
        @NotNull @PositiveOrZero Integer expectedVersion) {
}
