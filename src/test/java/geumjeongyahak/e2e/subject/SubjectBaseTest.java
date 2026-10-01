package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("subject")
public class SubjectBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "teacher01";
    public static final String TEST_SUBJECT_WRITER_USERNAME = "subjectWriter1234";
    public static final String TEST_SUBJECT_MANAGER_USERNAME = "subjectManager1234";
    private static final String SUBJECT_WRITE_PERMISSION = "subject:write:*";
    private static final String SUBJECT_MANAGE_PERMISSION = "subject:manage:*";

    protected static final long DEFAULT_CLASSROOM_ID = 1L;
    protected static final long DEFAULT_TEACHER_ID = 2L;

    protected String adminAccessToken;
    protected String volunteerAccessToken;
    protected String subjectWriteAccessToken;
    protected String subjectManageAccessToken;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/subjects";
        cleanSubjectTables();
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_VOLUNTEER_USERNAME);

        User subjectWriter = userTestHelper.createTestUser(TEST_SUBJECT_WRITER_USERNAME, RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(subjectWriter.getId(), SUBJECT_WRITE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(subjectWriter, SUBJECT_WRITE_PERMISSION)));
        this.subjectWriteAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_SUBJECT_WRITER_USERNAME);

        User subjectManager = userTestHelper.createTestUser(TEST_SUBJECT_MANAGER_USERNAME, RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(subjectManager.getId(), SUBJECT_MANAGE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(subjectManager, SUBJECT_MANAGE_PERMISSION)));
        this.subjectManageAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_SUBJECT_MANAGER_USERNAME);
    }

    /** 2099-03-02~2099-06-30, 19:20–20:00, 교사 DEFAULT_TEACHER_ID 과목 요청. */
    protected Map<String, Object> createRequest(long classroomId, String name, String dayOfWeek, int period) {
        return Map.ofEntries(
            Map.entry("classroomId", classroomId),
            Map.entry("teacherId", DEFAULT_TEACHER_ID),
            Map.entry("name", name),
            Map.entry("startAt", "2099-03-02"),
            Map.entry("endAt", "2099-06-30"),
            Map.entry("dayOfWeek", dayOfWeek),
            Map.entry("startTime", "19:20:00"),
            Map.entry("endTime", "20:00:00"),
            Map.entry("period", period),
            Map.entry("description", "PATCH 테스트")
        );
    }

    protected long createSubject(long classroomId, String name, String dayOfWeek, int period) {
        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest(classroomId, name, dayOfWeek, period))
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    protected long createSubjectWithoutTeacher() {
        Map<String, Object> request = new HashMap<>(createRequest(DEFAULT_CLASSROOM_ID, "미배정 과목", "MONDAY", 2));
        request.remove("teacherId");

        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((Object) null))
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    /** 월요일 과목을 만들고 id를 돌려준다. teacherId가 null이면 담당 교사 없이 만든다. */
    protected long createMondaySubject(
        long classroomId,
        Long teacherId,
        String name,
        String startAt,
        String endAt,
        String startTime,
        String endTime,
        int period
    ) {
        Map<String, Object> request = new HashMap<>(Map.of(
            "classroomId", classroomId,
            "name", name,
            "startAt", startAt,
            "endAt", endAt,
            "dayOfWeek", "MONDAY",
            "startTime", startTime,
            "endTime", endTime,
            "period", period
        ));
        if (teacherId != null) {
            request.put("teacherId", teacherId);
        }

        return given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    protected int countActiveLessons(long subjectId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId
        );
    }

    private void cleanSubjectTables() {
        // H2에서 FK 때문에 truncate 실패하는 경우가 있어 referential integrity를 잠깐 꺼줌
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // lesson 의존 테이블 -> lessons -> subjects 순서(lessons가 subject_id FK 가짐)
        jdbcTemplate.execute("TRUNCATE TABLE absence_requests");
        jdbcTemplate.execute("TRUNCATE TABLE lesson_exchange_proposals");
        jdbcTemplate.execute("TRUNCATE TABLE lesson_exchange_requests");
        jdbcTemplate.execute("TRUNCATE TABLE daily_student_attendances");
        jdbcTemplate.execute("TRUNCATE TABLE daily_teacher_attendances");
        jdbcTemplate.execute("TRUNCATE TABLE daily_schedules");
        jdbcTemplate.execute("TRUNCATE TABLE lessons");
        jdbcTemplate.execute("TRUNCATE TABLE subjects");

        // ID를 1부터 다시 시작
        jdbcTemplate.execute("ALTER TABLE absence_requests ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lesson_exchange_proposals ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lesson_exchange_requests ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE daily_student_attendances ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE daily_teacher_attendances ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE daily_schedules ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lessons ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE subjects ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
