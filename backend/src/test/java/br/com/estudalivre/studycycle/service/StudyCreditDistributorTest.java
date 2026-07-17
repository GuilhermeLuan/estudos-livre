package br.com.estudalivre.studycycle.service;

import br.com.estudalivre.studycycle.model.StudyCycleRunStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudyCreditDistributorTest {

    private final StudyCreditDistributor distributor = new StudyCreditDistributor();

    @Test
    void distributesCreditAcrossOrderedStagesOfTheSameSubject() {
        UUID runId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        UUID mathematicsId = UUID.randomUUID();
        UUID portugueseId = UUID.randomUUID();
        StudyCycleRunStage firstMathematics = stage(runId, cycleId, mathematicsId, 1, 60, 30);
        StudyCycleRunStage portuguese = stage(runId, cycleId, portugueseId, 2, 60, 0);
        StudyCycleRunStage secondMathematics = stage(runId, cycleId, mathematicsId, 3, 40, 0);

        StudyCreditDistributor.Distribution result = distributor.distribute(
                List.of(secondMathematics, portuguese, firstMathematics),
                mathematicsId,
                70);

        assertThat(result.allocations())
                .containsExactly(
                        new StudyCreditDistributor.Allocation(firstMathematics.id(), 30),
                        new StudyCreditDistributor.Allocation(secondMathematics.id(), 40));
        assertThat(result.unallocatedSeconds()).isZero();
    }

    @Test
    void leavesCreditBeyondTheLastEligibleStageUnallocated() {
        UUID runId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        UUID mathematicsId = UUID.randomUUID();
        StudyCycleRunStage mathematics = stage(runId, cycleId, mathematicsId, 1, 40, 10);

        StudyCreditDistributor.Distribution result = distributor.distribute(
                List.of(mathematics),
                mathematicsId,
                50);

        assertThat(result.allocations())
                .containsExactly(new StudyCreditDistributor.Allocation(mathematics.id(), 30));
        assertThat(result.unallocatedSeconds()).isEqualTo(20);
    }

    @Test
    void skipsAlreadyCompletedStages() {
        UUID runId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        UUID mathematicsId = UUID.randomUUID();
        StudyCycleRunStage completed = stage(runId, cycleId, mathematicsId, 1, 30, 30);
        StudyCycleRunStage incomplete = stage(runId, cycleId, mathematicsId, 2, 30, 0);

        StudyCreditDistributor.Distribution result = distributor.distribute(
                List.of(completed, incomplete),
                mathematicsId,
                15);

        assertThat(result.allocations())
                .containsExactly(new StudyCreditDistributor.Allocation(incomplete.id(), 15));
    }

    private StudyCycleRunStage stage(
            UUID runId,
            UUID cycleId,
            UUID subjectId,
            int position,
            long targetSeconds,
            long creditedSeconds) {
        return new StudyCycleRunStage(
                UUID.randomUUID(),
                runId,
                cycleId,
                UUID.randomUUID(),
                subjectId,
                "Matéria",
                position,
                targetSeconds,
                creditedSeconds);
    }
}
