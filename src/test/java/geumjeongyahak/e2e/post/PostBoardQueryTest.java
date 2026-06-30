package geumjeongyahak.e2e.post;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: 게시판 통합 조회 테스트")
public class PostBoardQueryTest extends BasePostTest {

    @Test
    @DisplayName("게시글 상세 조회 시 조회수가 증가한다")
    void getPost_IncreasesViewCount() {
        Long postId = createPost(noticeChannelId, "조회수 테스트 공지");

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
    @DisplayName("비인증 사용자는 공개 공지와 행사 게시글 상세를 조회할 수 있다")
    void getPost_WithoutAuth_AllowsGuestReadableNoticeAndEventDetails() {
        Long noticePostId = createPost(noticeChannelId, "비로그인 공지 상세");

        Channel eventChannel = channelRepository.save(Channel.builder()
                .name("행사안내")
                .description("비로그인 행사 상세 조회 테스트")
                .channelType(ChannelType.EVENT)
                .bindingType(ChannelBindingType.STANDALONE)
                .accessLevel(ChannelAccessLevel.READ_ONLY)
                .allowGuestRead(true)
                .isActive(true)
                .build());
        testChannelHelper.registerChannel(eventChannel.getId());
        Long eventPostId = createPost(eventChannel.getId(), "비로그인 행사 상세");

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, noticePostId)
                .then()
                .statusCode(200)
                .body("id", equalTo(noticePostId.intValue()))
                .body("title", equalTo("비로그인 공지 상세"))
                .body("viewCount", equalTo(1));

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", eventChannel.getId(), eventPostId)
                .then()
                .statusCode(200)
                .body("id", equalTo(eventPostId.intValue()))
                .body("title", equalTo("비로그인 행사 상세"))
                .body("viewCount", equalTo(1));
    }

    @Test
    @DisplayName("통합 게시판은 공지, 반별, 부서별 필터로 조회할 수 있다")
    void getBoardPosts_WithBoardFilters_Success() {
        Channel classroomChannel = channelRepository.save(Channel.builder()
                .name("겨울반 게시판")
                .description("반별 게시판")
                .channelType(ChannelType.CLASSROOM)
                .bindingType(ChannelBindingType.STANDALONE)
                .refId(101L)
                .accessLevel(ChannelAccessLevel.READ_WRITE)
                .isActive(true)
                .build());
        Channel departmentChannel = channelRepository.save(Channel.builder()
                .name("교육연구부 게시판")
                .description("부서 게시판")
                .channelType(ChannelType.DEPARTMENT)
                .bindingType(ChannelBindingType.STANDALONE)
                .refId(201L)
                .accessLevel(ChannelAccessLevel.READ_WRITE)
                .isActive(true)
                .build());

        testChannelHelper.registerChannel(classroomChannel.getId());
        testChannelHelper.registerChannel(departmentChannel.getId());

        createPost(noticeChannelId, "전체 공지");
        createPost(classroomChannel.getId(), "겨울반 안내");
        createPost(departmentChannel.getId(), "교육연구부 회의");

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("channelType", "NOTICE")
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("전체 공지"))
                .body("content[0].channelType", equalTo("NOTICE"));

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

    @Test
    @DisplayName("비인증 사용자는 닫힌 채널 게시글 목록과 상세를 조회할 수 없다")
    void getBoardPosts_WithoutAuth_RestrictsClosedChannelListAndDetail() {
        Channel closedChannel = channelRepository.save(Channel.builder()
                .name("닫힌 운영 채널")
                .description("목록 노출 테스트")
                .channelType(ChannelType.CUSTOM)
                .bindingType(ChannelBindingType.STANDALONE)
                .accessLevel(ChannelAccessLevel.CLOSED)
                .isActive(true)
                .build());
        testChannelHelper.registerChannel(closedChannel.getId());

        Long postId = createPost(closedChannel.getId(), "닫힌 채널 목록 노출");

        given()
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content.title", not(hasItem("닫힌 채널 목록 노출")));

        given()
                .queryParam("content", "닫힌")
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(403);

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", closedChannel.getId(), postId)
                .then()
                .statusCode(403);

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts", closedChannel.getId())
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("비인증 사용자는 공개 채널의 초안 게시글 상세를 조회할 수 없다")
    void getPost_WithoutAuth_RejectsDraftEvenInGuestReadableChannel() {
        Long draftId = testPostHelper.createDraftAndRegister(noticeChannelId, adminAccessToken);

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(200)
                .body("content.id", not(hasItem(draftId.intValue())));

        given()
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content.id", not(hasItem(draftId.intValue())));

        given()
                .when()
                .get("/api/v1/channels/{channelId}/posts/{postId}", noticeChannelId, draftId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("관리자는 게시글 본문으로 통합 목록을 검색할 수 있다")
    void getBoardPosts_WithContent_AsAdmin_Success() {
        createPost(noticeChannelId, "본문 검색 허용");

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("content", "본문 검색 허용")
                .when()
                .get("/api/v1/posts")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("본문 검색 허용"));
    }

    @Test
    @DisplayName("채널 하위 게시글 목록은 통합 게시판 전용 필터를 바인딩하지 않는다")
    void getChannelPosts_WithBoardScopeFilters_IgnoresBoardOnlyFilters() {
        Long postId = createPost(noticeChannelId, "채널 범위 필터 테스트");

        given()
                .queryParam("channelType", "NOTICE")
                .when()
                .get("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(200)
                .body("content.id", hasItem(postId.intValue()));

        given()
                .queryParam("channelId", noticeChannelId)
                .when()
                .get("/api/v1/channels/{channelId}/posts", noticeChannelId)
                .then()
                .statusCode(200)
                .body("content.id", hasItem(postId.intValue()));
    }

    private Long createPost(Long channelId, String title) {
        CreatePostRequest request = new CreatePostRequest(
                title,
                "<p>" + title + "</p>",
                "PUBLISHED",
                false,
                true,
                null
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
