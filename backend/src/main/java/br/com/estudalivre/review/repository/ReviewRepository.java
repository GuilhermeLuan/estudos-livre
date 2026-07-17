package br.com.estudalivre.review.repository;

import br.com.estudalivre.review.model.ReviewExecutionContext;
import br.com.estudalivre.review.model.ReviewQueueItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {

    private final JdbcClient jdbcClient;

    public ReviewRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void createPlanIfAbsent(
            UUID id,
            UUID ownerId,
            UUID subjectId,
            UUID contentId,
            UUID sourceSessionId,
            LocalDate initialStudyDate) {
        jdbcClient.sql("""
                        INSERT INTO review_plan (
                            id, owner_id, subject_id, content_id, source_session_id, initial_study_date
                        ) VALUES (
                            :id, :ownerId, :subjectId, :contentId, :sourceSessionId, :initialStudyDate
                        )
                        ON CONFLICT DO NOTHING
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("subjectId", subjectId)
                .param("contentId", contentId)
                .param("sourceSessionId", sourceSessionId)
                .param("initialStudyDate", initialStudyDate)
                .update();
    }

    public Optional<UUID> findActivePlanId(UUID ownerId, UUID contentId) {
        return jdbcClient.sql("""
                        SELECT id
                        FROM review_plan
                        WHERE owner_id = :ownerId
                          AND content_id = :contentId
                          AND status = 'ACTIVE'
                        """)
                .param("ownerId", ownerId)
                .param("contentId", contentId)
                .query(UUID.class)
                .optional();
    }

    public void createOccurrenceIfAbsent(
            UUID id,
            UUID planId,
            int intervalDays,
            LocalDate dueDate) {
        jdbcClient.sql("""
                        INSERT INTO review_occurrence (id, plan_id, interval_days, due_date)
                        VALUES (:id, :planId, :intervalDays, :dueDate)
                        ON CONFLICT DO NOTHING
                        """)
                .param("id", id)
                .param("planId", planId)
                .param("intervalDays", intervalDays)
                .param("dueDate", dueDate)
                .update();
    }

    public List<ReviewQueueItem> findScheduledByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT occurrence.id AS occurrence_id,
                               plan.id AS plan_id,
                               plan.subject_id,
                               subject.name AS subject_name,
                               plan.content_id,
                               content.name AS content_name,
                               plan.initial_study_date,
                               occurrence.interval_days,
                               occurrence.due_date
                        FROM review_occurrence occurrence
                        JOIN review_plan plan ON plan.id = occurrence.plan_id
                        JOIN subject ON subject.id = plan.subject_id
                        JOIN content ON content.id = plan.content_id
                        WHERE plan.owner_id = :ownerId
                          AND plan.status = 'ACTIVE'
                          AND occurrence.status = 'SCHEDULED'
                        ORDER BY occurrence.due_date, occurrence.interval_days, occurrence.id
                        """)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new ReviewQueueItem(
                        resultSet.getObject("occurrence_id", UUID.class),
                        resultSet.getObject("plan_id", UUID.class),
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_name"),
                        resultSet.getObject("content_id", UUID.class),
                        resultSet.getString("content_name"),
                        resultSet.getObject("initial_study_date", LocalDate.class),
                        resultSet.getInt("interval_days"),
                        resultSet.getObject("due_date", LocalDate.class)))
                .list();
    }

    public Optional<ReviewExecutionContext> findLatestDueForUpdate(
            UUID requestedOccurrenceId,
            UUID ownerId,
            LocalDate today) {
        return jdbcClient.sql("""
                        WITH requested AS (
                            SELECT occurrence.plan_id
                            FROM review_occurrence occurrence
                            JOIN review_plan plan ON plan.id = occurrence.plan_id
                            WHERE occurrence.id = :requestedOccurrenceId
                              AND plan.owner_id = :ownerId
                              AND plan.status = 'ACTIVE'
                              AND occurrence.status = 'SCHEDULED'
                              AND occurrence.due_date <= :today
                        )
                        SELECT occurrence.id AS occurrence_id,
                               occurrence.plan_id,
                               plan.subject_id,
                               plan.content_id,
                               occurrence.due_date
                        FROM review_occurrence occurrence
                        JOIN review_plan plan ON plan.id = occurrence.plan_id
                        WHERE occurrence.plan_id = (SELECT plan_id FROM requested)
                          AND occurrence.status = 'SCHEDULED'
                          AND occurrence.due_date <= :today
                        ORDER BY occurrence.due_date DESC, occurrence.interval_days DESC
                        LIMIT 1
                        FOR UPDATE OF occurrence
                        """)
                .param("requestedOccurrenceId", requestedOccurrenceId)
                .param("ownerId", ownerId)
                .param("today", today)
                .query((resultSet, rowNumber) -> new ReviewExecutionContext(
                        resultSet.getObject("occurrence_id", UUID.class),
                        resultSet.getObject("plan_id", UUID.class),
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getObject("content_id", UUID.class),
                        resultSet.getObject("due_date", LocalDate.class)))
                .optional();
    }

    public Optional<ReviewCompletionState> findCompletionStateForUpdate(
            UUID occurrenceId,
            UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT occurrence.plan_id,
                               occurrence.due_date,
                               occurrence.status,
                               occurrence.completed_session_id
                        FROM review_occurrence occurrence
                        JOIN review_plan plan ON plan.id = occurrence.plan_id
                        WHERE occurrence.id = :occurrenceId
                          AND plan.owner_id = :ownerId
                        FOR UPDATE OF occurrence
                        """)
                .param("occurrenceId", occurrenceId)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new ReviewCompletionState(
                        resultSet.getObject("plan_id", UUID.class),
                        resultSet.getObject("due_date", LocalDate.class),
                        resultSet.getString("status"),
                        resultSet.getObject("completed_session_id", UUID.class)))
                .optional();
    }

    public int complete(UUID occurrenceId, UUID sessionId) {
        return jdbcClient.sql("""
                        UPDATE review_occurrence
                        SET status = 'COMPLETED',
                            resolved_at = CURRENT_TIMESTAMP,
                            completed_session_id = :sessionId,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :occurrenceId AND status = 'SCHEDULED'
                        """)
                .param("occurrenceId", occurrenceId)
                .param("sessionId", sessionId)
                .update();
    }

    public void skipEarlierScheduled(UUID planId, LocalDate completedDueDate) {
        jdbcClient.sql("""
                        UPDATE review_occurrence
                        SET status = 'SKIPPED',
                            resolved_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE plan_id = :planId
                          AND status = 'SCHEDULED'
                          AND due_date < :completedDueDate
                        """)
                .param("planId", planId)
                .param("completedDueDate", completedDueDate)
                .update();
    }

    public record ReviewCompletionState(
            UUID planId,
            LocalDate dueDate,
            String status,
            UUID completedSessionId) {
    }
}
