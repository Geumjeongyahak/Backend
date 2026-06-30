package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.v1.dto.request.PublishPostRequest;
import geumjeongyahak.domain.post.v1.dto.request.SaveDraftRequest;

@DisplayName("E2E: Post 파일 및 썸네일 테스트")
@ResourceLock("post-e2e-shared-state")
class PostFileTest extends BasePostTest {

    @Autowired
    private FileRepository fileRepository;

    @Test
    @DisplayName("이미지를 업로드하면 발행 시 첫 번째 이미지가 자동으로 썸네일이 된다")
    void autoThumbnail_Success() {
        // 1. 초안 생성
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        // 2. 이미지 2개 업로드
        String imageUrl1 = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "image1.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("url");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "image2.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, postId)
        .then()
            .statusCode(200);

        // 3. 썸네일 없이 발행
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("자동 썸네일 테스트", imageTag(imageUrl1), true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("thumbnailUrl", equalTo(imageUrl1)); // 첫 번째 이미지가 썸네일이어야 함
    }

    @Test
    @DisplayName("명시적으로 썸네일을 지정하면 해당 URL이 유지된다")
    void explicitThumbnail_Success() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        String customThumbnail = "https://example.com/custom.png";

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest("수동 썸네일 테스트", "본문", true, customThumbnail))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("thumbnailUrl", equalTo(customThumbnail));
    }

    @Test
    @DisplayName("초안 저장 시 본문 HTML에서 제거된 이미지는 soft delete된다")
    void saveDraft_softDeletesImageRemovedFromContentHtml() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String imageUrl = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "used.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("url");

        String removedFileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "removed.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID removedFileId = UUID.fromString(removedFileIdStr);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new SaveDraftRequest("미사용 이미지 정리 테스트", imageTag(imageUrl), true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/draft", noticeChannelId, postId)
        .then()
            .statusCode(200);

        File removedFile = fileRepository.findById(removedFileId).orElseThrow();
        assertThat(removedFile.isDeleted()).isTrue();
        assertThat(removedFile.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("등록된 Google Drive 파일을 fileId로 게시글 첨부파일에 연동할 수 있다")
    void attachRegisteredDriveFile_Success() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        String driveUrl = "https://drive.google.com/file/d/post-drive-file-123/view?usp=sharing";

        String fileId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", driveUrl,
                "originalName", "자료실 첨부.pdf",
                "mimeType", "application/pdf",
                "fileSize", 1024
            ))
        .when()
            .post("/api/v1/files/drive")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("fileId");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", fileId, "sortOrder", 3))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("fileId", equalTo(fileId))
            .body("isGoogleDrive", equalTo(true))
            .body("url", equalTo(driveUrl));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .body("attachments.fileId", hasItem(fileId))
            .body("attachments.isGoogleDrive", hasItem(true))
            .body("attachments.downloadUrl", hasItem(driveUrl))
            .body("attachments.sortOrder", hasItem(3));
    }

    @Test
    @DisplayName("채널을 읽을 수 있는 봉사자는 게시글 Drive 첨부파일 다운로드 URL을 조회할 수 있다")
    void getAttachmentDownloadUrl_PostAttachmentReadableChannel_Success() {
        String volunteer = "post-download-volunteer";
        userTestHelper.createTestUser(volunteer, RoleType.VOLUNTEER);
        String volunteerToken = userTestHelper.generateAccessTokenByUserKey(volunteer);
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        String driveUrl = "https://drive.google.com/file/d/readable-drive-file/view?usp=sharing";
        String fileId = registerDriveFile(driveUrl);
        attachRegisteredFile(noticeChannelId, postId, fileId);
        publishDraft(noticeChannelId, postId, "다운로드 가능한 첨부");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
        .when()
            .get("/api/v1/files/attachments/{fileId}/download-url", fileId)
        .then()
            .statusCode(200)
            .body("downloadUrl", equalTo(driveUrl));
    }

    @Test
    @DisplayName("채널을 읽을 수 없는 봉사자는 게시글 첨부파일 다운로드 URL을 조회할 수 없다")
    void getAttachmentDownloadUrl_PostAttachmentUnreadableChannel_Forbidden() {
        String volunteer = "post-download-blocked-volunteer";
        userTestHelper.createTestUser(volunteer, RoleType.VOLUNTEER);
        String volunteerToken = userTestHelper.generateAccessTokenByUserKey(volunteer);
        Channel closedChannel = channelRepository.save(Channel.builder()
            .name("비공개 자료실")
            .description("다운로드 권한 테스트")
            .channelType(ChannelType.RESOURCE)
            .bindingType(ChannelBindingType.STANDALONE)
            .accessLevel(ChannelAccessLevel.CLOSED)
            .isActive(true)
            .build());
        testChannelHelper.registerChannel(closedChannel.getId());
        Long postId = testPostHelper.createDraftAndRegister(closedChannel.getId(), adminAccessToken);
        String fileId = registerDriveFile("https://drive.google.com/file/d/closed-drive-file/view?usp=sharing");
        attachRegisteredFile(closedChannel.getId(), postId, fileId);
        publishDraft(closedChannel.getId(), postId, "비공개 첨부");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
        .when()
            .get("/api/v1/files/attachments/{fileId}/download-url", fileId)
        .then()
            .statusCode(403);
    }

    private String imageTag(String imageUrl) {
        return "<p>본문</p><img src=\"" + imageUrl + "\" alt=\"image\">";
    }

    private void publishDraft(Long channelId, Long postId, String title) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new PublishPostRequest(title, "<p>" + title + "</p>", true, null))
        .when()
            .put("/api/v1/channels/{channelId}/posts/{postId}/publish", channelId, postId)
        .then()
            .statusCode(200);
    }

    private String registerDriveFile(String driveUrl) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", driveUrl,
                "originalName", "자료실 첨부.pdf",
                "mimeType", "application/pdf",
                "fileSize", 1024
            ))
        .when()
            .post("/api/v1/files/drive")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("fileId");
    }

    private void attachRegisteredFile(Long channelId, Long postId, String fileId) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", fileId, "sortOrder", 0))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/attachments", channelId, postId)
        .then()
            .statusCode(200);
    }
}
