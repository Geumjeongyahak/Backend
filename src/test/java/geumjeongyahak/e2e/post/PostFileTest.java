package geumjeongyahak.e2e.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import geumjeongyahak.domain.post.v1.dto.request.PublishPostRequest;

@DisplayName("E2E: Post 파일 및 썸네일 테스트")
@ResourceLock("post-e2e-shared-state")
class PostFileTest extends BasePostTest {

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
            .body(new PublishPostRequest("자동 썸네일 테스트", "본문", true, null))
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
}
