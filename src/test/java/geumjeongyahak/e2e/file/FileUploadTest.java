package geumjeongyahak.e2e.file;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import geumjeongyahak.domain.file.entity.File;

@DisplayName("E2E: File 업로드 테스트")
public class FileUploadTest extends BaseFileTest {

    @Test
    @DisplayName("인증된 사용자는 프로필 이미지를 업로드할 수 있다")
    void uploadProfileImage_success() throws IOException {
        String uploadedUrl = given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "avatar.jpg", readSampleImage(), "image/jpeg")
        .when()
            .post("/images/profile")
        .then()
            .statusCode(201)
            .body("fileId", notNullValue())
            .body("originalName", equalTo("profile-" + userTestHelper.getUser(TEST_FILE_USER).getId() + ".png"))
            .body("contentType", equalTo("image/png"))
            .body("ext", equalTo("png"))
            .body("url", containsString("/profiles/"))
            .extract()
            .path("url");

        userTestHelper.setUser(TEST_FILE_USER);
        assertThat(userTestHelper.getUser(TEST_FILE_USER).getProfileImageUrl())
            .isEqualTo(uploadedUrl);

        String originalBasePath = io.restassured.RestAssured.basePath;
        io.restassured.RestAssured.basePath = "";
        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .body("profileImageUrl", equalTo(uploadedUrl));
        io.restassured.RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("인증된 사용자는 첨부파일을 업로드하고 다운로드 URL을 조회할 수 있다")
    void uploadAttachment_andGetDownloadUrl_success() {
        UUID fileId = UUID.fromString(
            given()
                .header(AUTH_HEADER, getAuthHeader(userAccessToken))
                .contentType(ContentType.MULTIPART)
                .multiPart("file", "report.pdf", "fake-pdf".getBytes(StandardCharsets.UTF_8), "application/pdf")
            .when()
                .post("/attachments")
            .then()
                .statusCode(201)
                .body("fileId", notNullValue())
                .body("originalName", equalTo("report.pdf"))
                .body("contentType", equalTo("application/pdf"))
                .body("url", containsString("/documents/attachments/"))
                .extract()
                .path("fileId")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
        .when()
            .get("/attachments/{fileId}/download-url", fileId)
        .then()
            .statusCode(200)
            .body("downloadUrl", containsString("/documents/attachments/"))
            .body("downloadUrl", containsString("expires=30"));
    }

    @Test
    @DisplayName("관리자는 사이트 콘텐츠 이미지를 업로드할 수 있다")
    void uploadSiteContentImage_admin_success() throws IOException {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "history.jpg", "fake-image".getBytes(StandardCharsets.UTF_8), "image/jpeg")
        .when()
            .post("/images/site-contents")
        .then()
            .statusCode(201)
            .body("fileId", notNullValue())
            .body("originalName", equalTo("history.jpg"))
            .body("contentType", equalTo("image/jpeg"))
            .body("ext", equalTo("jpg"))
            .body("url", containsString("/site-contents/"));
    }

    @Test
    @DisplayName("봉사자는 사이트 콘텐츠 이미지를 업로드할 수 없다")
    void uploadSiteContentImage_volunteer_forbidden() throws IOException {
        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "history.jpg", "fake-image".getBytes(StandardCharsets.UTF_8), "image/jpeg")
        .when()
            .post("/images/site-contents")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증된 사용자는 Google Drive 파일 메타데이터를 등록하고 다운로드 URL로 Drive 링크를 조회할 수 있다")
    void registerDriveFile_andGetDownloadUrl_success() {
        String driveUrl = "https://drive.google.com/file/d/drive-file-123/view?usp=sharing";

        UUID fileId = UUID.fromString(
            given()
                .header(AUTH_HEADER, getAuthHeader(userAccessToken))
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "driveUrl", driveUrl,
                    "originalName", "자료집.pdf",
                    "mimeType", "application/pdf",
                    "fileSize", 204800
                ))
            .when()
                .post("/drive")
            .then()
                .statusCode(201)
                .body("fileId", notNullValue())
                .body("originalName", equalTo("자료집.pdf"))
                .body("contentType", equalTo("application/pdf"))
                .body("fileSize", equalTo(204800))
                .body("ext", equalTo("pdf"))
                .body("isGoogleDrive", equalTo(true))
                .body("url", equalTo(driveUrl))
                .extract()
                .path("fileId")
        );

        File file = fileRepository.findById(fileId).orElseThrow();
        assertThat(file.getBucket()).isEqualTo(File.GOOGLE_DRIVE_BUCKET);
        assertThat(file.isGoogleDrive()).isTrue();
        assertThat(file.getStorageKey()).isEqualTo("drive-file-123");

        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
        .when()
            .get("/attachments/{fileId}/download-url", fileId)
        .then()
            .statusCode(200)
            .body("downloadUrl", equalTo(driveUrl));
    }

    @Test
    @DisplayName("확장자가 없는 Google Drive 파일명은 drive 확장자로 등록된다")
    void registerDriveFile_withoutExtension_usesDriveExtension() {
        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", "https://docs.google.com/document/d/document-123/edit",
                "originalName", "운영 회의록",
                "mimeType", "application/vnd.google-apps.document"
            ))
        .when()
            .post("/drive")
        .then()
            .statusCode(201)
            .body("originalName", equalTo("운영 회의록"))
            .body("contentType", equalTo("application/vnd.google-apps.document"))
            .body("ext", equalTo("drive"));
    }

    @Test
    @DisplayName("인증 없이 Google Drive 파일 등록을 호출하면 실패한다")
    void registerDriveFile_unauthorized() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", "https://drive.google.com/file/d/drive-file-123/view",
                "originalName", "자료집.pdf"
            ))
        .when()
            .post("/drive")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("게스트는 Google Drive 파일 메타데이터를 등록할 수 없다")
    void registerDriveFile_guest_forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", "https://drive.google.com/file/d/drive-file-guest/view",
                "originalName", "게스트자료.pdf"
            ))
        .when()
            .post("/drive")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("첨부파일 삭제 요청 시 파일 메타데이터가 soft delete 된다")
    void deleteAttachment_softDeleteSuccess() {
        UUID fileId = UUID.fromString(
            given()
                .header(AUTH_HEADER, getAuthHeader(userAccessToken))
                .contentType(ContentType.MULTIPART)
                .multiPart(
                    "file",
                    "notes.txt",
                    "hello file".getBytes(StandardCharsets.UTF_8),
                    "text/plain"
                )
            .when()
                .post("/attachments")
            .then()
                .statusCode(201)
                .extract()
                .path("fileId")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
        .when()
            .delete("/attachments/{fileId}", fileId)
        .then()
            .statusCode(204);

        File deletedFile = fileRepository.findById(fileId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deletedFile.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("허용되지 않은 MIME 타입의 첨부파일 업로드는 실패한다")
    void uploadAttachment_invalidMimeType_badRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart(
                "file",
                "malware.exe",
                "fake-binary".getBytes(StandardCharsets.UTF_8),
                "application/octet-stream"
            )
        .when()
            .post("/attachments")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }

    @Test
    @DisplayName("application-test.yml에 설정된 이미지 최대 용량을 초과하면 업로드에 실패한다")
    void uploadProfileImage_overMaxSize_badRequest() {
        byte[] oversizedImage = new byte[10 * 1024 * 1024 + 1];

        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "too-large.jpg", oversizedImage, "image/jpeg")
        .when()
            .post("/images/profile")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }

    @Test
    @DisplayName("application-test.yml에 설정된 문서 최대 용량을 초과하면 업로드에 실패한다")
    void uploadAttachment_overMaxSize_badRequest() {
        byte[] oversizedDocument = new byte[30 * 1024 * 1024 + 1];

        given()
            .header(AUTH_HEADER, getAuthHeader(userAccessToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "too-large.pdf", oversizedDocument, "application/pdf")
        .when()
            .post("/attachments")
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL002"));
    }

    @Test
    @DisplayName("인증 없이 파일 업로드를 호출하면 실패한다")
    void uploadPostImage_unauthorized() {
        given()
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "image.png", "fake-image".getBytes(StandardCharsets.UTF_8), "image/png")
        .when()
            .post("/images/posts")
        .then()
            .statusCode(401);
    }

    private byte[] readSampleImage() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        }
    }
}
