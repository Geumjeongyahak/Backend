package geumjeongyahak.e2e.channel;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.channel.v1.dto.request.CreateChannelRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: Channel CRUD 테스트")
public class ChannelCrudTest extends BaseChannelTest {

    @Test
    @DisplayName("관리자는 공지 채널을 생성하고 단건 조회할 수 있다")
    void createAndGetChannel_Success() {
        CreateChannelRequest request = new CreateChannelRequest(
                "테스트 공지 채널",
                "공지 테스트용 채널",
                "NOTICE",
                false,
                true,
                "READ_ONLY",
                false
        );

        Long channelId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("테스트 공지 채널"))
                .body("description", equalTo("공지 테스트용 채널"))
                .body("channelType", equalTo("NOTICE"))
                .body("bindingType", equalTo("STANDALONE"))
                .body("accessLevel", equalTo("READ_ONLY"))
                .extract()
                .jsonPath()
                .getLong("id");

        testChannelHelper.registerChannel(channelId);

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .when()
                .get("/{id}", channelId)
                .then()
                .statusCode(200)
                .body("id", equalTo(channelId.intValue()))
                .body("description", equalTo("공지 테스트용 채널"))
                .body("channelType", equalTo("NOTICE"))
                .body("bindingType", equalTo("STANDALONE"))
                .body("accessLevel", equalTo("READ_ONLY"));
    }

    @Test
    @DisplayName("관리자는 자료 채널을 생성할 수 있다")
    void createChannel_ResourceType_Success() {
        CreateChannelRequest request = new CreateChannelRequest(
                "테스트 자료 채널",
                "자료 테스트용 채널",
                "RESOURCE",
                false,
                true,
                "READ_WRITE",
                false
        );

        Long channelId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("channelType", equalTo("RESOURCE"))
                .body("bindingType", equalTo("STANDALONE"))
                .extract()
                .jsonPath()
                .getLong("id");

        testChannelHelper.registerChannel(channelId);
    }

    @Test
    @DisplayName("관리자는 이벤트 채널을 생성할 수 있다")
    void createChannel_EventType_Success() {
        CreateChannelRequest request = new CreateChannelRequest(
                "테스트 이벤트 채널",
                "이벤트 테스트용 채널",
                "EVENT",
                false,
                true,
                "READ_ONLY",
                true
        );

        Long channelId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("channelType", equalTo("EVENT"))
                .body("bindingType", equalTo("STANDALONE"))
                .extract()
                .jsonPath()
                .getLong("id");

        testChannelHelper.registerChannel(channelId);
    }

    @Test
    @DisplayName("관리자는 안내 채널을 생성할 수 있다")
    void createChannel_GuideType_Success() {
        CreateChannelRequest request = new CreateChannelRequest(
                "테스트 안내 채널",
                "안내 테스트용 채널",
                "GUIDE",
                false,
                true,
                "READ_ONLY",
                true
        );

        Long channelId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("channelType", equalTo("GUIDE"))
                .body("bindingType", equalTo("STANDALONE"))
                .extract()
                .jsonPath()
                .getLong("id");

        testChannelHelper.registerChannel(channelId);
    }

    @Test
    @DisplayName("채널 유형을 생략하면 커스텀 채널로 생성된다")
    void createChannel_DefaultCustomType_Success() {
        CreateChannelRequest request = new CreateChannelRequest(
                "테스트 커스텀 채널",
                "커스텀 테스트용 채널",
                false,
                true,
                "READ_WRITE",
                false
        );

        Long channelId = given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("channelType", equalTo("CUSTOM"))
                .body("bindingType", equalTo("STANDALONE"))
                .extract()
                .jsonPath()
                .getLong("id");

        testChannelHelper.registerChannel(channelId);
    }

    @Test
    @DisplayName("관리자는 도메인 연동 채널 유형을 수동 생성할 수 없다")
    void createChannel_DomainLinkedType_BadRequest() {
        CreateChannelRequest request = new CreateChannelRequest(
                "분반 채널 생성 시도",
                "도메인 연동 타입은 수동 생성 불가",
                "CLASSROOM",
                false,
                true,
                "READ_WRITE",
                false
        );

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("detail", equalTo("수동 생성 채널 유형은 NOTICE, EVENT, RESOURCE, GUIDE, CUSTOM만 허용됩니다."));
    }

    @Test
    @DisplayName("관리자는 알 수 없는 채널 유형을 수동 생성할 수 없다")
    void createChannel_UnknownType_BadRequest() {
        CreateChannelRequest request = new CreateChannelRequest(
                "알 수 없는 채널 생성 시도",
                "정의되지 않은 타입은 생성 불가",
                "UNKNOWN",
                false,
                true,
                "READ_WRITE",
                false
        );

        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("detail", equalTo("알 수 없는 채널 유형입니다. 허용 값: NOTICE, EVENT, RESOURCE, GUIDE, CUSTOM"));
    }

    @Test
    @DisplayName("잘못된 sort 형식으로 채널 목록 조회 시 400을 반환한다")
    void getChannels_InvalidSortFormat_BadRequest() {
        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .queryParam("sort", "name")
                .when()
                .get()
                .then()
                .statusCode(400)
                .body("code", anyOf(equalTo("VAL001"), equalTo("VAL002")));
    }

    @Test
    @DisplayName("게스트는 채널을 생성할 수 없다")
    void createChannel_Guest_Forbidden() {
        CreateChannelRequest request = new CreateChannelRequest(
                "게스트 생성 시도",
                "권한 없음",
                false,
                true,
                "READ_WRITE",
                false
        );

        given()
                .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(403);
    }
}
