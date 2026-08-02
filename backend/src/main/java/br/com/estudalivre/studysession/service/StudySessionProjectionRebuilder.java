package br.com.estudalivre.studysession.service;

import br.com.estudalivre.studycycle.repository.StudyCycleRepository;
import br.com.estudalivre.studycycle.service.StudyCreditDistributor;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudySessionProjectionRebuilder {

    private final StudySessionRepository sessionRepository;
    private final StudyCycleRepository cycleRepository;
    private final StudyCreditDistributor distributor;

    public StudySessionProjectionRebuilder(
            StudySessionRepository sessionRepository,
            StudyCycleRepository cycleRepository,
            StudyCreditDistributor distributor) {
        this.sessionRepository = sessionRepository;
        this.cycleRepository = cycleRepository;
        this.distributor = distributor;
    }

    public void rebuild(UUID ownerId, UUID cycleId, UUID affectedRunId) {
        var cycle = cycleRepository.findByIdAndOwnerId(cycleId, ownerId)
                .orElseThrow(StudySessionNotFoundException::new);
        cycleRepository.lockProjection(cycleId);
        List<StudyCycleRepository.ProjectionRun> allRuns =
                cycleRepository.findProjectionRunsForUpdate(cycleId);
        int affectedIndex = 0;
        for (int index = 0; index < allRuns.size(); index++) {
            if (allRuns.get(index).id().equals(affectedRunId)) {
                affectedIndex = index;
                break;
            }
        }
        int segmentStart = 0;
        for (int index = 0; index < affectedIndex; index++) {
            if (allRuns.get(index).explicitlyAbandoned()) {
                segmentStart = index + 1;
            }
        }
        int segmentEnd = allRuns.size();
        boolean explicitlyClosed = false;
        for (int index = affectedIndex; index < allRuns.size(); index++) {
            if (allRuns.get(index).explicitlyAbandoned()) {
                segmentEnd = index + 1;
                explicitlyClosed = true;
                break;
            }
        }
        List<StudyCycleRepository.ProjectionRun> runs =
                new ArrayList<>(allRuns.subList(segmentStart, segmentEnd));
        if (runs.isEmpty()) {
            return;
        }
        List<UUID> segmentRunIds = runs.stream().map(StudyCycleRepository.ProjectionRun::id).toList();
        for (var run : runs) {
            sessionRepository.deleteCreditsForRun(run.id());
            cycleRepository.resetRunProjection(run.id());
        }

        int runIndex = 0;
        for (var session : sessionRepository.findProjectionSessions(segmentRunIds)) {
            if (runIndex >= runs.size()) {
                if (explicitlyClosed) {
                    runIndex = runs.size() - 1;
                } else {
                    var newRun = createRun(cycleId);
                    runs.add(newRun);
                }
            }
            var run = runs.get(runIndex);
            sessionRepository.moveProjectionSession(session.sessionId(), cycleId, run.id());
            var stages = cycleRepository.findRunStagesForUpdate(run.id());
            var distribution = distributor.distribute(stages, session.subjectId(), session.effectiveSeconds());
            for (var allocation : distribution.allocations()) {
                if (cycleRepository.creditRunStage(
                        allocation.runStageId(), allocation.creditedSeconds()) != 1) {
                    throw new StudySessionConflictException();
                }
                sessionRepository.createCredit(
                        session.sessionId(), allocation.runStageId(), allocation.creditedSeconds());
            }
            if (cycleRepository.isRunFinished(run.id())) {
                if (explicitlyClosed && runIndex == runs.size() - 1) {
                    cycleRepository.markRunExplicitlyAbandoned(run.id());
                } else {
                    cycleRepository.markRunCompleted(run.id());
                    runIndex++;
                }
            }
        }

        if (explicitlyClosed) {
            cycleRepository.markRunExplicitlyAbandoned(runs.getLast().id());
            return;
        }
        if (runIndex >= runs.size()) {
            var newRun = createRun(cycleId);
                runs.add(newRun);
        }
        cycleRepository.markRunOpen(
                runs.get(runIndex).id(), cycle.status().equals("ACTIVE") ? "IN_PROGRESS" : "PAUSED");
    }

    private StudyCycleRepository.ProjectionRun createRun(UUID cycleId) {
        UUID runId = UUID.randomUUID();
        cycleRepository.createRun(runId, cycleId);
        cycleRepository.snapshotRunStages(runId, cycleId);
        return new StudyCycleRepository.ProjectionRun(runId, cycleId, 0, "IN_PROGRESS", false);
    }
}
