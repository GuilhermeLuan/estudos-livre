package br.com.estudalivre.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateReviewScheduleRequest(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @NotEmpty List<@Valid Occurrence> occurrences) {

    public record Occurrence(
            @NotNull UUID occurrenceId,
            @NotNull LocalDate dueDate) {
    }
}
