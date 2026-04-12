package sonmoeum.e2e.post;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.post.v1.dto.request.CreatePostRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: Channel 하위 Post 리소스 테스트")
public class PostNestedResourceTest extends BasePostTest {

    @Test
    @DisplayName("관리자는 채널 하위 경로로 게시글을 생성하고 조회할 수 있다")
    void createAndReadPostUnderChannel_Success() {
        CreatePostRequest request = new CreatePostRequest(
                "4월 운영 공지",
                "<p>공지 내용입니다.</p>",
                "NOTICE",
                "PUBLISHED",
                true,
                true
        );

        Long postId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("channelId", equalTo(noticeChannelId.intValue()))
                .body("title", equalTo("4월 운영 공지"))
                .body("postType", equalTo("NOTICE"))
                .extract()
                .jsonPath()
                .getLong("id");

        testPostHelper.registerPost(postId);

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
                .then()
                .statusCode(200)
                .body("id", equalTo(postId.intValue()))
                .body("channelId", equalTo(noticeChannelId.intValue()))
                .body("title", equalTo("4월 운영 공지"));
    }

    @Test
    @DisplayName("게스트는 관리자 전용 공지 채널에 게시글을 생성할 수 없다")
    void createPostWithoutPermission_Forbidden() {
        CreatePostRequest request = new CreatePostRequest(
                "권한 없는 공지",
                "<p>권한 없음</p>",
                "NOTICE",
                "PUBLISHED",
                false,
                true
        );

        given()
                .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(403);
    }
}
