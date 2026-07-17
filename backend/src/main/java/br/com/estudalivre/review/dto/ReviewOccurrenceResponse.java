package br.com.estudalivre.review.dto;

import br.com.estudalivre.review.model.ReviewQueueItem;
import java.time.LocalDate;
import java.util.UUID;

public record ReviewOccurrenceResponse(
        UUID occurrenceId,
        UUID planId,
        UUID subjectId,
        String subjectName,
        UUID contentId,
        String contentName,
        LocalDate initialStudyDate,
        int intervalDays,
        LocalDate dueDate,
        ReviewTiming timing) {

    public static ReviewOccurrenceResponse from(ReviewQueueItem item, LocalDate today) {
        ReviewTiming timing = item.dueDate().isBefore(today)
                ? ReviewTiming.OVERDUE
                : item.dueDate().isEqual(today) ? ReviewTiming.TODAY : ReviewTiming.FUTURE;
        return new ReviewOccurrenceResponse(
                item.occurrenceId(),
                item.planId(),
                item.subjectId(),
                item.subjectName(),
                item.contentId(),
                item.contentName(),
                item.initialStudyDate(),
                item.intervalDays(),
                item.dueDate(),
                timing);
    }
}
