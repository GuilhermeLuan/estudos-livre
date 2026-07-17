package br.com.estudalivre.review.model;

import java.time.LocalDate;
import java.util.UUID;

public record ReviewExecutionContext(
        UUID occurrenceId,
        UUID planId,
        UUID subjectId,
        UUID contentId,
        LocalDate dueDate) {
}
