package geumjeongyahak.unit.advice;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.common.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionTimedOutException;

@DisplayName("GlobalExceptionHandler DB 실패 변환 단위 테스트")
class GlobalExceptionHandlerDatabaseFailureTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("락 획득 실패는 409 BIZ005로 답한다")
    void lockFailure_returnsResourceBusy() {
        ResponseEntity<ProblemDetail> response = handler.handlePessimisticLockingFailureException(
            new CannotAcquireLockException("lock"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getProperties()).containsEntry("code", "BIZ005");
    }

    @Test
    @DisplayName("질의 시간 초과는 클라이언트 오류가 아니라 503 SYS006으로 답한다")
    void queryTimeout_returnsProcessingTimeout() {
        ResponseEntity<ProblemDetail> response = handler.handleProcessingTimeout(new QueryTimeoutException("timeout"));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getProperties()).containsEntry("code", "SYS006");
    }

    @Test
    @DisplayName("트랜잭션 시간 초과는 503 SYS006으로 답한다")
    void transactionTimeout_returnsProcessingTimeout() {
        ResponseEntity<ProblemDetail> response = handler.handleProcessingTimeout(
            new TransactionTimedOutException("timeout"));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getProperties()).containsEntry("code", "SYS006");
    }
}
