package geumjeongyahak.unit.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQL 스키마 정합성 테스트")
class SqlSchemaConsistencyTest {

    private static final Path V1_SCHEMA = Path.of("src/main/resources/db/migration/V1__initial_schema.sql");
    private static final Path MAIN_SCHEMA = Path.of("src/main/resources/sql/init_scheme.sql");
    private static final Path TEST_SCHEMA = Path.of("src/test/resources/sql/init_scheme.sql");
    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final List<String> USER_CREDENTIAL_COLUMNS = List.of(
        "password_reset_token_hash",
        "password_reset_token_expires_at",
        "password_reset_requested_at",
        "password_reset_failed_attempts",
        "email_verification_token_hash",
        "email_verification_token_expires_at",
        "email_verification_requested_at",
        "email_verification_failed_attempts"
    );
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
        "(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z_]+)"
    );

    @Test
    @DisplayName("V1, 개발, 테스트 스키마는 최신 인증 컬럼을 모두 포함해야 한다")
    void schemas_containLatestUserCredentialColumns() throws IOException {
        for (Path schema : List.of(V1_SCHEMA, MAIN_SCHEMA, TEST_SCHEMA)) {
            String sql = Files.readString(schema);
            for (String column : USER_CREDENTIAL_COLUMNS) {
                assertTrue(sql.contains(column), () -> schema + "에 " + column + " 컬럼이 없습니다.");
            }
        }
    }

    @Test
    @DisplayName("V1, 개발, 테스트 스키마의 테이블 목록은 같아야 한다")
    void schemas_haveSameTables() throws IOException {
        Set<String> expectedTables = tableNames(MAIN_SCHEMA);

        assertEquals(expectedTables, tableNames(V1_SCHEMA), "V1 테이블 목록이 개발 스키마와 다릅니다.");
        assertEquals(expectedTables, tableNames(TEST_SCHEMA), "테스트 테이블 목록이 개발 스키마와 다릅니다.");
    }

    @Test
    @DisplayName("첫 배포 전 V1에 흡수한 add migration은 남기지 않는다")
    void absorbedAuthMigrations_areRemoved() throws IOException {
        try (var files = Files.list(MIGRATION_DIR)) {
            List<String> remaining = files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.contains("add_password_reset_token_columns")
                    || name.contains("add_email_verification_token_columns"))
                .toList();

            assertTrue(remaining.isEmpty(), () -> "흡수 대상 migration이 남아 있습니다: " + remaining);
        }
    }

    private static Set<String> tableNames(Path schema) throws IOException {
        return CREATE_TABLE_PATTERN.matcher(Files.readString(schema))
            .results()
            .map(match -> match.group(1).toLowerCase())
            .collect(Collectors.toCollection(java.util.TreeSet::new));
    }
}
