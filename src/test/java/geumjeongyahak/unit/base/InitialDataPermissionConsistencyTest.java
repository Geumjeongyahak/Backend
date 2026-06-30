package geumjeongyahak.unit.base;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import geumjeongyahak.domain.base.model.PermissionCode;

@DisplayName("초기 데이터 권한 정합성 테스트")
class InitialDataPermissionConsistencyTest {

    private static final Pattern PERMISSION_INSERT_PATTERN = Pattern.compile(
        "INSERT\\s+INTO\\s+(?:user_permissions|department_permissions)\\b.*?;",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile(
        "'([a-z][a-z0-9_-]*:[a-z][a-z0-9_]*:[^']+)'"
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("initialDataFiles")
    @DisplayName("개발 및 테스트 초기 데이터의 권한 코드는 현재 정책에서 허용되어야 한다")
    void permissionCodes_followCurrentPolicy(String description, Path initialDataFile) throws IOException {
        String sql = Files.readString(initialDataFile);
        List<String> permissionCodes = PERMISSION_INSERT_PATTERN.matcher(sql).results()
            .flatMap(statement -> PERMISSION_CODE_PATTERN.matcher(statement.group()).results())
            .map(result -> result.group(1))
            .toList();

        assertFalse(permissionCodes.isEmpty(), () -> "권한 초기 데이터를 찾을 수 없습니다: " + initialDataFile);
        permissionCodes.forEach(permissionCode ->
            assertDoesNotThrow(
                () -> new PermissionCode(permissionCode),
                () -> "허용되지 않는 초기 권한입니다: " + initialDataFile + " -> " + permissionCode
            )
        );
    }

    private static Stream<Object[]> initialDataFiles() {
        return Stream.of(
            new Object[]{"개발 초기 데이터", Path.of("src/main/resources/sql/init_data.sql")},
            new Object[]{"테스트 초기 데이터", Path.of("src/test/resources/sql/init_data.sql")}
        );
    }
}
