package br.com.estudalivre.studysession.service;

import br.com.estudalivre.studysession.dto.StartStudySessionRequest;
import br.com.estudalivre.studysession.dto.StudySessionOrigin;
import br.com.estudalivre.studysession.dto.StudySessionResponse;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import br.com.estudalivre.studycycle.dto.StudyCycleResponse;
import br.com.estudalivre.studycycle.dto.StudyCycleStageResponse;
import br.com.estudalivre.studycycle.service.StudyCycleService;
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
    private final SubjectService subjectService;
    private final ContentService contentService;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            StudyCycleService studyCycleService,
            SubjectService subjectService,
            ContentService contentService) {
        this.studySessionRepository = studySessionRepository;
        this.studyCycleService = studyCycleService;
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
                .map(StudySessionResponse::from);
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

    private SessionContext cycleContext(UUID ownerId, StartStudySessionRequest request) {
        if (request.cycleId() == null || request.subjectId() != null) {
            throw new IllegalArgumentException("Sessões do ciclo exigem apenas o ciclo e um conteúdo opcional.");
        }
        StudyCycleResponse cycle = studyCycleService.get(ownerId, request.cycleId());
        if (!cycle.status().equals("ACTIVE") || cycle.currentRun() == null) {
            throw new StudyCycleIsNotActiveException();
        }
        StudyCycleStageResponse stage = cycle.stages().stream()
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
                .map(StudySessionResponse::from)
                .orElseThrow(StudySessionNotFoundException::new);
    }

    private record SessionContext(
            UUID subjectId,
            UUID cycleId,
            UUID cycleRunId,
            UUID cycleStageId) {
    }
}
