package sonmoeum.e2e.channel;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.e2e.BaseE2ETest;
import sonmoeum.e2e.util.TestChannelHelper;

import java.util.List;

@Tag("channel")
public abstract class BaseChannelTest extends BaseE2ETest {
    protected static final String TEST_CHANNEL_ADMIN_USERNAME = "channelAdminUser1234";
    protected static final String TEST_CHANNEL_GUEST_USERNAME = "channelGuestUser1234";

    protected String adminAccessToken;
    protected String guestAccessToken;

    @Autowired
    protected TestChannelHelper testChannelHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/channels";

        userTestHelper.createTestUser(TEST_CHANNEL_ADMIN_USERNAME, List.of(RoleType.ROLE_ADMIN));
        userTestHelper.createTestUser(TEST_CHANNEL_GUEST_USERNAME, List.of(RoleType.ROLE_GUEST));

        adminAccessToken = userTestHelper.generateAccessToken(TEST_CHANNEL_ADMIN_USERNAME);
        guestAccessToken = userTestHelper.generateAccessToken(TEST_CHANNEL_GUEST_USERNAME);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        super.tearDown();
        testChannelHelper.clearAll();
    }
}
