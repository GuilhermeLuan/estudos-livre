package br.com.estudalivre.migration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.estudalivre.testing.IntegrationTest;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class StudyCycleRunMigrationIntegrationTest {

    private static final int ORIGINAL_V8_CHECKSUM = -839255919;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void upgradesAnExistingVersionEightDatabaseWithoutLosingStudyCycleRuns() {
        String schema = "migration_upgrade_" + UUID.randomUUID().toString().replace("-", "");

        try {
            Flyway versionEight = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("8"))
                    .load();
            versionEight.migrate();

            jdbcTemplate.update("""
                    UPDATE %s.flyway_schema_history
                    SET checksum = ?
                    WHERE version = '8'
                    """.formatted(schema), ORIGINAL_V8_CHECKSUM);

            UUID ownerId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO %s.identity_user (id, email, password_hash, time_zone)
                    VALUES (?, 'migration@example.com', 'hash', 'America/Sao_Paulo')
                    """.formatted(schema), ownerId);
            jdbcTemplate.update("""
                    INSERT INTO %s.study_cycle (id, owner_id, name, status)
                    VALUES (?, ?, 'Ciclo existente', 'ACTIVE')
                    """.formatted(schema), cycleId, ownerId);
            jdbcTemplate.update("""
                    INSERT INTO %s.study_cycle_run
                        (id, cycle_id, run_number, current_stage_position, completed_at)
                    VALUES (?, ?, 1, 3, CURRENT_TIMESTAMP), (?, ?, 2, 2, NULL)
                    """.formatted(schema), UUID.randomUUID(), cycleId, UUID.randomUUID(), cycleId);

            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            List<String> statuses = jdbcTemplate.queryForList("""
                    SELECT status
                    FROM %s.study_cycle_run
                    ORDER BY run_number
                    """.formatted(schema), String.class);
            Integer legacyColumns = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = 'study_cycle_run'
                      AND column_name IN ('current_stage_position', 'completed_at')
                    """, Integer.class, schema);
            Integer currentColumns = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = 'study_cycle_run'
                      AND column_name IN ('status', 'ended_at')
                    """, Integer.class, schema);

            assertThat(statuses).containsExactly("COMPLETED", "IN_PROGRESS");
            assertThat(legacyColumns).isZero();
            assertThat(currentColumns).isEqualTo(2);
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
