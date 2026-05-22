package geumjeongyahak.e2e.channel;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestChannelHelper;

@Tag("channel")
public abstract class BaseChannelTest extends BaseE2ETest {
    protected static final String TEST_CHANNEL_ADMIN_USERNAME = "channelAdminUser1234@test.com";
    protected static final String TEST_CHANNEL_GUEST_USERNAME = "channelGuestUser1234@test.com";

    protected String adminAccessToken;
    protected String guestAccessToken;

    @Autowired
    protected TestChannelHelper testChannelHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/channels";

        userTestHelper.createTestUser(TEST_CHANNEL_ADMIN_USERNAME, RoleType.ADMIN);
        userTestHelper.createTestUser(TEST_CHANNEL_GUEST_USERNAME, RoleType.GUEST);

        adminAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_CHANNEL_ADMIN_USERNAME);
        guestAccessToken = userTestHelper.generateAccessTokenByEmail(TEST_CHANNEL_GUEST_USERNAME);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        super.tearDown();
        testChannelHelper.clearAll();
    }
}
