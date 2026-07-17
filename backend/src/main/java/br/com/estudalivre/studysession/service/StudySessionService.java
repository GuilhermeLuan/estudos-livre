package br.com.estudalivre.studysession.service;

import br.com.estudalivre.studysession.dto.FinishStudySessionRequest;
import br.com.estudalivre.studysession.dto.CreateManualStudySessionRequest;
import br.com.estudalivre.studysession.dto.StartStudySessionRequest;
import br.com.estudalivre.studysession.dto.StudySessionOrigin;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
import br.com.estudalivre.studysession.dto.UpdateExerciseResultRequest;
import br.com.estudalivre.studysession.dto.ExerciseSummaryResponse;
import br.com.estudalivre.studysession.model.ExerciseResult;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import br.com.estudalivre.studycycle.dto.StudyCycleResponse;
import br.com.estudalivre.studycycle.dto.StudyCycleStageResponse;
import br.com.estudalivre.studycycle.service.StudyCycleService;
import br.com.estudalivre.studycycle.service.StudyCreditDistributor;
import br.com.estudalivre.studycycle.repository.StudyCycleRepository;
import br.com.estudalivre.subject.service.ContentService;
import br.com.estudalivre.subject.service.SubjectService;
import java.util.Optional;
import java.util.UUID;
import java.time.ZoneId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final StudyCycleService studyCycleService;
    private final StudyCycleRepository studyCycleRepository;
    private final StudyCreditDistributor studyCreditDistributor;
    private final SubjectService subjectService;
    private final ContentService contentService;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            StudyCycleService studyCycleService,
            StudyCycleRepository studyCycleRepository,
            StudyCreditDistributor studyCreditDistributor,
            SubjectService subjectService,
            ContentService contentService) {
        this.studySessionRepository = studySessionRepository;
        this.studyCycleService = studyCycleService;
        this.studyCycleRepository = studyCycleRepository;
        this.studyCreditDistributor = studyCreditDistributor;
        this.subjectService = subjectService;
        this.contentService = contentService;
    }

    @Transactional
    public StudySessionResponse start(UUID ownerId, StartStudySessionRequest request) {
        SessionContext context = request.origin() == StudySessionOrigin.CYCLE
                ? cycleContext(ownerId, request)
                : freeContext(ownerId, request);
        validateOptionalContent(ownerId, context.subjectId(), request.contentId());

        UUID sessionId = UUID.randomUUID();
        try {
            studySessionRepository.create(
                    sessionId,
                    ownerId,
                    request.origin().name(),
                    context.subjectId(),
                    request.contentId(),
                    context.cycleId(),
                    context.cycleRunId(),
                    context.cycleStageId());
            studySessionRepository.createTimerSegment(UUID.randomUUID(), sessionId);
        } catch (DuplicateKeyException exception) {
            throw new OpenStudySessionAlreadyExistsException();
        }
        return findOwned(sessionId, ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<StudySessionResponse> current(UUID ownerId) {
        return studySessionRepository.findCurrentByOwnerId(ownerId)
                .map(session -> StudySessionResponse.from(session, studySessionRepository.findCredits(session.id())));
    }

    @Transactional
    public StudySessionResponse createManual(
            UUID ownerId,
            String timeZone,
            CreateManualStudySessionRequest request) {
        subjectService.getActive(ownerId, request.subjectId());
        validateOptionalContent(ownerId, request.subjectId(), request.contentId());
        var startedAt = request.startedAtLocal().atZone(ZoneId.of(timeZone)).toOffsetDateTime();
        UUID sessionId = UUID.randomUUID();
        studySessionRepository.createManual(
                sessionId,
                ownerId,
                request.subjectId(),
                request.contentId(),
                startedAt,
                startedAt.plusSeconds(request.effectiveSeconds()),
                request.effectiveSeconds(),
                request.notes());
        studyCycleRepository.findActiveByOwnerId(ownerId)
                .flatMap(cycle -> studyCycleRepository.findCurrentRun(cycle.id()))
                .ifPresent(run -> applyCreditsToRun(
                        sessionId, run.id(), request.subjectId(), request.effectiveSeconds()));
        return findOwned(sessionId, ownerId);
    }

    @Transactional(readOnly = true)
    public java.util.List<StudySessionResponse> history(UUID ownerId) {
        return studySessionRepository.findHistoryByOwnerId(ownerId).stream()
                .map(session -> StudySessionResponse.from(
                        session, studySessionRepository.findCredits(session.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseSummaryResponse exerciseSummary(UUID ownerId) {
        return ExerciseSummaryResponse.from(
                studySessionRepository.findSubjectExerciseSummary(ownerId),
                studySessionRepository.findContentExerciseSummary(ownerId));
    }

    @Transactional
    public StudySessionResponse pause(UUID ownerId, UUID sessionId) {
        String status = lockOwnedStatus(sessionId, ownerId);
        if (!status.equals("ACTIVE")) {
            throw new InvalidStudySessionTransitionException("Somente uma sessão ativa pode ser pausada.");
        }
        if (studySessionRepository.closeCurrentTimerSegment(sessionId) != 1
                || studySessionRepository.pause(sessionId, ownerId) != 1) {
            throw new InvalidStudySessionTransitionException("A sessão não pôde ser pausada no estado atual.");
        }
        return findOwned(sessionId, ownerId);
    }

    @Transactional
    public StudySessionResponse resume(UUID ownerId, UUID sessionId) {
        String status = lockOwnedStatus(sessionId, ownerId);
        if (!status.equals("PAUSED")) {
            throw new InvalidStudySessionTransitionException("Somente uma sessão pausada pode ser retomada.");
        }
        if (studySessionRepository.resume(sessionId, ownerId) != 1) {
            throw new InvalidStudySessionTransitionException("A sessão não pôde ser retomada no estado atual.");
        }
        studySessionRepository.createTimerSegment(UUID.randomUUID(), sessionId);
        return findOwned(sessionId, ownerId);
    }

    @Transactional
    public StudySessionResponse finish(UUID ownerId, UUID sessionId, FinishStudySessionRequest request) {
        Optional<ExerciseResult> exerciseResult = ExerciseResult.optional(
                request.questionsAttempted(), request.questionsCorrect());
        var state = studySessionRepository.findFinishStateForUpdate(sessionId, ownerId)
                .orElseThrow(StudySessionNotFoundException::new);
        if (state.status().equals("FINISHED")) {
            if (state.effectiveSeconds().equals(request.effectiveSeconds())) {
                return findOwned(sessionId, ownerId);
            }
            throw new StudySessionFinishConflictException();
        }
        if (state.version() != request.expectedVersion()) {
            throw new StudySessionFinishConflictException();
        }
        if (state.status().equals("ACTIVE") && studySessionRepository.closeCurrentTimerSegment(sessionId) != 1) {
            throw new InvalidStudySessionTransitionException("O cronômetro ativo não pôde ser encerrado.");
        }
        if (studySessionRepository.finish(
                sessionId, ownerId, request.effectiveSeconds(), request.expectedVersion()) != 1) {
            throw new StudySessionFinishConflictException();
        }
        if (state.cycleRunId() != null) {
            applyCreditsToRun(
                    sessionId, state.cycleRunId(), state.subjectId(), request.effectiveSeconds());
        }
        exerciseResult.ifPresent(result -> studySessionRepository.saveExerciseResult(sessionId, result));
        return findOwned(sessionId, ownerId);
    }

    @Transactional
    public StudySessionResponse updateExerciseResult(
            UUID ownerId,
            UUID sessionId,
            UpdateExerciseResultRequest request) {
        Optional<ExerciseResult> exerciseResult = ExerciseResult.optional(
                request.questionsAttempted(), request.questionsCorrect());
        String status = lockOwnedStatus(sessionId, ownerId);
        if (!status.equals("FINISHED")) {
            throw new InvalidStudySessionTransitionException(
                    "Somente uma sessão finalizada pode ter seus exercícios editados.");
        }
        if (exerciseResult.isPresent()) {
            studySessionRepository.saveExerciseResult(sessionId, exerciseResult.orElseThrow());
        } else {
            studySessionRepository.deleteExerciseResult(sessionId);
        }
        return findOwned(sessionId, ownerId);
    }

    private void applyCreditsToRun(
            UUID sessionId,
            UUID runId,
            UUID subjectId,
            long effectiveSeconds) {
        var runStages = studyCycleRepository.findRunStagesForUpdate(runId);
        var distribution = studyCreditDistributor.distribute(runStages, subjectId, effectiveSeconds);
        distribution.allocations().forEach(allocation -> {
            if (studyCycleRepository.creditRunStage(
                    allocation.runStageId(), allocation.creditedSeconds()) != 1) {
                throw new StudySessionFinishConflictException();
            }
            studySessionRepository.createCredit(
                    sessionId, allocation.runStageId(), allocation.creditedSeconds());
        });
        if (!runStages.isEmpty() && studyCycleRepository.completeRunIfFinished(runId) == 1) {
            UUID nextRunId = UUID.randomUUID();
            UUID cycleId = runStages.getFirst().cycleId();
            studyCycleRepository.createRun(nextRunId, cycleId);
            studyCycleRepository.snapshotRunStages(nextRunId, cycleId);
        }
    }

    private SessionContext cycleContext(UUID ownerId, StartStudySessionRequest request) {
        if (request.cycleId() == null || request.subjectId() != null) {
            throw new IllegalArgumentException("Sessões do ciclo exigem apenas o ciclo e um conteúdo opcional.");
        }
        StudyCycleResponse cycle = studyCycleService.get(ownerId, request.cycleId());
        if (!cycle.status().equals("ACTIVE") || cycle.currentRun() == null) {
            throw new StudyCycleIsNotActiveException();
        }
        StudyCycleStageResponse stage = cycle.stages().stream()
                .filter(candidate -> candidate.position() == cycle.currentRun().currentStagePosition())
                .findFirst()
                .orElseThrow(StudyCycleIsNotActiveException::new);
        return new SessionContext(
                stage.subjectId(),
                cycle.id(),
                cycle.currentRun().id(),
                stage.id());
    }

    private SessionContext freeContext(UUID ownerId, StartStudySessionRequest request) {
        if (request.subjectId() == null || request.cycleId() != null) {
            throw new IllegalArgumentException("Sessões livres exigem uma matéria e um conteúdo opcional.");
        }
        subjectService.getActive(ownerId, request.subjectId());
        return new SessionContext(request.subjectId(), null, null, null);
    }

    private void validateOptionalContent(UUID ownerId, UUID subjectId, UUID contentId) {
        if (contentId != null) {
            contentService.getActive(ownerId, subjectId, contentId);
        }
    }

    private String lockOwnedStatus(UUID sessionId, UUID ownerId) {
        return studySessionRepository.findStatusForUpdate(sessionId, ownerId)
                .orElseThrow(StudySessionNotFoundException::new);
    }

    private StudySessionResponse findOwned(UUID sessionId, UUID ownerId) {
        return studySessionRepository.findByIdAndOwnerId(sessionId, ownerId)
                .map(session -> StudySessionResponse.from(session, studySessionRepository.findCredits(session.id())))
                .orElseThrow(StudySessionNotFoundException::new);
    }

    private record SessionContext(
            UUID subjectId,
            UUID cycleId,
            UUID cycleRunId,
            UUID cycleStageId) {
    }
}
