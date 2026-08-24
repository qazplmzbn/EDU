package com.xyz.question_bank_management_system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.migration.teacher-agent-resource", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TeacherAgentResourceMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("qb_learning_resource", "content",
                "ALTER TABLE qb_learning_resource ADD COLUMN content LONGTEXT NULL AFTER summary");
        addColumnIfMissing("qb_learning_resource", "personalization_basis",
                "ALTER TABLE qb_learning_resource ADD COLUMN personalization_basis JSON NULL AFTER content");
        addColumnIfMissing("qb_learning_resource", "review_report_json",
                "ALTER TABLE qb_learning_resource ADD COLUMN review_report_json JSON NULL AFTER personalization_basis");
        addColumnIfMissing("qb_learning_resource", "model_source_json",
                "ALTER TABLE qb_learning_resource ADD COLUMN model_source_json JSON NULL AFTER review_report_json");
        addColumnIfMissing("qb_learning_resource", "audit_status",
                "ALTER TABLE qb_learning_resource ADD COLUMN audit_status VARCHAR(40) NOT NULL DEFAULT 'manual' AFTER model_source_json");
        addIndexIfMissing("qb_learning_resource", "idx_resource_audit_status",
                "CREATE INDEX idx_resource_audit_status ON qb_learning_resource(audit_status)");
        createTableIfMissing("qb_learning_resource_target", """
                CREATE TABLE qb_learning_resource_target (
                  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  resource_id BIGINT NOT NULL,
                  student_id BIGINT NOT NULL,
                  class_id BIGINT NULL,
                  target_type VARCHAR(30) NOT NULL DEFAULT 'student',
                  created_by BIGINT NULL,
                  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  UNIQUE KEY uk_resource_target_student (resource_id, student_id),
                  KEY idx_resource_target_student (student_id, created_at),
                  KEY idx_resource_target_class (class_id)
                )
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND INDEX_NAME = ?
            """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void createTableIfMissing(String tableName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
            """, Integer.class, tableName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
