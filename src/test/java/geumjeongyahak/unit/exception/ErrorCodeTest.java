package geumjeongyahak.unit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import geumjeongyahak.common.exception.ErrorCode;
import geumjeongyahak.common.exception.ErrorCodeRegistry;

@DisplayName("ErrorCode 단위 테스트")
class ErrorCodeTest {

    @Test
    @DisplayName("모든 에러 코드는 고유한 code 값을 가진다")
    void errorCodes_areUnique() {
        Map<String, Long> codeCounts = ErrorCodeRegistry.getAll().stream()
            .collect(Collectors.groupingBy(ErrorCode::getCode, Collectors.counting()));

        assertThat(codeCounts.values())
            .allMatch(count -> count == 1L);
    }
}
