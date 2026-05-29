package geumjeongyahak.e2e.post;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestChannelHelper;
import geumjeongyahak.e2e.util.TestCommentHelper;
import geumjeongyahak.e2e.util.TestFileHelper;
import geumjeongyahak.e2e.util.TestPostHelper;

@Tag("post")
public abstract class BasePostTest extends BaseE2ETest {
    protected static final String TEST_POST_ADMIN_USERNAME = "postAdminUser1234";
    protected static final String TEST_POST_GUEST_USERNAME = "postGuestUser1234";

    protected String adminAccessToken;
    protected String guestAccessToken;
    protected Long noticeChannelId;

    @Autowired
    protected TestPostHelper testPostHelper;

    @Autowired
    protected TestFileHelper testFileHelper;

    @Autowired
    protected TestChannelHelper testChannelHelper;

    @Autowired
    protected TestCommentHelper testCommentHelper;

    @Autowired
    protected ChannelRepository channelRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        userTestHelper.createTestUser(TEST_POST_ADMIN_USERNAME, RoleType.ADMIN);
        userTestHelper.createTestUser(TEST_POST_GUEST_USERNAME, RoleType.GUEST);

        adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_POST_ADMIN_USERNAME);
        guestAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_POST_GUEST_USERNAME);

        Channel channel = channelRepository.save(Channel.builder()
                .name("테스트 공지 채널")
                .description("Post E2E 테스트용 공지 채널")
                .channelType(ChannelType.NOTICE)
                .bindingType(ChannelBindingType.STANDALONE)
                .accessLevel(ChannelAccessLevel.READ_ONLY)
                .allowGuestRead(true)
                .refId(null)
                .isDefault(false)
                .isActive(true)
                .build());
        noticeChannelId = channel.getId();
        testChannelHelper.registerChannel(noticeChannelId);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        testCommentHelper.clearAll();
        testPostHelper.clearAll();
        testFileHelper.clearAll();
        testChannelHelper.clearAll();
        super.tearDown();
    }
}
