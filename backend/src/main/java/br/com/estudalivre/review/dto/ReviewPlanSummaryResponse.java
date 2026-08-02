package br.com.estudalivre.review.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReviewPlanSummaryResponse(
        UUID id,
        String status,
        int version,
        String subjectName,
        String contentName,
        LocalDate initialStudyDate,
        int scheduledCount,
        int completedCount,
        int skippedCount,
        int canceledCount) {
}
