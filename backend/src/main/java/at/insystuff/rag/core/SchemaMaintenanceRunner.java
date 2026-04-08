package at.insystuff.rag.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops legacy schema constraints that prevent uploading documents to the
 * SimpleVectorStore (second vector DB).
 *
 * <p>The original init SQL created a FK on {@code vector_store_document_chunk.vector_id}
 * pointing to {@code vector_store.id}.  Because the in-memory SimpleVectorStore never
 * writes rows to the {@code vector_store} table, every upload to that backend failed
 * with a FK-violation.  This runner drops that constraint on startup so existing
 * deployments are fixed automatically without recreating the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMaintenanceRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropConstraintIfExists("vector_store_document_chunk", "vector_store_document_chunk_vector_id_fkey");
        addColumnIfMissing("document_metadata", "vector_store_type", "TEXT");
    }

    /**
     * Validates that the given name is a safe PostgreSQL identifier (letters, digits,
     * underscores only) before using it in a DDL statement.
     */
    private static void validateIdentifier(String name) {
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + name);
        }
    }

    private void dropConstraintIfExists(String table, String constraint) {
        validateIdentifier(table);
        validateIdentifier(constraint);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_schema = 'public' AND table_name = ? AND constraint_name = ?",
                Integer.class, table, constraint);
        if (count != null && count > 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE public." + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            log.info("Dropped legacy FK constraint '{}' from table '{}'", constraint, table);
        }
    }

    private void addColumnIfMissing(String table, String column, String type) {
        validateIdentifier(table);
        validateIdentifier(column);
        validateIdentifier(type);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE public." + table + " ADD COLUMN IF NOT EXISTS " + column + " " + type);
            log.info("Added missing column '{}.{}' ({})", table, column, type);
        }
    }
}
