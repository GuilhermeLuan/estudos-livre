package br.com.estudalivre.review.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReviewPlanDetailRow(
        UUID planId,
        String planStatus,
        int planVersion,
        UUID subjectId,
        String subjectName,
        UUID contentId,
        String contentName,
        LocalDate initialStudyDate,
        UUID occurrenceId,
        int intervalDays,
        LocalDate dueDate,
        String occurrenceStatus,
        Instant resolvedAt,
        boolean inProgress) {
}
