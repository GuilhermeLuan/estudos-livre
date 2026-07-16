package br.com.estudalivre.subject.repository;

import br.com.estudalivre.subject.model.Subject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectRepository {

    private final JdbcClient jdbcClient;

    public SubjectRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(UUID id, UUID ownerId, String name) {
        jdbcClient.sql("""
                        INSERT INTO subject (id, owner_id, name)
                        VALUES (:id, :ownerId, :name)
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public Optional<Subject> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, archived_at, created_at, updated_at
                        FROM subject
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .query(SubjectRepository::mapSubject)
                .optional();
    }

    public List<Subject> findActiveByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, archived_at, created_at, updated_at
                        FROM subject
                        WHERE owner_id = :ownerId AND archived_at IS NULL
                        ORDER BY LOWER(name), id
                        """)
                .param("ownerId", ownerId)
                .query(SubjectRepository::mapSubject)
                .list();
    }

    public List<Subject> findArchivedByOwnerId(UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT id, owner_id, name, archived_at, created_at, updated_at
                        FROM subject
                        WHERE owner_id = :ownerId AND archived_at IS NOT NULL
                        ORDER BY LOWER(name), id
                        """)
                .param("ownerId", ownerId)
                .query(SubjectRepository::mapSubject)
                .list();
    }

    public int updateName(UUID id, UUID ownerId, String name) {
        return jdbcClient.sql("""
                        UPDATE subject
                        SET name = :name, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public int archive(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE subject
                        SET archived_at = COALESCE(archived_at, CURRENT_TIMESTAMP),
                            updated_at = CASE WHEN archived_at IS NULL THEN CURRENT_TIMESTAMP ELSE updated_at END
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .update();
    }

    public int restore(UUID id, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE subject
                        SET archived_at = NULL,
                            updated_at = CASE WHEN archived_at IS NOT NULL THEN CURRENT_TIMESTAMP ELSE updated_at END
                        WHERE id = :id AND owner_id = :ownerId
                        """)
                .param("id", id)
                .param("ownerId", ownerId)
                .update();
    }

    private static Subject mapSubject(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Subject(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("owner_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getObject("archived_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }
}
