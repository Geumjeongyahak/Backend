package geumjeongyahak.e2e.notification;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.notification.repository.PushSubscriptionRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Tag("notification")
@Import(TestNotificationConfig.class)
public abstract class BaseNotificationTest extends BaseE2ETest {

    protected static final String TEST_NOTIFICATION_USER = "notificationUser1234";
    protected static final String TEST_NOTIFICATION_OTHER_USER = "notificationOtherUser1234";
    protected static final String TEST_NOTIFICATION_ADMIN = "notificationAdmin1234";

    protected String accessToken;
    protected String otherAccessToken;
    protected String adminEmail;
    protected String adminPassword;

    @Autowired
    protected PushSubscriptionRepository pushSubscriptionRepository;

    @Autowired
    protected TestNotificationConfig.ControlledPushNotificationSender controlledPushNotificationSender;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/push/subscriptions";

        userTestHelper.createTestUser(TEST_NOTIFICATION_USER, RoleType.VOLUNTEER);
        userTestHelper.createTestUser(TEST_NOTIFICATION_OTHER_USER, RoleType.VOLUNTEER);
        userTestHelper.createTestUser(TEST_NOTIFICATION_ADMIN, RoleType.ADMIN);

        accessToken = userTestHelper.generateAccessTokenByNickname(TEST_NOTIFICATION_USER);
        otherAccessToken = userTestHelper.generateAccessTokenByNickname(TEST_NOTIFICATION_OTHER_USER);
        adminEmail = TEST_NOTIFICATION_ADMIN + "@test.com";
        adminPassword = userTestHelper.getDefaultPassword(TEST_NOTIFICATION_ADMIN);
        controlledPushNotificationSender.reset();
    }

    @AfterEach
    @Override
    protected void tearDown() {
        pushSubscriptionRepository.deleteAll();
        pushSubscriptionRepository.flush();
        controlledPushNotificationSender.reset();
        super.tearDown();
    }
}
