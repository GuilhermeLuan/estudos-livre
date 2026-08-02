package br.com.estudalivre.review.repository;

import br.com.estudalivre.review.model.ReviewExecutionContext;
import br.com.estudalivre.review.model.ReviewPlanDetailRow;
import br.com.estudalivre.review.model.ReviewQueueItem;
import br.com.estudalivre.review.dto.ReviewPlanSummaryResponse;
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

    public List<ReviewPlanDetailRow> findPlanDetails(UUID ownerId, UUID planId) {
        return jdbcClient.sql("""
                        SELECT plan.id AS plan_id,
                               plan.status AS plan_status,
                               plan.version AS plan_version,
                               plan.subject_id,
                               subject.name AS subject_name,
                               plan.content_id,
                               content.name AS content_name,
                               plan.initial_study_date,
                               occurrence.id AS occurrence_id,
                               occurrence.interval_days,
                               occurrence.due_date,
                               occurrence.status AS occurrence_status,
                               occurrence.resolved_at,
                               EXISTS (
                                   SELECT 1
                                   FROM study_session session
                                   WHERE session.review_occurrence_id = occurrence.id
                                     AND session.status IN ('ACTIVE', 'PAUSED')
                               ) AS in_progress
                        FROM review_plan plan
                        JOIN subject ON subject.id = plan.subject_id
                        JOIN content ON content.id = plan.content_id
                        JOIN review_occurrence occurrence ON occurrence.plan_id = plan.id
                        WHERE plan.id = :planId
                          AND plan.owner_id = :ownerId
                        ORDER BY occurrence.interval_days, occurrence.id
                        """)
                .param("planId", planId)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new ReviewPlanDetailRow(
                        resultSet.getObject("plan_id", UUID.class),
                        resultSet.getString("plan_status"),
                        resultSet.getInt("plan_version"),
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_name"),
                        resultSet.getObject("content_id", UUID.class),
                        resultSet.getString("content_name"),
                        resultSet.getObject("initial_study_date", LocalDate.class),
                        resultSet.getObject("occurrence_id", UUID.class),
                        resultSet.getInt("interval_days"),
                        resultSet.getObject("due_date", LocalDate.class),
                        resultSet.getString("occurrence_status"),
                        resultSet.getObject("resolved_at", java.time.OffsetDateTime.class) == null
                                ? null
                                : resultSet.getObject("resolved_at", java.time.OffsetDateTime.class).toInstant(),
                        resultSet.getBoolean("in_progress")))
                .list();
    }

    public List<ReviewPlanSummaryResponse> findPlanSummaries(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT plan.id,
                               plan.status,
                               plan.version,
                               subject.name AS subject_name,
                               content.name AS content_name,
                               plan.initial_study_date,
                               COUNT(*) FILTER (WHERE occurrence.status = 'SCHEDULED') AS scheduled_count,
                               COUNT(*) FILTER (WHERE occurrence.status = 'COMPLETED') AS completed_count,
                               COUNT(*) FILTER (WHERE occurrence.status = 'SKIPPED') AS skipped_count,
                               COUNT(*) FILTER (WHERE occurrence.status = 'CANCELED') AS canceled_count
                        FROM review_plan plan
                        JOIN subject ON subject.id = plan.subject_id
                        JOIN content ON content.id = plan.content_id
                        JOIN review_occurrence occurrence ON occurrence.plan_id = plan.id
                        WHERE plan.owner_id = :ownerId
                        GROUP BY plan.id, subject.name, content.name
                        ORDER BY CASE plan.status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
                                 plan.updated_at DESC,
                                 plan.id
                        """)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new ReviewPlanSummaryResponse(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getInt("version"),
                        resultSet.getString("subject_name"),
                        resultSet.getString("content_name"),
                        resultSet.getObject("initial_study_date", LocalDate.class),
                        resultSet.getInt("scheduled_count"),
                        resultSet.getInt("completed_count"),
                        resultSet.getInt("skipped_count"),
                        resultSet.getInt("canceled_count")))
                .list();
    }

    public int advancePlanVersion(UUID ownerId, UUID planId, int expectedVersion, String status) {
        return jdbcClient.sql("""
                        UPDATE review_plan
                        SET version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :planId
                          AND owner_id = :ownerId
                          AND version = :expectedVersion
                          AND status = :status
                        """)
                .param("planId", planId)
                .param("ownerId", ownerId)
                .param("expectedVersion", expectedVersion)
                .param("status", status)
                .update();
    }

    public int changePlanStatus(
            UUID ownerId,
            UUID planId,
            int expectedVersion,
            String currentStatus,
            String newStatus) {
        return jdbcClient.sql("""
                        UPDATE review_plan
                        SET status = :newStatus,
                            version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :planId
                          AND owner_id = :ownerId
                          AND version = :expectedVersion
                          AND status = :currentStatus
                        """)
                .param("planId", planId)
                .param("ownerId", ownerId)
                .param("expectedVersion", expectedVersion)
                .param("currentStatus", currentStatus)
                .param("newStatus", newStatus)
                .update();
    }

    public void cancelPendingOccurrences(UUID planId) {
        jdbcClient.sql("""
                        UPDATE review_occurrence occurrence
                        SET status = 'CANCELED',
                            resolved_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE occurrence.plan_id = :planId
                          AND occurrence.status = 'SCHEDULED'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM study_session session
                              WHERE session.review_occurrence_id = occurrence.id
                                AND session.status IN ('ACTIVE', 'PAUSED')
                          )
                        """)
                .param("planId", planId)
                .update();
    }

    public void restoreCanceledOccurrences(UUID planId) {
        jdbcClient.sql("""
                        UPDATE review_occurrence
                        SET status = 'SCHEDULED',
                            resolved_at = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE plan_id = :planId
                          AND status = 'CANCELED'
                        """)
                .param("planId", planId)
                .update();
    }

    public int rescheduleOccurrence(
            UUID planId,
            UUID occurrenceId,
            LocalDate dueDate,
            LocalDate today) {
        return jdbcClient.sql("""
                        UPDATE review_occurrence
                        SET due_date = :dueDate,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :occurrenceId
                          AND plan_id = :planId
                          AND status = 'SCHEDULED'
                          AND due_date > :today
                        """)
                .param("planId", planId)
                .param("occurrenceId", occurrenceId)
                .param("dueDate", dueDate)
                .param("today", today)
                .update();
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
