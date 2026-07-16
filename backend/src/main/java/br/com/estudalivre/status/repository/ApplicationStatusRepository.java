package br.com.estudalivre.status.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationStatusRepository {

    private final JdbcClient jdbcClient;

    public ApplicationStatusRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public String findSchemaVersion() {
        return jdbcClient.sql("SELECT schema_version FROM application_status WHERE singleton = TRUE")
                .query(String.class)
                .single();
    }
}
