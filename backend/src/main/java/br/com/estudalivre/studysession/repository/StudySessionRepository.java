package br.com.estudalivre.studysession.repository;

import br.com.estudalivre.studysession.model.StudySession;
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
                   (
                       SELECT COALESCE(
                           FLOOR(SUM(EXTRACT(EPOCH FROM (
                               COALESCE(segment.ended_at, CURRENT_TIMESTAMP) - segment.started_at
                           )))),
                           0
                       )::BIGINT
                       FROM study_session_timer_segment segment
                       WHERE segment.session_id = study_session.id
                   ) AS measured_seconds,
                   CURRENT_TIMESTAMP AS server_now
            FROM study_session
            JOIN subject ON subject.id = study_session.subject_id
            LEFT JOIN content ON content.id = study_session.content_id
            LEFT JOIN study_cycle ON study_cycle.id = study_session.cycle_id
            LEFT JOIN study_cycle_run ON study_cycle_run.id = study_session.cycle_run_id
            LEFT JOIN study_cycle_stage ON study_cycle_stage.id = study_session.cycle_stage_id
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
                resultSet.getLong("measured_seconds"),
                resultSet.getObject("server_now", OffsetDateTime.class));
    }
}
