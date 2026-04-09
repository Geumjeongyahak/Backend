package sonmoeum.e2e.file;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import io.restassured.RestAssured;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.file.repository.FileRepository;
import sonmoeum.e2e.BaseE2ETest;

@Tag("file")
@Import(TestFileStorageConfig.class)
public abstract class BaseFileTest extends BaseE2ETest {

    protected static final String TEST_FILE_USER = "fileUser1234";

    protected String userAccessToken;

    @Autowired
    protected FileRepository fileRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/files";

        userTestHelper.createTestUser(TEST_FILE_USER, List.of(RoleType.ROLE_VOLUNTEER));
        userAccessToken = userTestHelper.generateAccessToken(TEST_FILE_USER);
    }

    @AfterEach
    @Override
    public void tearDown() {
        fileRepository.deleteAll();
        super.tearDown();
    }
}
