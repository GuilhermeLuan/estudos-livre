package br.com.estudalivre.studysession.repository;

import br.com.estudalivre.studysession.model.StudySession;
import br.com.estudalivre.studysession.model.StudySessionCredit;
import br.com.estudalivre.studysession.model.ExerciseResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StudySessionRepository {

    private static final String SESSION_PROJECTION = """
            SELECT study_session.id,
                   study_session.owner_id,
                   study_session.origin,
                   study_session.status,
                   study_session.subject_id,
                   subject.name AS subject_name,
                   study_session.content_id,
                   content.name AS content_name,
                   study_session.cycle_id,
                   study_cycle.name AS cycle_name,
                   study_session.cycle_run_id,
                   study_cycle_run.run_number,
                   study_session.cycle_stage_id,
                   study_cycle_stage.position AS stage_position,
                   study_cycle_stage.target_minutes,
                   study_session.started_at,
                   study_session.notes,
                   study_session.effective_seconds,
                   study_session.finished_at,
                   study_session.version,
                   exercise_result.questions_attempted,
                   exercise_result.questions_correct,
                   CASE WHEN study_session.origin = 'MANUAL'
                     THEN study_session.effective_seconds
                     ELSE (
                       SELECT COALESCE(
                           FLOOR(SUM(EXTRACT(EPOCH FROM (
                               COALESCE(segment.ended_at, CURRENT_TIMESTAMP) - segment.started_at
                           )))),
                           0
                       )::BIGINT
                       FROM study_session_timer_segment segment
                       WHERE segment.session_id = study_session.id
                     )
                   END AS measured_seconds,
                   CURRENT_TIMESTAMP AS server_now
            FROM study_session
            JOIN subject ON subject.id = study_session.subject_id
            LEFT JOIN content ON content.id = study_session.content_id
            LEFT JOIN study_cycle ON study_cycle.id = study_session.cycle_id
            LEFT JOIN study_cycle_run ON study_cycle_run.id = study_session.cycle_run_id
            LEFT JOIN study_cycle_stage ON study_cycle_stage.id = study_session.cycle_stage_id
            LEFT JOIN study_session_exercise_result exercise_result
              ON exercise_result.session_id = study_session.id
            """;

    private final JdbcClient jdbcClient;

    public StudySessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(
            UUID id,
            UUID ownerId,
            String origin,
            UUID subjectId,
            UUID contentId,
            UUID cycleId,
            UUID cycleRunId,
            UUID cycleStageId) {
        jdbcClient.sql("""
                        INSERT INTO study_session (
                            id, owner_id, origin, subject_id, content_id,
                            cycle_id, cycle_run_id, cycle_stage_id
                        ) VALUES (
                            :id, :ownerId, :origin, :subjectId, :contentId,
                            :cycleId, :cycleRunId, :cycleStageId
                        )
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("origin", origin)
                .param("subjectId", subjectId)
                .param("contentId", contentId)
                .param("cycleId", cycleId)
                .param("cycleRunId", cycleRunId)
                .param("cycleStageId", cycleStageId)
                .update();
    }

    public void createTimerSegment(UUID id, UUID sessionId) {
        jdbcClient.sql("""
                        INSERT INTO study_session_timer_segment (id, session_id)
                        VALUES (:id, :sessionId)
                        """)
                .param("id", id)
                .param("sessionId", sessionId)
                .update();
    }

    public void createManual(
            UUID id,
            UUID ownerId,
            UUID subjectId,
            UUID contentId,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            long effectiveSeconds,
            String notes) {
        jdbcClient.sql("""
                        INSERT INTO study_session (
                            id, owner_id, origin, status, subject_id, content_id,
                            started_at, finished_at, effective_seconds, notes
                        ) VALUES (
                            :id, :ownerId, 'MANUAL', 'FINISHED', :subjectId, :contentId,
                            :startedAt, :finishedAt, :effectiveSeconds, :notes
                        )
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("subjectId", subjectId)
                .param("contentId", contentId)
                .param("startedAt", startedAt)
                .param("finishedAt", finishedAt)
                .param("effectiveSeconds", effectiveSeconds)
                .param("notes", notes)
                .update();
    }

    public void createReview(
            UUID id,
            UUID ownerId,
            UUID subjectId,
            UUID contentId,
            UUID reviewOccurrenceId) {
        jdbcClient.sql("""
                        INSERT INTO study_session (
                            id, owner_id, origin, subject_id, content_id, review_occurrence_id
                        ) VALUES (
                            :id, :ownerId, 'REVIEW', :subjectId, :contentId, :reviewOccurrenceId
                        )
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("subjectId", subjectId)
                .param("contentId", contentId)
                .param("reviewOccurrenceId", reviewOccurrenceId)
                .update();
    }

    public Optional<StudySession> findCurrentByOwnerId(UUID ownerId) {
        return jdbcClient.sql(SESSION_PROJECTION + """
                        WHERE study_session.owner_id = :ownerId
                          AND study_session.status IN ('ACTIVE', 'PAUSED')
                        """)
                .param("ownerId", ownerId)
                .query(StudySessionRepository::mapSession)
                .optional();
    }

    public Optional<StudySession> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return jdbcClient.sql(SESSION_PROJECTION + """
                        WHERE study_session.id = :id
                          AND study_session.owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query(StudySessionRepository::mapSession)
                .optional();
    }

    public java.util.List<StudySession> findHistoryByOwnerId(UUID ownerId) {
        return jdbcClient.sql(SESSION_PROJECTION + """
                        WHERE study_session.owner_id = :ownerId
                          AND study_session.status = 'FINISHED'
                        ORDER BY study_session.started_at DESC, study_session.id DESC
                        """)
                .param("ownerId", ownerId)
                .query(StudySessionRepository::mapSession)
                .list();
    }

    public Optional<String> findStatusForUpdate(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT status
                        FROM study_session
                        WHERE id = :id AND owner_id = :ownerId
                        FOR UPDATE
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query(String.class)
                .optional();
    }

    public Optional<FinishState> findFinishStateForUpdate(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT status, origin, subject_id, content_id, cycle_run_id,
                               review_occurrence_id,
                               started_at, effective_seconds, version
                        FROM study_session
                        WHERE id = :id AND owner_id = :ownerId
                        FOR UPDATE
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new FinishState(
                        resultSet.getString("status"),
                        resultSet.getString("origin"),
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getObject("content_id", UUID.class),
                        resultSet.getObject("cycle_run_id", UUID.class),
                        resultSet.getObject("review_occurrence_id", UUID.class),
                        resultSet.getObject("started_at", OffsetDateTime.class),
                        resultSet.getObject("effective_seconds", Long.class),
                        resultSet.getInt("version")))
                .optional();
    }

    public int pause(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE study_session
                        SET status = 'PAUSED', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId AND status = 'ACTIVE'
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .update();
    }

    public int closeCurrentTimerSegment(UUID sessionId) {
        return jdbcClient.sql("""
                        UPDATE study_session_timer_segment
                        SET ended_at = CURRENT_TIMESTAMP
                        WHERE session_id = :sessionId AND ended_at IS NULL
                        """)
                .param("sessionId", sessionId)
                .update();
    }

    public int resume(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE study_session
                        SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId AND status = 'PAUSED'
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .update();
    }

    public int finish(UUID id, UUID ownerId, long effectiveSeconds, int expectedVersion) {
        return jdbcClient.sql("""
                        UPDATE study_session
                        SET status = 'FINISHED', effective_seconds = :effectiveSeconds,
                            finished_at = CURRENT_TIMESTAMP, version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId
                          AND status IN ('ACTIVE', 'PAUSED')
                          AND version = :expectedVersion
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("effectiveSeconds", effectiveSeconds)
                .param("expectedVersion", expectedVersion)
                .update();
    }

    public void saveExerciseResult(UUID sessionId, ExerciseResult result) {
        jdbcClient.sql("""
                        INSERT INTO study_session_exercise_result (
                            session_id, questions_attempted, questions_correct
                        ) VALUES (
                            :sessionId, :questionsAttempted, :questionsCorrect
                        )
                        ON CONFLICT (session_id) DO UPDATE SET
                            questions_attempted = EXCLUDED.questions_attempted,
                            questions_correct = EXCLUDED.questions_correct,
                            updated_at = CURRENT_TIMESTAMP
                        """)
                .param("sessionId", sessionId)
                .param("questionsAttempted", result.questionsAttempted())
                .param("questionsCorrect", result.questionsCorrect())
                .update();
    }

    public void deleteExerciseResult(UUID sessionId) {
        jdbcClient.sql("""
                        DELETE FROM study_session_exercise_result
                        WHERE session_id = :sessionId
                        """)
                .param("sessionId", sessionId)
                .update();
    }

    public void createCredit(UUID sessionId, UUID runStageId, long creditedSeconds) {
        jdbcClient.sql("""
                        INSERT INTO study_session_credit (session_id, run_stage_id, credited_seconds)
                        VALUES (:sessionId, :runStageId, :creditedSeconds)
                        """)
                .param("sessionId", sessionId)
                .param("runStageId", runStageId)
                .param("creditedSeconds", creditedSeconds)
                .update();
    }

    public java.util.List<StudySessionCredit> findCredits(UUID sessionId) {
        return jdbcClient.sql("""
                        SELECT credit.run_stage_id, run_stage.cycle_id, run_stage.run_id,
                               run_stage.source_stage_id, run_stage.position,
                               credit.credited_seconds
                        FROM study_session_credit credit
                        JOIN study_cycle_run_stage run_stage ON run_stage.id = credit.run_stage_id
                        WHERE credit.session_id = :sessionId
                        ORDER BY run_stage.position
                        """)
                .param("sessionId", sessionId)
                .query((resultSet, rowNumber) -> new StudySessionCredit(
                        resultSet.getObject("run_stage_id", UUID.class),
                        resultSet.getObject("cycle_id", UUID.class),
                        resultSet.getObject("run_id", UUID.class),
                        resultSet.getObject("source_stage_id", UUID.class),
                        resultSet.getInt("position"),
                        resultSet.getLong("credited_seconds")))
                .list();
    }

    public java.util.List<SubjectExerciseAggregate> findSubjectExerciseSummary(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT session.subject_id, subject.name AS subject_name,
                               SUM(result.questions_attempted) AS questions_attempted,
                               SUM(result.questions_correct) AS questions_correct
                        FROM study_session_exercise_result result
                        JOIN study_session session ON session.id = result.session_id
                        JOIN subject ON subject.id = session.subject_id
                        WHERE session.owner_id = :ownerId
                        GROUP BY session.subject_id, subject.name
                        ORDER BY subject.name, session.subject_id
                        """)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new SubjectExerciseAggregate(
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_name"),
                        resultSet.getLong("questions_attempted"),
                        resultSet.getLong("questions_correct")))
                .list();
    }

    public java.util.List<ContentExerciseAggregate> findContentExerciseSummary(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT session.content_id, content.name AS content_name,
                               session.subject_id, subject.name AS subject_name,
                               SUM(result.questions_attempted) AS questions_attempted,
                               SUM(result.questions_correct) AS questions_correct
                        FROM study_session_exercise_result result
                        JOIN study_session session ON session.id = result.session_id
                        JOIN subject ON subject.id = session.subject_id
                        JOIN content ON content.id = session.content_id
                        WHERE session.owner_id = :ownerId
                        GROUP BY session.content_id, content.name, session.subject_id, subject.name
                        ORDER BY subject.name, content.name, session.content_id
                        """)
                .param("ownerId", ownerId)
                .query((resultSet, rowNumber) -> new ContentExerciseAggregate(
                        resultSet.getObject("content_id", UUID.class),
                        resultSet.getString("content_name"),
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_name"),
                        resultSet.getLong("questions_attempted"),
                        resultSet.getLong("questions_correct")))
                .list();
    }

    private static StudySession mapSession(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudySession(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("owner_id", UUID.class),
                resultSet.getString("origin"),
                resultSet.getString("status"),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("subject_name"),
                resultSet.getObject("content_id", UUID.class),
                resultSet.getString("content_name"),
                resultSet.getObject("cycle_id", UUID.class),
                resultSet.getString("cycle_name"),
                resultSet.getObject("cycle_run_id", UUID.class),
                resultSet.getObject("run_number", Integer.class),
                resultSet.getObject("cycle_stage_id", UUID.class),
                resultSet.getObject("stage_position", Integer.class),
                resultSet.getObject("target_minutes", Integer.class),
                resultSet.getObject("started_at", OffsetDateTime.class),
                resultSet.getString("notes"),
                resultSet.getLong("measured_seconds"),
                resultSet.getObject("effective_seconds", Long.class),
                resultSet.getObject("finished_at", OffsetDateTime.class),
                resultSet.getInt("version"),
                resultSet.getObject("questions_attempted", Integer.class) == null
                        ? null
                        : new ExerciseResult(
                                resultSet.getInt("questions_attempted"),
                                resultSet.getInt("questions_correct")),
                resultSet.getObject("server_now", OffsetDateTime.class));
    }

    public record FinishState(
            String status,
            String origin,
            UUID subjectId,
            UUID contentId,
            UUID cycleRunId,
            UUID reviewOccurrenceId,
            OffsetDateTime startedAt,
            Long effectiveSeconds,
            int version) {
    }

    public record SubjectExerciseAggregate(
            UUID subjectId,
            String subjectName,
            long questionsAttempted,
            long questionsCorrect) {
    }

    public record ContentExerciseAggregate(
            UUID contentId,
            String contentName,
            UUID subjectId,
            String subjectName,
            long questionsAttempted,
            long questionsCorrect) {
    }
}
