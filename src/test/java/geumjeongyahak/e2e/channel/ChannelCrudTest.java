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
                false,
                true,
                "READ_ONLY"
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
                .body("channelType", equalTo("CUSTOM"))
                .body("managementMode", equalTo("USER_MANAGED"))
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
                .body("channelType", equalTo("CUSTOM"))
                .body("managementMode", equalTo("USER_MANAGED"))
                .body("accessLevel", equalTo("READ_ONLY"));
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
                "READ_WRITE"
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
