package geumjeongyahak.e2e.sitecontent;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.sitecontent.repository.SiteContentRepository;
import geumjeongyahak.domain.sitecontent.repository.SiteHistoryRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("sitecontent")
public abstract class SiteContentBaseTest extends BaseE2ETest {

    public static final String TEST_VOLUNTEER_USERNAME = "siteContentVolunteer";

    public String adminAccessToken;
    public String volunteerAccessToken;

    @Autowired
    protected SiteContentRepository siteContentRepository;

    @Autowired
    protected SiteHistoryRepository siteHistoryRepository;

    @Autowired
    protected FileRepository fileRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/site-contents";
        clearSiteContentData();
        userTestHelper.createTestUser(TEST_VOLUNTEER_USERNAME, RoleType.VOLUNTEER);
        this.adminAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        this.volunteerAccessToken = userTestHelper.generateAccessTokenByUserKey(TEST_VOLUNTEER_USERNAME);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        clearSiteContentData();
        super.tearDown();
    }

    private void clearSiteContentData() {
        siteHistoryRepository.deleteAll();
        siteContentRepository.deleteAll();
        fileRepository.deleteAll();
    }
}
