package br.com.estudalivre.review.service;

import br.com.estudalivre.review.dto.ReviewOccurrenceResponse;
import br.com.estudalivre.review.model.ReviewSchedule;
import br.com.estudalivre.review.repository.ReviewRepository;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import br.com.estudalivre.studysession.service.OpenStudySessionAlreadyExistsException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final StudySessionRepository studySessionRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            StudySessionRepository studySessionRepository) {
        this.reviewRepository = reviewRepository;
        this.studySessionRepository = studySessionRepository;
    }

    @Transactional
    public void ensurePlan(
            UUID ownerId,
            UUID subjectId,
            UUID contentId,
            UUID sourceSessionId,
            LocalDate initialStudyDate) {
        reviewRepository.createPlanIfAbsent(
                UUID.randomUUID(),
                ownerId,
                subjectId,
                contentId,
                sourceSessionId,
                initialStudyDate);
        UUID planId = reviewRepository.findActivePlanId(ownerId, contentId)
                .orElseThrow(() -> new IllegalStateException("O plano de revisão ativo não foi encontrado."));
        ReviewSchedule.from(initialStudyDate).forEach(schedule -> reviewRepository.createOccurrenceIfAbsent(
                UUID.randomUUID(),
                planId,
                schedule.intervalDays(),
                schedule.dueDate()));
    }

    @Transactional(readOnly = true)
    public List<ReviewOccurrenceResponse> list(UUID ownerId, String timeZone) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        return reviewRepository.findScheduledByOwnerId(ownerId).stream()
                .map(item -> ReviewOccurrenceResponse.from(item, today))
                .toList();
    }

    @Transactional
    public UUID start(UUID ownerId, String timeZone, UUID requestedOccurrenceId) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        var context = reviewRepository.findLatestDueForUpdate(
                        requestedOccurrenceId,
                        ownerId,
                        today)
                .orElseThrow(ReviewOccurrenceNotAvailableException::new);
        UUID sessionId = UUID.randomUUID();
        try {
            studySessionRepository.createReview(
                    sessionId,
                    ownerId,
                    context.subjectId(),
                    context.contentId(),
                    context.occurrenceId());
            studySessionRepository.createTimerSegment(UUID.randomUUID(), sessionId);
        } catch (DuplicateKeyException exception) {
            throw new OpenStudySessionAlreadyExistsException();
        }
        return sessionId;
    }

    @Transactional
    public void complete(UUID ownerId, UUID occurrenceId, UUID sessionId) {
        var state = reviewRepository.findCompletionStateForUpdate(occurrenceId, ownerId)
                .orElseThrow(ReviewOccurrenceNotAvailableException::new);
        if (state.status().equals("COMPLETED") && sessionId.equals(state.completedSessionId())) {
            return;
        }
        if (!state.status().equals("SCHEDULED")
                || reviewRepository.complete(occurrenceId, sessionId) != 1) {
            throw new ReviewOccurrenceNotAvailableException();
        }
        reviewRepository.skipEarlierScheduled(state.planId(), state.dueDate());
    }
}
