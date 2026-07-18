package br.com.estudalivre.review.dto;

import br.com.estudalivre.review.model.ReviewPlanDetailRow;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReviewPlanResponse(
        UUID id,
        String status,
        int version,
        Reference subject,
        Reference content,
        LocalDate initialStudyDate,
        List<Occurrence> occurrences) {

    public static ReviewPlanResponse from(List<ReviewPlanDetailRow> rows) {
        ReviewPlanDetailRow plan = rows.getFirst();
        return new ReviewPlanResponse(
                plan.planId(),
                plan.planStatus(),
                plan.planVersion(),
                new Reference(plan.subjectId(), plan.subjectName()),
                new Reference(plan.contentId(), plan.contentName()),
                plan.initialStudyDate(),
                rows.stream().map(Occurrence::from).toList());
    }

    public record Reference(UUID id, String name) {
    }

    public record Occurrence(
            UUID id,
            int intervalDays,
            LocalDate dueDate,
            String status,
            Instant resolvedAt,
            boolean inProgress) {

        static Occurrence from(ReviewPlanDetailRow row) {
            return new Occurrence(
                    row.occurrenceId(),
                    row.intervalDays(),
                    row.dueDate(),
                    row.occurrenceStatus(),
                    row.resolvedAt(),
                    row.inProgress());
        }
    }
}
