package br.com.estudalivre.review.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReviewScheduleTest {

    @Test
    void anchorsCanonicalIntervalsToTheInitialLocalStudyDate() {
        LocalDate initialStudyDate = LocalDate.of(2026, 12, 31);

        assertThat(ReviewSchedule.from(initialStudyDate))
                .containsExactly(
                        new ReviewSchedule(1, LocalDate.of(2027, 1, 1)),
                        new ReviewSchedule(7, LocalDate.of(2027, 1, 7)),
                        new ReviewSchedule(30, LocalDate.of(2027, 1, 30)),
                        new ReviewSchedule(60, LocalDate.of(2027, 3, 1)),
                        new ReviewSchedule(90, LocalDate.of(2027, 3, 31)),
                        new ReviewSchedule(120, LocalDate.of(2027, 4, 30)));
    }
}
