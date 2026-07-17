package br.com.estudalivre.studysession.service;

import br.com.estudalivre.studysession.dto.FinishStudySessionRequest;
import br.com.estudalivre.studysession.dto.StartStudySessionRequest;
import br.com.estudalivre.studysession.dto.StudySessionOrigin;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
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
            var runStages = studyCycleRepository.findRunStagesForUpdate(state.cycleRunId());
            var distribution = studyCreditDistributor.distribute(
                    runStages, state.subjectId(), request.effectiveSeconds());
            distribution.allocations().forEach(allocation -> {
                if (studyCycleRepository.creditRunStage(
                        allocation.runStageId(), allocation.creditedSeconds()) != 1) {
                    throw new StudySessionFinishConflictException();
                }
                studySessionRepository.createCredit(
                        sessionId, allocation.runStageId(), allocation.creditedSeconds());
            });
            if (!runStages.isEmpty()
                    && studyCycleRepository.completeRunIfFinished(state.cycleRunId()) == 1) {
                UUID nextRunId = UUID.randomUUID();
                UUID cycleId = runStages.getFirst().cycleId();
                studyCycleRepository.createRun(nextRunId, cycleId);
                studyCycleRepository.snapshotRunStages(nextRunId, cycleId);
            }
        }
        return findOwned(sessionId, ownerId);
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
