package br.com.estudalivre.review.model;

import java.time.LocalDate;
import java.util.List;

public record ReviewSchedule(int intervalDays, LocalDate dueDate) {

    private static final List<Integer> CANONICAL_INTERVALS = List.of(1, 7, 30, 60, 90, 120);

    public static List<ReviewSchedule> from(LocalDate initialStudyDate) {
        return CANONICAL_INTERVALS.stream()
                .map(interval -> new ReviewSchedule(interval, initialStudyDate.plusDays(interval)))
                .toList();
    }
}
