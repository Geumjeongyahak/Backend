package geumjeongyahak.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.users.service.UserProxyService;
import geumjeongyahak.e2e.request.RequestBaseTest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 승인 트랜잭션이 요청을 읽은 뒤 다른 트랜잭션(만료 스케줄러 등)이 같은 행을 먼저 바꾸면
 * 승인은 낡은 버전을 저장하게 되고 409로 끝나야 한다.
 */
@Tag("absence-request")
@DisplayName("E2E: 결석 요청 낙관적 락")
class AbsenceRequestOptimisticLockTest extends RequestBaseTest {

    @MockitoSpyBean
    private UserProxyService userProxyService;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long requestId;

    @AfterEach
    void cleanup() {
        if (requestId != null) {
            absenceRequestRepository.deleteById(requestId);
        }
        lessonHelper.clearAll(getAuthHeader(adminToken));
    }

    @Test
    @DisplayName("승인 도중 다른 트랜잭션이 요청을 먼저 바꾸면 -> 409 BIZ004, 상태는 다른 쪽 결과")
    void approve_withStaleVersion_returns409() {
        Long subjectId = lessonHelper.createSubjectAndRegister(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID, "낙관락");
        Long lessonId = lessonHelper.createLessonAndRegister(getAuthHeader(adminToken), subjectId, TEACHER_ID);
        requestId = createAbsenceRequest(getAuthHeader(volunteerToken), lessonId, "낙관적 락");

        // 승인 서비스가 요청을 읽은 뒤 승인자를 조회하는 순간, 별도 트랜잭션이 요청을 만료시킨다
        AtomicBoolean armed = new AtomicBoolean(true);
        TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        doAnswer(invocation -> {
            if (armed.getAndSet(false)) {
                concurrent.executeWithoutResult(status -> jdbcTemplate.update(
                    "update absence_requests set status = 'EXPIRED', version = version + 1 where id = ?",
                    requestId
                ));
            }
            return invocation.callRealMethod();
        }).when(userProxyService).getById(anyLong());

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("BIZ004"));

        assertThat(armed).isFalse();
        assertThat(absenceRequestRepository.findById(requestId).orElseThrow().getStatus())
            .isEqualTo(RequestStatus.EXPIRED);
    }
}
