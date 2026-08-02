package br.com.estudalivre.review.service;

import br.com.estudalivre.review.dto.ReviewOccurrenceResponse;
import br.com.estudalivre.review.dto.ReviewPlanResponse;
import br.com.estudalivre.review.dto.ReviewPlanSummaryResponse;
import br.com.estudalivre.review.dto.UpdateReviewScheduleRequest;
import br.com.estudalivre.review.model.ReviewSchedule;
import br.com.estudalivre.review.repository.ReviewRepository;
import br.com.estudalivre.studysession.repository.StudySessionRepository;
import br.com.estudalivre.studysession.service.OpenStudySessionAlreadyExistsException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.HashSet;
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

    @Transactional(readOnly = true)
    public ReviewPlanResponse getPlan(UUID ownerId, UUID planId) {
        var rows = reviewRepository.findPlanDetails(ownerId, planId);
        if (rows.isEmpty()) {
            throw new ReviewPlanNotFoundException();
        }
        return ReviewPlanResponse.from(rows);
    }

    @Transactional(readOnly = true)
    public List<ReviewPlanSummaryResponse> listPlans(UUID ownerId) {
        return reviewRepository.findPlanSummaries(ownerId);
    }

    @Transactional
    public ReviewPlanResponse updateSchedule(
            UUID ownerId,
            String timeZone,
            UUID planId,
            UpdateReviewScheduleRequest request) {
        var current = getPlan(ownerId, planId);
        if (!current.status().equals("ACTIVE")
                || current.version() != request.expectedVersion()) {
            throw new ReviewPlanConflictException();
        }
        var occurrenceIds = new HashSet<UUID>();
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        for (var occurrence : request.occurrences()) {
            if (!occurrenceIds.add(occurrence.occurrenceId())
                    || !occurrence.dueDate().isAfter(today)) {
                throw new ReviewPlanConflictException();
            }
        }
        if (reviewRepository.advancePlanVersion(
                ownerId, planId, request.expectedVersion(), "ACTIVE") != 1) {
            throw new ReviewPlanConflictException();
        }
        for (var occurrence : request.occurrences()) {
            if (reviewRepository.rescheduleOccurrence(
                    planId, occurrence.occurrenceId(), occurrence.dueDate(), today) != 1) {
                throw new ReviewPlanConflictException();
            }
        }
        return getPlan(ownerId, planId);
    }

    @Transactional
    public ReviewPlanResponse cancelPlan(UUID ownerId, UUID planId, int expectedVersion) {
        var current = getPlan(ownerId, planId);
        if (!current.status().equals("ACTIVE") || current.version() != expectedVersion
                || reviewRepository.changePlanStatus(
                        ownerId, planId, expectedVersion, "ACTIVE", "CANCELED") != 1) {
            throw new ReviewPlanConflictException();
        }
        reviewRepository.cancelPendingOccurrences(planId);
        return getPlan(ownerId, planId);
    }

    @Transactional
    public ReviewPlanResponse reactivatePlan(UUID ownerId, UUID planId, int expectedVersion) {
        var current = getPlan(ownerId, planId);
        if (!current.status().equals("CANCELED") || current.version() != expectedVersion) {
            throw new ReviewPlanConflictException();
        }
        try {
            if (reviewRepository.changePlanStatus(
                    ownerId, planId, expectedVersion, "CANCELED", "ACTIVE") != 1) {
                throw new ReviewPlanConflictException();
            }
        } catch (DuplicateKeyException exception) {
            throw new ReviewPlanConflictException();
        }
        reviewRepository.restoreCanceledOccurrences(planId);
        return getPlan(ownerId, planId);
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
