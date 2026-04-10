package sonmoeum.e2e.post;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.enums.ChannelWriterPolicy;
import sonmoeum.domain.channel.repository.ChannelRepository;
import sonmoeum.e2e.BaseE2ETest;
import sonmoeum.e2e.util.TestChannelHelper;
import sonmoeum.e2e.util.TestCommentHelper;
import sonmoeum.e2e.util.TestPostHelper;

import java.util.List;

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
    protected TestChannelHelper testChannelHelper;

    @Autowired
    protected TestCommentHelper testCommentHelper;

    @Autowired
    protected ChannelRepository channelRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        userTestHelper.createTestUser(TEST_POST_ADMIN_USERNAME, List.of(RoleType.ROLE_ADMIN));
        userTestHelper.createTestUser(TEST_POST_GUEST_USERNAME, List.of(RoleType.ROLE_GUEST));

        adminAccessToken = userTestHelper.generateAccessToken(TEST_POST_ADMIN_USERNAME);
        guestAccessToken = userTestHelper.generateAccessToken(TEST_POST_GUEST_USERNAME);

        Channel channel = channelRepository.save(Channel.builder()
                .name("테스트 공지 채널")
                .slug("test-notice-" + System.currentTimeMillis())
                .description("Post E2E 테스트용 공지 채널")
                .channelType(ChannelType.ALL)
                .writerPolicy(ChannelWriterPolicy.ADMIN_MANAGER_ONLY)
                .isDefault(false)
                .isActive(true)
                .sortOrder(1)
                .build());
        noticeChannelId = channel.getId();
        testChannelHelper.registerChannel(noticeChannelId);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        testCommentHelper.clearAll();
        testPostHelper.clearAll();
        testChannelHelper.clearAll();
        super.tearDown();
    }
}
