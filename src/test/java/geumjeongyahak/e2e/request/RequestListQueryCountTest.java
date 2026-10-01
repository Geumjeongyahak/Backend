package geumjeongyahak.e2e.request;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService.ReviewRequiredRequestRow;
import geumjeongyahak.domain.users.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 요청 목록·관리자 대시보드가 행 수만큼 질의를 늘리지 않는지 센다 (#240 A).
 * 행을 더 만든 뒤에도 질의 수가 같아야 한다.
 */
@DisplayName("E2E: 요청 목록 질의 수")
class RequestListQueryCountTest extends RequestBaseTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;
    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;
    @Autowired
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;
    @Autowired
    private LessonExchangeRequestAdminViewService adminViewService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<Long> absenceRequestIds = new ArrayList<>();
    private final List<Long> exchangeRequestIds = new ArrayList<>();
    private final List<Long> proposalIds = new ArrayList<>();
    private int exchangeDayOffset = 40;

    @AfterEach
    void cleanup() {
        proposalIds.forEach(lessonExchangeProposalRepository::deleteById);
        exchangeRequestIds.forEach(lessonExchangeRequestRepository::deleteById);
        absenceRequestIds.forEach(absenceRequestRepository::deleteById);
        lessonHelper.clearAll(getAuthHeader(adminToken));
    }

    @Test
    @DisplayName("결석 요청 목록 질의 수는 행 수와 무관하다")
    void absenceList_queryCountDoesNotGrowWithRows() {
        createAbsenceRequests(TEACHER_ID, volunteerToken, 1);
        createAbsenceRequests(TEACHER2_ID, volunteer2Token, 1);
        long few = countStatements(() -> getList("/api/v1/absence-requests"));

        createAbsenceRequests(TEACHER_ID, volunteerToken, 2);
        createAbsenceRequests(TEACHER2_ID, volunteer2Token, 2);
        long more = countStatements(() -> getList("/api/v1/absence-requests"));

        log.info("[A 측정] 결석 요청 목록: 2행={} 6행={}", few, more);
        assertThat(more).isEqualTo(few);
    }

    @Test
    @DisplayName("수업 교환 요청 목록 질의 수는 행 수와 무관하다")
    void exchangeList_queryCountDoesNotGrowWithRows() {
        createExchangeRequest(TEACHER_ID, volunteerToken);
        createExchangeRequest(TEACHER2_ID, volunteer2Token);
        long few = countStatements(() -> getList("/api/v1/lesson-exchange-requests"));

        createExchangeRequest(TEACHER_ID, volunteerToken);
        createExchangeRequest(TEACHER2_ID, volunteer2Token);
        createExchangeRequest(TEACHER_ID, volunteerToken);
        createExchangeRequest(TEACHER2_ID, volunteer2Token);
        long more = countStatements(() -> getList("/api/v1/lesson-exchange-requests"));

        log.info("[A 측정] 수업 교환 요청 목록: 2행={} 6행={}", few, more);
        assertThat(more).isEqualTo(few);
    }

    @Test
    @DisplayName("관리자 대시보드는 검토 대기 요청마다 제안 수를 따로 세지 않고, 개수는 그대로다")
    void dashboard_countsProposalsInOneQuery() {
        Long first = createExchangeRequest(TEACHER_ID, volunteerToken);
        Long second = createExchangeRequest(TEACHER2_ID, volunteer2Token);
        addProposal(first, false);
        addProposal(first, true);
        addProposal(second, false);
        long few = countStatements(adminViewService::getDashboard);

        createExchangeRequest(TEACHER_ID, volunteerToken);
        createExchangeRequest(TEACHER2_ID, volunteer2Token);
        createExchangeRequest(TEACHER_ID, volunteerToken);
        createExchangeRequest(TEACHER2_ID, volunteer2Token);
        long more = countStatements(adminViewService::getDashboard);

        log.info("[A 측정] 관리자 대시보드: 2행={} 6행={}", few, more);
        assertThat(more).isEqualTo(few);

        List<ReviewRequiredRequestRow> rows = adminViewService.getDashboard().reviewRequiredRequests();
        ReviewRequiredRequestRow firstRow = rowOf(rows, first);
        assertThat(firstRow.proposalCount()).isEqualTo(2);
        assertThat(firstRow.activeProposalCount()).isEqualTo(1);
        assertThat(firstRow.requestedByName()).isNotBlank();
        ReviewRequiredRequestRow secondRow = rowOf(rows, second);
        assertThat(secondRow.proposalCount()).isEqualTo(1);
        assertThat(secondRow.activeProposalCount()).isEqualTo(1);
        ReviewRequiredRequestRow emptyRow = rowOf(rows, exchangeRequestIds.getLast());
        assertThat(emptyRow.proposalCount()).isZero();
        assertThat(emptyRow.activeProposalCount()).isZero();
    }

    private long countStatements(Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long before = statistics.getPrepareStatementCount();
        action.run();
        return statistics.getPrepareStatementCount() - before;
    }

    private void getList(String path) {
        given()
            .basePath(path)
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get()
            .then()
            .statusCode(200);
    }

    private void createAbsenceRequests(long teacherId, String token, int count) {
        Long subjectId = lessonHelper.createSubjectAndRegister(
            getAuthHeader(adminToken), CLASSROOM_ID, teacherId, "질의수");
        for (int i = 0; i < count; i++) {
            Long lessonId = lessonHelper.createLessonAndRegister(getAuthHeader(adminToken), subjectId, teacherId);
            absenceRequestIds.add(createAbsenceRequest(getAuthHeader(token), lessonId, "질의 수 측정"));
        }
    }

    private Long createExchangeRequest(long teacherId, String token) {
        LocalDate lessonDate = LocalDate.now().plusDays(exchangeDayOffset++);
        Long subjectId = lessonHelper.createSubjectAndRegister(
            getAuthHeader(adminToken), CLASSROOM_ID, teacherId, "질의수");
        lessonHelper.createLessonAndRegister(
            getAuthHeader(adminToken), subjectId, teacherId, lessonDate.toString(), "09:00:00", "10:00:00", 1);
        Long requestId = createLessonExchangeRequest(
            getAuthHeader(token), lessonDate, "질의 수 측정", "내용", lessonDate.minusDays(3).atTime(23, 0));
        exchangeRequestIds.add(requestId);
        return requestId;
    }

    private void addProposal(Long requestId, boolean withdrawn) {
        Long proposalId = transactionTemplate.execute(status -> {
            LessonExchangeProposal proposal = new LessonExchangeProposal(
                lessonExchangeRequestRepository.getReferenceById(requestId),
                userRepository.getReferenceById(1L),
                LessonExchangeProposalType.SUBSTITUTION,
                null,
                "제안",
                null
            );
            if (withdrawn) {
                proposal.withdraw();
            }
            return lessonExchangeProposalRepository.save(proposal).getId();
        });
        proposalIds.add(proposalId);
    }

    private ReviewRequiredRequestRow rowOf(List<ReviewRequiredRequestRow> rows, Long requestId) {
        return rows.stream().filter(row -> row.id().equals(requestId)).findFirst().orElseThrow();
    }
}
