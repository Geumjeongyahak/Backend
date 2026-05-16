package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;

import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.service.FileCleanupScheduler;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.e2e.TestStorageConfig;

@DisplayName("E2E: 파일 삭제 플로우 테스트")
@ResourceLock("post-e2e-shared-state")
class FileDeleteFlowTest extends BasePostTest {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PostFileRepository postFileRepository;

    @Autowired
    private PostAttachmentRepository postAttachmentRepository;

    @Autowired
    private FileCleanupScheduler fileCleanupScheduler;

    @Autowired
    private TestStorageConfig.ControlledStorageService controlledStorageService;

    @AfterEach
    void resetStorage() {
        controlledStorageService.resetFailPaths();
    }

    @Test
    @DisplayName("게시글 삭제 시 해당 게시글에만 연결된 이미지 File이 soft delete된다")
    void deletePost_softDeletesOrphanImageFile() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String fileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "test.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID fileId = UUID.fromString(fileIdStr);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(204);

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.isDeleted()).isTrue();
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글 삭제 시 첨부파일 File도 soft delete된다")
    void deletePost_softDeletesOrphanAttachmentFile() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String fileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "doc.pdf"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/attachments", noticeChannelId, postId)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID fileId = UUID.fromString(fileIdStr);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
        .then()
            .statusCode(204);

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.isDeleted()).isTrue();
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 게시글에서도 참조 중인 File은 soft delete되지 않는다")
    void deletePost_doesNotSoftDelete_sharedFile() {
        Long post1Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        Long post2Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String fileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "shared.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, post1Id)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID fileId = UUID.fromString(fileIdStr);

        // post2에서도 같은 File을 참조하도록 직접 연결
        testFileHelper.linkFileToPost(post2Id, fileId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, post1Id)
        .then()
            .statusCode(204);

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.isDeleted()).isFalse();
        assertThat(file.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("다른 게시글 첨부파일로 참조 중인 이미지 File은 soft delete되지 않는다")
    void deletePost_doesNotSoftDelete_imageReferencedByAttachment() {
        Long post1Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        Long post2Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String fileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartImageRequest("file", "shared-image.png"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/images", noticeChannelId, post1Id)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID fileId = UUID.fromString(fileIdStr);

        testFileHelper.linkAttachmentToPost(post2Id, fileId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, post1Id)
        .then()
            .statusCode(204);

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.isDeleted()).isFalse();
        assertThat(file.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("다른 게시글 이미지로 참조 중인 첨부파일 File은 soft delete되지 않는다")
    void deletePost_doesNotSoftDelete_attachmentReferencedByImage() {
        Long post1Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        Long post2Id = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        String fileIdStr = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .multiPart(testFileHelper.multipartAttachmentRequest("file", "shared-attachment.pdf"))
        .when()
            .post("/api/v1/channels/{channelId}/posts/{postId}/attachments", noticeChannelId, post1Id)
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("fileId");
        UUID fileId = UUID.fromString(fileIdStr);

        testFileHelper.linkFileToPost(post2Id, fileId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, post1Id)
        .then()
            .statusCode(204);

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.isDeleted()).isFalse();
        assertThat(file.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("스케줄러 실행 시 storage 삭제 성공 파일의 PostFile/PostAttachment/File 레코드가 hard delete된다")
    void cleanupScheduler_hardDeletesRecordsOnStorageSuccess() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        UUID fileId = testFileHelper.createOrphanedFile(LocalDateTime.now().minusDays(8));
        testFileHelper.linkFileToPost(postId, fileId);
        testFileHelper.linkAttachmentToPost(postId, fileId);

        fileCleanupScheduler.cleanupDeletedFiles();

        assertThat(fileRepository.findById(fileId)).isEmpty();
        assertThat(postFileRepository.findFileIdsByPostId(postId)).doesNotContain(fileId);
        assertThat(postAttachmentRepository.findFileIdsByPostId(postId)).doesNotContain(fileId);
    }

    @Test
    @DisplayName("스케줄러 실행 시 storage 삭제 실패 파일은 DB 레코드가 유지된다")
    void cleanupScheduler_keepsDbRecordsOnStorageFailure() {
        Long postId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);
        String failPath = "test/orphan/fail-" + UUID.randomUUID() + ".pdf";
        UUID fileId = testFileHelper.createOrphanedFileWithKey(LocalDateTime.now().minusDays(8), failPath);
        testFileHelper.linkAttachmentToPost(postId, fileId);

        controlledStorageService.failDeleteFor(failPath);
        fileCleanupScheduler.cleanupDeletedFiles();

        assertThat(fileRepository.findById(fileId)).isPresent();
        assertThat(postAttachmentRepository.findFileIdsByPostId(postId)).contains(fileId);
    }
}
