package br.com.estudalivre.subject.repository;

import br.com.estudalivre.subject.model.Content;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContentRepository {

    private final JdbcClient jdbcClient;

    public ContentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(UUID id, UUID subjectId, String name) {
        jdbcClient.sql("""
                        INSERT INTO content (id, subject_id, name)
                        VALUES (:id, :subjectId, :name)
                        """)
                .param("id", id)
                .param("subjectId", subjectId)
                .param("name", name)
                .update();
    }

    public Optional<Content> findByIdAndSubjectIdAndOwnerId(UUID id, UUID subjectId, UUID ownerId) {
        return jdbcClient.sql("""
                        SELECT c.id, c.subject_id, c.name, c.archived_at, c.created_at, c.updated_at
                        FROM content c
                        JOIN subject s ON s.id = c.subject_id
                        WHERE c.id = :id AND c.subject_id = :subjectId AND s.owner_id = :ownerId
                        """)
                .param("id", id)
                .param("subjectId", subjectId)
                .param("ownerId", ownerId)
                .query(ContentRepository::mapContent)
                .optional();
    }

    public List<Content> findActiveBySubjectIdAndOwnerId(UUID subjectId, UUID ownerId) {
        return findByStatus(subjectId, ownerId, false);
    }

    public List<Content> findArchivedBySubjectIdAndOwnerId(UUID subjectId, UUID ownerId) {
        return findByStatus(subjectId, ownerId, true);
    }

    public int updateName(UUID id, UUID subjectId, UUID ownerId, String name) {
        return jdbcClient.sql("""
                        UPDATE content c
                        SET name = :name, updated_at = CURRENT_TIMESTAMP
                        FROM subject s
                        WHERE c.id = :id
                          AND c.subject_id = :subjectId
                          AND s.id = c.subject_id
                          AND s.owner_id = :ownerId
                        """)
                .param("id", id)
                .param("subjectId", subjectId)
                .param("ownerId", ownerId)
                .param("name", name)
                .update();
    }

    public int archive(UUID id, UUID subjectId, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE content c
                        SET archived_at = COALESCE(c.archived_at, CURRENT_TIMESTAMP),
                            updated_at = CASE
                                WHEN c.archived_at IS NULL THEN CURRENT_TIMESTAMP
                                ELSE c.updated_at
                            END
                        FROM subject s
                        WHERE c.id = :id
                          AND c.subject_id = :subjectId
                          AND s.id = c.subject_id
                          AND s.owner_id = :ownerId
                        """)
                .param("id", id)
                .param("subjectId", subjectId)
                .param("ownerId", ownerId)
                .update();
    }

    public int restore(UUID id, UUID subjectId, UUID ownerId) {
        return jdbcClient.sql("""
                        UPDATE content c
                        SET archived_at = NULL,
                            updated_at = CASE
                                WHEN c.archived_at IS NOT NULL THEN CURRENT_TIMESTAMP
                                ELSE c.updated_at
                            END
                        FROM subject s
                        WHERE c.id = :id
                          AND c.subject_id = :subjectId
                          AND s.id = c.subject_id
                          AND s.owner_id = :ownerId
                        """)
                .param("id", id)
                .param("subjectId", subjectId)
                .param("ownerId", ownerId)
                .update();
    }

    private List<Content> findByStatus(UUID subjectId, UUID ownerId, boolean archived) {
        return jdbcClient.sql("""
                        SELECT c.id, c.subject_id, c.name, c.archived_at, c.created_at, c.updated_at
                        FROM content c
                        JOIN subject s ON s.id = c.subject_id
                        WHERE c.subject_id = :subjectId
                          AND s.owner_id = :ownerId
                          AND ((:archived AND c.archived_at IS NOT NULL)
                            OR (NOT :archived AND c.archived_at IS NULL))
                        ORDER BY LOWER(c.name), c.id
                        """)
                .param("subjectId", subjectId)
                .param("ownerId", ownerId)
                .param("archived", archived)
                .query(ContentRepository::mapContent)
                .list();
    }

    private static Content mapContent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Content(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("subject_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getObject("archived_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }
}
