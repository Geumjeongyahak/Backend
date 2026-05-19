package geumjeongyahak.e2e.file;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        given()
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
            .body("url", containsString("/profiles/"));
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
        try (InputStream inputStream = getClass().getResourceAsStream("/images/sample1.jpg")) {
            if (inputStream == null) {
                throw new IOException("sample image resource not found: /images/sample1.jpg");
            }
            return inputStream.readAllBytes();
        }
    }
}
