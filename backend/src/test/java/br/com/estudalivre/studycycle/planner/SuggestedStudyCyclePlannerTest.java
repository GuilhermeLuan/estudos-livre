package br.com.estudalivre.studycycle.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SuggestedStudyCyclePlannerTest {

    private final SuggestedStudyCyclePlanner planner = new SuggestedStudyCyclePlanner();

    @Test
    void allocatesTheMinimumBeforeDistributingTheTenHourCycleByPriority() {
        var portugueseId = UUID.randomUUID();
        var lawId = UUID.randomUUID();
        var technologyId = UUID.randomUUID();

        SuggestedStudyCyclePlan plan = planner.generate(List.of(
                input(portugueseId, "Português", 20, 2, StudyCycleDifficulty.EASY),
                input(lawId, "Direito", 10, 1, StudyCycleDifficulty.HARD),
                input(technologyId, "Tecnologia", 10, 1, StudyCycleDifficulty.EASY)));

        assertThat(plan.totalMinutes()).isEqualTo(600);
        assertThat(plan.subjects())
                .extracting(SuggestedStudyCycleSubject::subjectId,
                        SuggestedStudyCycleSubject::priority,
                        SuggestedStudyCycleSubject::allocatedMinutes)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(portugueseId, new BigDecimal("40.00"), 320),
                        org.assertj.core.groups.Tuple.tuple(lawId, new BigDecimal("15.00"), 155),
                        org.assertj.core.groups.Tuple.tuple(technologyId, new BigDecimal("10.00"), 125));
    }

    @Test
    void splitsAllocationsAboveThreeHoursIntoBalancedAppearances() {
        var portugueseId = UUID.randomUUID();

        SuggestedStudyCyclePlan plan = planner.generate(List.of(
                input(portugueseId, "Português", 20, 2, StudyCycleDifficulty.MEDIUM)));

        assertThat(plan.subjects().getFirst().appearanceCount()).isEqualTo(4);
        assertThat(plan.stages())
                .extracting(SuggestedStudyCycleStage::subjectId, SuggestedStudyCycleStage::targetMinutes)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(portugueseId, 150),
                        org.assertj.core.groups.Tuple.tuple(portugueseId, 150),
                        org.assertj.core.groups.Tuple.tuple(portugueseId, 150),
                        org.assertj.core.groups.Tuple.tuple(portugueseId, 150));
    }

    @Test
    void ordersAppearancesByRemainingLoadWithoutRepeatingASubjectWhenThereIsAnAlternative() {
        var portugueseId = UUID.randomUUID();
        var lawId = UUID.randomUUID();
        var technologyId = UUID.randomUUID();

        SuggestedStudyCyclePlan plan = planner.generate(List.of(
                input(portugueseId, "Português", 20, 2, StudyCycleDifficulty.EASY),
                input(lawId, "Direito", 10, 1, StudyCycleDifficulty.HARD),
                input(technologyId, "Tecnologia", 10, 1, StudyCycleDifficulty.EASY)));

        assertThat(plan.stages())
                .extracting(SuggestedStudyCycleStage::subjectId)
                .containsExactly(portugueseId, lawId, portugueseId, technologyId);
    }

    @Test
    void rejectsMoreSubjectsThanTheThirtyHourLimitCanGiveTheOneHourMinimum() {
        List<SuggestedStudyCycleInput> inputs = IntStream.rangeClosed(1, 31)
                .mapToObj(index -> input(
                        UUID.randomUUID(), "Matéria " + index, 10, 1, StudyCycleDifficulty.EASY))
                .toList();

        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.generate(inputs))
                .withMessage("O ciclo sugerido aceita entre 1 e 30 matérias.");
    }

    @Test
    void rejectsNonPositiveQuestionCountsAndWeights() {
        var invalidInput = input(
                UUID.randomUUID(), "Português", 0, 1, StudyCycleDifficulty.EASY);

        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> planner.generate(List.of(invalidInput)))
                .withMessage("Questões e peso devem ser positivos.");
    }

    @Test
    void preservesPlannerInvariantsAndDeterminismAcrossTheSupportedCycleSizes() {
        for (int subjectCount = 1; subjectCount <= 30; subjectCount++) {
            int currentSubjectCount = subjectCount;
            List<SuggestedStudyCycleInput> inputs = IntStream.range(0, currentSubjectCount)
                    .mapToObj(index -> input(
                            UUID.nameUUIDFromBytes((currentSubjectCount + "-" + index).getBytes(StandardCharsets.UTF_8)),
                            "Matéria " + index,
                            index + 1,
                            index % 5 + 1,
                            StudyCycleDifficulty.values()[index % StudyCycleDifficulty.values().length]))
                    .toList();

            SuggestedStudyCyclePlan plan = planner.generate(inputs);
            int expectedTotal = Math.clamp(currentSubjectCount * 120, 600, 1800);

            assertThat(plan.totalMinutes()).as("total com %s matérias", currentSubjectCount).isEqualTo(expectedTotal);
            assertThat(plan.subjects()).allSatisfy(subject -> {
                assertThat(subject.allocatedMinutes()).isGreaterThanOrEqualTo(60);
                assertThat(subject.allocatedMinutes() % 5).isZero();
            });
            assertThat(plan.subjects()).extracting(SuggestedStudyCycleSubject::allocatedMinutes)
                    .satisfies(minutes -> assertThat(minutes.stream().mapToInt(Integer::intValue).sum()).isEqualTo(expectedTotal));
            assertThat(plan.stages()).allSatisfy(stage -> {
                assertThat(stage.targetMinutes()).isBetween(5, 180);
                assertThat(stage.targetMinutes() % 5).isZero();
            });
            assertThat(plan.stages()).extracting(SuggestedStudyCycleStage::targetMinutes)
                    .satisfies(minutes -> assertThat(minutes.stream().mapToInt(Integer::intValue).sum()).isEqualTo(expectedTotal));
            assertThat(plan.stages()).extracting(SuggestedStudyCycleStage::subjectId)
                    .containsAll(inputs.stream().map(SuggestedStudyCycleInput::subjectId).toList());
            assertThat(planner.generate(inputs)).isEqualTo(plan);
        }
    }

    private SuggestedStudyCycleInput input(
            UUID subjectId,
            String subjectName,
            int questionCount,
            int weight,
            StudyCycleDifficulty difficulty) {
        return new SuggestedStudyCycleInput(subjectId, subjectName, questionCount, weight, difficulty);
    }
}
