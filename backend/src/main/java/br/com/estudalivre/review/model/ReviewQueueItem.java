package br.com.estudalivre.review.model;

import java.time.LocalDate;
import java.util.UUID;

public record ReviewQueueItem(
        UUID occurrenceId,
        UUID planId,
        UUID subjectId,
        String subjectName,
        UUID contentId,
        String contentName,
        LocalDate initialStudyDate,
        int intervalDays,
        LocalDate dueDate) {
}
