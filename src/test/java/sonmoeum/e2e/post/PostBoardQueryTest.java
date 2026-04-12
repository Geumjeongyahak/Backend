package sonmoeum.e2e.post;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.enums.ChannelWriterPolicy;
import sonmoeum.domain.post.v1.dto.request.CreatePostRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: 게시판 통합 조회 테스트")
public class PostBoardQueryTest extends BasePostTest {

    @Test
    @DisplayName("게시글 상세 조회 시 조회수가 증가한다")
    void getPost_IncreasesViewCount() {
        Long postId = createPost(noticeChannelId, "조회수 테스트 공지", "NOTICE");

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
                .then()
                .statusCode(200)
                .body("id", equalTo(postId.intValue()))
                .body("viewCount", equalTo(1));

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, postId)
                .then()
                .statusCode(200)
                .body("viewCount", equalTo(2));
    }

    @Test
    @DisplayName("통합 게시판은 공지, 반별, 부서별 필터로 조회할 수 있다")
    void getBoardPosts_WithBoardFilters_Success() {
        Channel classroomChannel = channelRepository.save(Channel.builder()
                .name("겨울반 게시판")
                .slug("classroom-board-" + System.currentTimeMillis())
                .description("반별 게시판")
                .channelType(ChannelType.CLASSROOM)
                .refId(101L)
                .writerPolicy(ChannelWriterPolicy.ALL_AUTHENTICATED)
                .isActive(true)
                .sortOrder(2)
                .build());
        Channel departmentChannel = channelRepository.save(Channel.builder()
                .name("교육연구부 게시판")
                .slug("department-board-" + System.currentTimeMillis())
                .description("부서 게시판")
                .channelType(ChannelType.DEPARTMENT)
                .refId(201L)
                .writerPolicy(ChannelWriterPolicy.ALL_AUTHENTICATED)
                .isActive(true)
                .sortOrder(3)
                .build());

        testChannelHelper.registerChannel(classroomChannel.getId());
        testChannelHelper.registerChannel(departmentChannel.getId());

        createPost(noticeChannelId, "전체 공지", "NOTICE");
        createPost(classroomChannel.getId(), "겨울반 안내", "GENERAL");
        createPost(departmentChannel.getId(), "교육연구부 회의", "GENERAL");

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("postType", "NOTICE")
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("전체 공지"))
                .body("content[0].channelType", equalTo("ALL"));

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("channelType", "CLASSROOM")
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("겨울반 안내"))
                .body("content[0].channelName", equalTo("겨울반 게시판"));

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("departmentId", 201L)
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("교육연구부 회의"))
                .body("content[0].channelType", equalTo("DEPARTMENT"));
    }

    private Long createPost(Long channelId, String title, String postType) {
        CreatePostRequest request = new CreatePostRequest(
                title,
                "<p>" + title + "</p>",
                postType,
                "PUBLISHED",
                false,
                true
        );

        Long postId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/channels/{channelId}/posts", channelId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getLong("id");

        testPostHelper.registerPost(postId);
        return postId;
    }
}
