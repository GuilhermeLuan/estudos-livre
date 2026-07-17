package br.com.estudalivre.studycycle.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SuggestedStudyCyclePlanner {

    private static final int MINIMUM_CYCLE_MINUTES = 10 * 60;
    private static final int MAXIMUM_CYCLE_MINUTES = 30 * 60;
    private static final int MINIMUM_SUBJECT_MINUTES = 60;
    private static final int MAXIMUM_APPEARANCE_MINUTES = 180;
    private static final int ROUNDING_MINUTES = 5;

    public SuggestedStudyCyclePlan generate(List<SuggestedStudyCycleInput> inputs) {
        if (inputs == null || inputs.isEmpty() || inputs.size() > 30) {
            throw new IllegalArgumentException("O ciclo sugerido aceita entre 1 e 30 matérias.");
        }
        if (inputs.stream().anyMatch(input -> input == null
                || input.questionCount() <= 0
                || input.weight() <= 0)) {
            throw new IllegalArgumentException("Questões e peso devem ser positivos.");
        }
        int totalMinutes = Math.clamp(inputs.size() * 2 * 60, MINIMUM_CYCLE_MINUTES, MAXIMUM_CYCLE_MINUTES);
        int extraUnits = (totalMinutes - inputs.size() * MINIMUM_SUBJECT_MINUTES) / ROUNDING_MINUTES;
        List<Allocation> allocations = inputs.stream()
                .map(input -> new Allocation(input, priority(input)))
                .toList();
        BigDecimal totalPriority = allocations.stream()
                .map(Allocation::priority)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int assignedExtraUnits = 0;
        for (Allocation allocation : allocations) {
            BigDecimal exactUnits = allocation.priority()
                    .multiply(BigDecimal.valueOf(extraUnits))
                    .divide(totalPriority, 12, RoundingMode.DOWN);
            allocation.extraUnits = exactUnits.setScale(0, RoundingMode.DOWN).intValueExact();
            allocation.remainder = exactUnits.subtract(BigDecimal.valueOf(allocation.extraUnits));
            assignedExtraUnits += allocation.extraUnits;
        }

        List<Allocation> byLargestRemainder = new ArrayList<>(allocations);
        byLargestRemainder.sort(Comparator.comparing(Allocation::remainder).reversed()
                .thenComparingInt(allocation -> inputs.indexOf(allocation.input())));
        for (int index = 0; index < extraUnits - assignedExtraUnits; index++) {
            byLargestRemainder.get(index).extraUnits++;
        }

        List<SuggestedStudyCycleSubject> subjects = new ArrayList<>();
        List<AppearanceQueue> appearanceQueues = new ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            Allocation allocation = allocations.get(index);
            int allocatedMinutes = MINIMUM_SUBJECT_MINUTES + allocation.extraUnits * ROUNDING_MINUTES;
            List<Integer> appearances = splitAppearances(allocatedMinutes);
            subjects.add(new SuggestedStudyCycleSubject(
                    allocation.input().subjectId(),
                    allocation.input().subjectName(),
                    allocation.input().questionCount(),
                    allocation.input().weight(),
                    allocation.input().difficulty(),
                    allocation.priority(),
                    allocatedMinutes,
                    appearances.size()));
            appearanceQueues.add(new AppearanceQueue(
                    index, allocation.input(), new ArrayDeque<>(appearances), allocatedMinutes));
        }
        return new SuggestedStudyCyclePlan(totalMinutes, List.copyOf(subjects), orderAppearances(appearanceQueues));
    }

    private List<SuggestedStudyCycleStage> orderAppearances(List<AppearanceQueue> queues) {
        List<SuggestedStudyCycleStage> stages = new ArrayList<>();
        SuggestedStudyCycleInput previous = null;
        while (queues.stream().anyMatch(AppearanceQueue::hasAppearances)) {
            SuggestedStudyCycleInput previousSubject = previous;
            long availableSubjects = queues.stream().filter(AppearanceQueue::hasAppearances).count();
            AppearanceQueue selected = queues.stream()
                    .filter(AppearanceQueue::hasAppearances)
                    .filter(queue -> previousSubject == null
                            || availableSubjects == 1
                            || !queue.input().subjectId().equals(previousSubject.subjectId()))
                    .min(Comparator.comparingInt(AppearanceQueue::remainingMinutes).reversed()
                            .thenComparingInt(AppearanceQueue::inputIndex))
                    .orElseThrow();
            int minutes = selected.removeFirst();
            stages.add(new SuggestedStudyCycleStage(
                    selected.input().subjectId(), selected.input().subjectName(), minutes));
            previous = selected.input();
        }
        return List.copyOf(stages);
    }

    private List<Integer> splitAppearances(int allocatedMinutes) {
        int appearanceCount = Math.ceilDiv(allocatedMinutes, MAXIMUM_APPEARANCE_MINUTES);
        int totalUnits = allocatedMinutes / ROUNDING_MINUTES;
        int baseUnits = totalUnits / appearanceCount;
        int remainderUnits = totalUnits % appearanceCount;
        List<Integer> appearances = new ArrayList<>();
        for (int index = 0; index < appearanceCount; index++) {
            appearances.add((baseUnits + (index < remainderUnits ? 1 : 0)) * ROUNDING_MINUTES);
        }
        return appearances;
    }

    private BigDecimal priority(SuggestedStudyCycleInput input) {
        return BigDecimal.valueOf(input.questionCount())
                .multiply(BigDecimal.valueOf(input.weight()))
                .multiply(input.difficulty().factor())
                .setScale(2);
    }

    private static final class Allocation {
        private final SuggestedStudyCycleInput input;
        private final BigDecimal priority;
        private int extraUnits;
        private BigDecimal remainder;

        private Allocation(SuggestedStudyCycleInput input, BigDecimal priority) {
            this.input = input;
            this.priority = priority;
        }

        private SuggestedStudyCycleInput input() {
            return input;
        }

        private BigDecimal priority() {
            return priority;
        }

        private BigDecimal remainder() {
            return remainder;
        }
    }

    private static final class AppearanceQueue {
        private final int inputIndex;
        private final SuggestedStudyCycleInput input;
        private final ArrayDeque<Integer> appearances;
        private int remainingMinutes;

        private AppearanceQueue(
                int inputIndex,
                SuggestedStudyCycleInput input,
                ArrayDeque<Integer> appearances,
                int remainingMinutes) {
            this.inputIndex = inputIndex;
            this.input = input;
            this.appearances = appearances;
            this.remainingMinutes = remainingMinutes;
        }

        private boolean hasAppearances() {
            return !appearances.isEmpty();
        }

        private int removeFirst() {
            int minutes = appearances.removeFirst();
            remainingMinutes -= minutes;
            return minutes;
        }

        private int inputIndex() {
            return inputIndex;
        }

        private SuggestedStudyCycleInput input() {
            return input;
        }

        private int remainingMinutes() {
            return remainingMinutes;
        }
    }
}
