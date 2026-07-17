package br.com.estudalivre.studycycle.service;

import br.com.estudalivre.studycycle.model.StudyCycleRunStage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class StudyCreditDistributor {

    public Distribution distribute(List<StudyCycleRunStage> stages, UUID subjectId, long effectiveSeconds) {
        long remaining = effectiveSeconds;
        List<Allocation> allocations = new ArrayList<>();

        List<StudyCycleRunStage> eligibleStages = stages.stream()
                .filter(stage -> stage.subjectId().equals(subjectId))
                .filter(stage -> stage.remainingSeconds() > 0)
                .sorted(Comparator.comparingInt(StudyCycleRunStage::position))
                .toList();

        for (StudyCycleRunStage stage : eligibleStages) {
            if (remaining == 0) {
                break;
            }
            long creditedSeconds = Math.min(remaining, stage.remainingSeconds());
            allocations.add(new Allocation(stage.id(), creditedSeconds));
            remaining -= creditedSeconds;
        }

        return new Distribution(List.copyOf(allocations), remaining);
    }

    public record Allocation(UUID runStageId, long creditedSeconds) {
    }

    public record Distribution(List<Allocation> allocations, long unallocatedSeconds) {
    }
}
