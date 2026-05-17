package geumjeongyahak.e2e.subject;

import io.restassured.RestAssured;
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

    protected String adminAccessToken;
    protected String volunteerAccessToken;
    protected String subjectWriteAccessToken;
    protected String subjectManageAccessToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/subjects";
        cleanSubjectTables();
        this.adminAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_VOLUNTEER_USERNAME);

        User subjectWriter = userTestHelper.createTestUser(TEST_SUBJECT_WRITER_USERNAME, RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(subjectWriter.getId(), SUBJECT_WRITE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(subjectWriter, SUBJECT_WRITE_PERMISSION)));
        this.subjectWriteAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_SUBJECT_WRITER_USERNAME);

        User subjectManager = userTestHelper.createTestUser(TEST_SUBJECT_MANAGER_USERNAME, RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(subjectManager.getId(), SUBJECT_MANAGE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(subjectManager, SUBJECT_MANAGE_PERMISSION)));
        this.subjectManageAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_SUBJECT_MANAGER_USERNAME);
    }

    private void cleanSubjectTables() {
        // H2에서 FK 때문에 truncate 실패하는 경우가 있어 referential integrity를 잠깐 꺼줌
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // lesson 의존 테이블 -> lessons -> subjects 순서(lessons가 subject_id FK 가짐)
        jdbcTemplate.execute("TRUNCATE TABLE absence_requests");
        jdbcTemplate.execute("TRUNCATE TABLE lesson_exchange_proposals");
        jdbcTemplate.execute("TRUNCATE TABLE lesson_exchange_requests");
        jdbcTemplate.execute("TRUNCATE TABLE lessons");
        jdbcTemplate.execute("TRUNCATE TABLE subjects");

        // ID를 1부터 다시 시작
        jdbcTemplate.execute("ALTER TABLE absence_requests ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lesson_exchange_proposals ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lesson_exchange_requests ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE lessons ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE subjects ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
