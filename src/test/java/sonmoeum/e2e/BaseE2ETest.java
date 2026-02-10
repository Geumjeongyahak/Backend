package sonmoeum.e2e;

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
import sonmoeum.e2e.util.TestUserHelper;

/**
 * E2E 테스트 Base 클래스
 * - MockMvc를 사용한 통합 테스트
 * - 실제 HTTP 요청/응답 시뮬레이션
 * - JWT 토큰 기반 인증 지원
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("e2e")
public abstract class BaseE2ETest {
    public static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);
    public static final String AUTH_HEADER = "Authorization";
    public static final String TEST_ADMIN_USERNAME = "admin1234";
    public static final String TEST_ADMIN_PASSWORD = "admin1234";

    @Autowired
    protected TestUserHelper userTestHelper;


    @LocalServerPort
    protected int port;

    @BeforeEach
    protected void setUp() {
        log.info("포트 {} 에서 E2E 테스트 시작", port);
        RestAssured.baseURI = "http://localhost" + ":" + port;
        log.info("RestAssured.baseURI 설정: {}", RestAssured.baseURI);
    }

    @AfterEach
    protected void tearDown() {
        userTestHelper.clearAll();
    }

    protected String getAuthHeader(String accessToken) {
        return "Bearer " + accessToken;
    }
}
