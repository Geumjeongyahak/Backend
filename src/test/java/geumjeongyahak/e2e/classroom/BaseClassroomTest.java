package geumjeongyahak.e2e.classroom;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestChannelHelper;
import geumjeongyahak.e2e.util.TestClassroomHelper;

@Tag("classroom")
public abstract class BaseClassroomTest extends BaseE2ETest {
    protected static String TEST_ADMIN_USERNAME = "classroomAdminUser1234";
    protected static String TEST_GUEST_USERNAME = "classroomGuestUser1234";

    protected String adminAccessToken;
    protected String guestAccessToken;

    @Autowired
    protected TestClassroomHelper testClassroomHelper;

    @Autowired
    protected ChannelRepository channelRepository;

    @Autowired
    protected TestChannelHelper testChannelHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/classrooms";

        // 테스트용 게스트 사용자 생성
        this.userTestHelper.createTestUser(TEST_ADMIN_USERNAME, RoleType.ADMIN);
        this.userTestHelper.createTestUser(TEST_GUEST_USERNAME, RoleType.GUEST);
        // 토큰 생성
        this.adminAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_ADMIN_USERNAME);
        this.guestAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_GUEST_USERNAME);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();

        // 테스트 데이터 정리
        this.testChannelHelper.clearAll();
        this.testClassroomHelper.clearAll();
    }
}
