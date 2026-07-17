package br.com.estudalivre.studysession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.estudalivre.studysession.model.ExerciseResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ExerciseResultTest {

    @Test
    void treatsEmptyAndZeroAttemptsAsNoExerciseResult() {
        assertThat(ExerciseResult.optional(null, null)).isEmpty();
        assertThat(ExerciseResult.optional(0, null)).isEmpty();
        assertThat(ExerciseResult.optional(0, 0)).isEmpty();
    }

    @Test
    void rejectsIncompleteNegativeOrImpossibleCounts() {
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(10, null));
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(null, 8));
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(-1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(10, -1));
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(10, 11));
        assertThatIllegalArgumentException().isThrownBy(() -> ExerciseResult.optional(0, 1));
    }

    @Test
    void calculatesAccuracyWithOneDecimalAndHalfUpRounding() {
        ExerciseResult result = ExerciseResult.optional(9, 7).orElseThrow();

        assertThat(result.accuracyPercentage()).isEqualByComparingTo(new BigDecimal("77.8"));
    }
}
