package br.com.estudalivre.studycycle.repository;

import br.com.estudalivre.studycycle.model.StudyCycle;
import br.com.estudalivre.studycycle.model.StudyCycleStage;
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

    public int updateDraft(UUID id, UUID ownerId, String name) {
        return jdbcClient.sql("""
                        UPDATE study_cycle
                        SET name = :name, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId AND status = 'DRAFT'
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
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
                               stage.position, stage.target_minutes
                        FROM study_cycle_stage stage
                        JOIN subject ON subject.id = stage.subject_id
                        WHERE stage.cycle_id = :cycleId
                        ORDER BY stage.position
                        """)
                .param("cycleId", cycleId)
                .query(StudyCycleRepository::mapStage)
                .list();
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
                resultSet.getInt("target_minutes"));
    }
}
