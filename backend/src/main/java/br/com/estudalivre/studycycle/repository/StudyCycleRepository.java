package br.com.estudalivre.studycycle.repository;

import br.com.estudalivre.studycycle.model.StudyCycle;
import br.com.estudalivre.studycycle.model.StudyCycleRun;
import br.com.estudalivre.studycycle.model.StudyCycleRunStage;
import br.com.estudalivre.studycycle.model.StudyCycleStage;
import br.com.estudalivre.studycycle.planner.StudyCycleDifficulty;
import br.com.estudalivre.studycycle.planner.SuggestedStudyCycleSubject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StudyCycleRepository {

    private final JdbcClient jdbcClient;

    public StudyCycleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(UUID id, UUID ownerId, String name) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle (id, owner_id, name)
                        VALUES (:id, :ownerId, :name)
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public void createSuggested(UUID id, UUID ownerId, String name) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle (id, owner_id, name, mode)
                        VALUES (:id, :ownerId, :name, 'SUGGESTED')
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public void createSuggestionSubject(
            UUID cycleId,
            int inputPosition,
            SuggestedStudyCycleSubject subject) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle_suggestion_subject (
                            cycle_id, subject_id, input_position, question_count, weight,
                            difficulty, priority, allocated_minutes, appearance_count
                        ) VALUES (
                            :cycleId, :subjectId, :inputPosition, :questionCount, :weight,
                            :difficulty, :priority, :allocatedMinutes, :appearanceCount
                        )
                        """)
                .param("cycleId", cycleId)
                .param("subjectId", subject.subjectId())
                .param("inputPosition", inputPosition)
                .param("questionCount", subject.questionCount())
                .param("weight", subject.weight())
                .param("difficulty", subject.difficulty().name())
                .param("priority", subject.priority())
                .param("allocatedMinutes", subject.allocatedMinutes())
                .param("appearanceCount", subject.appearanceCount())
                .update();
    }

    public List<SuggestedStudyCycleSubject> findSuggestionSubjects(UUID cycleId) {
        return jdbcClient.sql("""
                        SELECT input.subject_id, subject.name AS subject_name,
                               input.question_count, input.weight, input.difficulty,
                               input.priority, input.allocated_minutes, input.appearance_count
                        FROM study_cycle_suggestion_subject input
                        JOIN subject ON subject.id = input.subject_id
                        WHERE input.cycle_id = :cycleId
                        ORDER BY input.input_position
                        """)
                .param("cycleId", cycleId)
                .query((resultSet, rowNumber) -> new SuggestedStudyCycleSubject(
                        resultSet.getObject("subject_id", UUID.class),
                        resultSet.getString("subject_name"),
                        resultSet.getInt("question_count"),
                        resultSet.getInt("weight"),
                        StudyCycleDifficulty.valueOf(resultSet.getString("difficulty")),
                        resultSet.getObject("priority", BigDecimal.class),
                        resultSet.getInt("allocated_minutes"),
                        resultSet.getInt("appearance_count")))
                .list();
    }

    public Optional<StudyCycle> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, mode, status, created_at, updated_at
                        FROM study_cycle
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query(StudyCycleRepository::mapCycle)
                .optional();
    }

    public List<StudyCycle> findByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, mode, status, created_at, updated_at
                        FROM study_cycle
                        WHERE owner_id = :ownerId
                        ORDER BY LOWER(name), id
                        """)
                .param("ownerId", ownerId)
                .query(StudyCycleRepository::mapCycle)
                .list();
    }

    public Optional<StudyCycle> findActiveByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, mode, status, created_at, updated_at
                        FROM study_cycle
                        WHERE owner_id = :ownerId AND status = 'ACTIVE'
                        """)
                .param("ownerId", ownerId)
                .query(StudyCycleRepository::mapCycle)
                .optional();
    }

    public int update(UUID id, UUID ownerId, String name) {
        return jdbcClient.sql("""
                        UPDATE study_cycle
                        SET name = :name, mode = 'CUSTOM', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public int updateSuggested(UUID id, UUID ownerId, String name) {
        return jdbcClient.sql("""
                        UPDATE study_cycle
                        SET name = :name, mode = 'SUGGESTED', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public void deleteSuggestion(UUID cycleId) {
        jdbcClient.sql("DELETE FROM study_cycle_suggestion_subject WHERE cycle_id = :cycleId")
                .param("cycleId", cycleId)
                .update();
    }

    public void deleteStages(UUID cycleId) {
        jdbcClient.sql("DELETE FROM study_cycle_stage WHERE cycle_id = :cycleId")
                .param("cycleId", cycleId)
                .update();
    }

    public void createStage(
            UUID id,
            UUID cycleId,
            UUID subjectId,
            int position,
            int targetMinutes) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle_stage (id, cycle_id, subject_id, position, target_minutes)
                        VALUES (:id, :cycleId, :subjectId, :position, :targetMinutes)
                        """)
                .param("id", id)
                .param("cycleId", cycleId)
                .param("subjectId", subjectId)
                .param("position", position)
                .param("targetMinutes", targetMinutes)
                .update();
    }

    public List<StudyCycleStage> findStages(UUID cycleId) {
        return jdbcClient.sql("""
                        SELECT stage.id, stage.cycle_id, stage.subject_id, subject.name AS subject_name,
                               stage.position, stage.target_minutes,
                               COALESCE(run_stage.credited_seconds, 0) AS credited_seconds
                        FROM study_cycle_stage stage
                        JOIN subject ON subject.id = stage.subject_id
                        LEFT JOIN study_cycle_run current_run
                          ON current_run.cycle_id = stage.cycle_id
                         AND current_run.status IN ('IN_PROGRESS', 'PAUSED')
                        LEFT JOIN study_cycle_run_stage run_stage
                          ON run_stage.run_id = current_run.id
                         AND run_stage.source_stage_id = stage.id
                        WHERE stage.cycle_id = :cycleId
                        ORDER BY stage.position
                        """)
                .param("cycleId", cycleId)
                .query(StudyCycleRepository::mapStage)
                .list();
    }

    public int activate(UUID cycleId, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE study_cycle
                        SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
                        WHERE id = :cycleId AND owner_id = :ownerId
                        """)
                .param("cycleId", cycleId)
                .param("ownerId", ownerId)
                .update();
    }

    public void lockOwner(UUID ownerId) {
        jdbcClient.sql("""
                        SELECT id
                        FROM identity_user
                        WHERE id = :ownerId
                        FOR UPDATE
                        """)
                .param("ownerId", ownerId)
                .query(UUID.class)
                .single();
    }

    public void deactivateActiveCycles(UUID ownerId, UUID exceptCycleId) {
        jdbcClient.sql("""
                        UPDATE study_cycle
                        SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
                        WHERE owner_id = :ownerId
                          AND status = 'ACTIVE'
                          AND id <> :exceptCycleId
                        """)
                .param("ownerId", ownerId)
                .param("exceptCycleId", exceptCycleId)
                .update();
    }

    public void pauseCurrentRun(UUID cycleId) {
        jdbcClient.sql("""
                        UPDATE study_cycle_run
                        SET status = 'PAUSED', updated_at = CURRENT_TIMESTAMP
                        WHERE cycle_id = :cycleId AND status = 'IN_PROGRESS'
                        """)
                .param("cycleId", cycleId)
                .update();
    }

    public void resumeCurrentRun(UUID cycleId) {
        jdbcClient.sql("""
                        UPDATE study_cycle_run
                        SET status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                        WHERE cycle_id = :cycleId AND status = 'PAUSED'
                        """)
                .param("cycleId", cycleId)
                .update();
    }

    public void abandonCurrentRun(UUID cycleId) {
        jdbcClient.sql("""
                        UPDATE study_cycle_run
                        SET status = 'ABANDONED', ended_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE cycle_id = :cycleId AND status = 'IN_PROGRESS'
                        """)
                .param("cycleId", cycleId)
                .update();
    }

    public void createRun(UUID id, UUID cycleId) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle_run (id, cycle_id, run_number)
                        SELECT :id, :cycleId, COALESCE(MAX(run_number), 0) + 1
                        FROM study_cycle_run
                        WHERE cycle_id = :cycleId
                        """)
                .param("id", id)
                .param("cycleId", cycleId)
                .update();
    }

    public void snapshotRunStages(UUID runId, UUID cycleId) {
        jdbcClient.sql("""
                        INSERT INTO study_cycle_run_stage (
                            id, run_id, cycle_id, source_stage_id, subject_id,
                            subject_name, position, target_seconds
                        )
                        SELECT gen_random_uuid(), :runId, :cycleId, stage.id, stage.subject_id,
                               subject.name, stage.position, stage.target_minutes * 60::BIGINT
                        FROM study_cycle_stage stage
                        JOIN subject ON subject.id = stage.subject_id
                        WHERE stage.cycle_id = :cycleId
                        ORDER BY stage.position
                        """)
                .param("runId", runId)
                .param("cycleId", cycleId)
                .update();
    }

    public List<StudyCycleRunStage> findRunStagesForUpdate(UUID runId) {
        return jdbcClient.sql("""
                        SELECT id, run_id, cycle_id, source_stage_id, subject_id, subject_name,
                               position, target_seconds, credited_seconds
                        FROM study_cycle_run_stage
                        WHERE run_id = :runId
                        ORDER BY position
                        FOR UPDATE
                        """)
                .param("runId", runId)
                .query(StudyCycleRepository::mapRunStage)
                .list();
    }

    public List<StudyCycleRunStage> findRunStages(UUID runId) {
        return jdbcClient.sql("""
                        SELECT id, run_id, cycle_id, source_stage_id, subject_id, subject_name,
                               position, target_seconds, credited_seconds
                        FROM study_cycle_run_stage
                        WHERE run_id = :runId
                        ORDER BY position
                        """)
                .param("runId", runId)
                .query(StudyCycleRepository::mapRunStage)
                .list();
    }

    public int creditRunStage(UUID runStageId, long creditedSeconds) {
        return jdbcClient.sql("""
                        UPDATE study_cycle_run_stage
                        SET credited_seconds = credited_seconds + :creditedSeconds,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :runStageId
                          AND credited_seconds + :creditedSeconds <= target_seconds
                        """)
                .param("runStageId", runStageId)
                .param("creditedSeconds", creditedSeconds)
                .update();
    }

    public int completeRunIfFinished(UUID runId) {
        return jdbcClient.sql("""
                        UPDATE study_cycle_run run
                        SET status = 'COMPLETED', ended_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE run.id = :runId
                          AND run.status IN ('IN_PROGRESS', 'PAUSED')
                          AND NOT EXISTS (
                              SELECT 1
                              FROM study_cycle_run_stage stage
                              WHERE stage.run_id = run.id
                                AND stage.credited_seconds < stage.target_seconds
                          )
                        """)
                .param("runId", runId)
                .update();
    }

    public List<StudyCycleRun> findRuns(UUID cycleId) {
        return jdbcClient.sql("""
                        SELECT id, cycle_id, run_number, status, started_at, ended_at
                        FROM study_cycle_run
                        WHERE cycle_id = :cycleId
                        ORDER BY run_number DESC
                        """)
                .param("cycleId", cycleId)
                .query(StudyCycleRepository::mapRun)
                .list();
    }

    public Optional<StudyCycleRun> findCurrentRun(UUID cycleId) {
        return jdbcClient.sql("""
                        SELECT id, cycle_id, run_number, status, started_at, ended_at
                        FROM study_cycle_run
                        WHERE cycle_id = :cycleId
                          AND status IN ('IN_PROGRESS', 'PAUSED')
                        ORDER BY run_number DESC
                        LIMIT 1
                        """)
                .param("cycleId", cycleId)
                .query(StudyCycleRepository::mapRun)
                .optional();
    }

    private static StudyCycle mapCycle(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudyCycle(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("owner_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("mode"),
                resultSet.getString("status"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private static StudyCycleStage mapStage(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudyCycleStage(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("cycle_id", UUID.class),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("subject_name"),
                resultSet.getInt("position"),
                resultSet.getInt("target_minutes"),
                resultSet.getLong("credited_seconds"));
    }

    private static StudyCycleRun mapRun(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudyCycleRun(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("cycle_id", UUID.class),
                resultSet.getInt("run_number"),
                resultSet.getString("status"),
                resultSet.getObject("started_at", OffsetDateTime.class),
                resultSet.getObject("ended_at", OffsetDateTime.class));
    }

    private static StudyCycleRunStage mapRunStage(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudyCycleRunStage(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("run_id", UUID.class),
                resultSet.getObject("cycle_id", UUID.class),
                resultSet.getObject("source_stage_id", UUID.class),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("subject_name"),
                resultSet.getInt("position"),
                resultSet.getLong("target_seconds"),
                resultSet.getLong("credited_seconds"));
    }
}
