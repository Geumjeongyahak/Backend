package geumjeongyahak.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.util.TestUserHelper;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
@Tag("e2e")
public abstract class BaseE2ETest {
    public static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);
    public static final String AUTH_HEADER = "Authorization";
    public static final String TEST_ADMIN_USERNAME = "admin@test.com";
    public static final String TEST_ADMIN_EMAIL = "admin@test.com";
    public static final String TEST_ADMIN_PASSWORD = "admin1234";

    @Autowired
    protected TestUserHelper userTestHelper;

    @LocalServerPort
    protected int port;

    @BeforeEach
    protected void setUp() {
        log.info("포트 {} 에서 E2E 테스트 시작", port);
        RestAssured.baseURI = "http://localhost" + ":" + port;
        RestAssured.basePath = "";
        log.info("RestAssured.baseURI 설정: {}", RestAssured.baseURI);

        // Ensure admin user exists with correct password for E2E tests
        userTestHelper.createTestUser(TEST_ADMIN_EMAIL, "관리자", TEST_ADMIN_PASSWORD, RoleType.ADMIN);
    }

    @AfterEach
    protected void tearDown() {
        userTestHelper.clearAll();
    }

    protected String getAuthHeader(String accessToken) {
        return "Bearer " + accessToken;
    }
}
