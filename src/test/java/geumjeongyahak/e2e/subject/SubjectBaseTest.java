package geumjeongyahak.e2e.subject;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.e2e.BaseE2ETest;

@Tag("subject")
public class SubjectBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "teacher01";

    protected String adminAccessToken;
    protected String volunteerAccessToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/subjects";
        cleanSubjectTables();
        this.adminAccessToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessToken(TEST_VOLUNTEER_USERNAME);
    }

    private void cleanSubjectTables() {
        // H2에서 FK 때문에 truncate 실패하는 경우가 있어 referential integrity를 잠깐 꺼줌
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // lessons -> subjects 순서(lessons가 subject_id FK 가짐)
        jdbcTemplate.execute("TRUNCATE TABLE lessons");
        jdbcTemplate.execute("TRUNCATE TABLE subjects");

        // ID를 1부터 다시 시작
        jdbcTemplate.execute("ALTER TABLE lessons ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE subjects ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
