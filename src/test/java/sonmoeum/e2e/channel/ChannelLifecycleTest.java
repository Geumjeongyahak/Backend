package sonmoeum.e2e.channel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.channel.v1.dto.request.CreateChannelRequest;
import sonmoeum.domain.channel.v1.dto.request.UpdateChannelRequest;

@DisplayName("E2E: Channel 운영 흐름 테스트")
class ChannelLifecycleTest extends BaseChannelTest {

    @Test
    @DisplayName("관리자는 채널 목록을 조건으로 필터링할 수 있다")
    void listChannels_withFilters_returnsMatchingChannelsOnly() {
        Long activeChannelId = createChannel("운영 공지 채널", true);
        Long hiddenChannelId = createChannel("숨김 공지 채널", false);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "운영 공지")
            .queryParam("isActive", true)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("id", hasItems(activeChannelId.intValue()))
            .body("id", not(hasItems(hiddenChannelId.intValue())));
    }

    @Test
    @DisplayName("관리자는 채널을 수정할 수 있다")
    void updateChannel_asAdmin_returnsUpdatedChannel() {
        Long channelId = createChannel("수정 전 채널", true);

        UpdateChannelRequest request = new UpdateChannelRequest(
            "수정 후 채널",
            "updated-channel-" + System.currentTimeMillis(),
            "설명이 수정되었습니다.",
            null,
            null,
            null,
            null,
            "ALL_AUTHENTICATED",
            true,
            null,
            20
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/{id}", channelId)
        .then()
            .statusCode(200)
            .body("id", equalTo(channelId.intValue()))
            .body("name", equalTo("수정 후 채널"))
            .body("description", equalTo("설명이 수정되었습니다."))
            .body("writerPolicy", equalTo("ALL_AUTHENTICATED"))
            .body("isDefault", equalTo(true))
            .body("isActive", equalTo(true));
    }

    @Test
    @DisplayName("관리자는 채널을 숨겼다가 다시 표시할 수 있다")
    void hideAndShowChannel_asAdmin_togglesActiveState() {
        Long channelId = createChannel("토글 채널", true);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .patch("/{id}/hide", channelId)
        .then()
            .statusCode(200)
            .body("id", equalTo(channelId.intValue()))
            .body("isActive", equalTo(false));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("isActive", false)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("id", hasItems(channelId.intValue()));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .patch("/{id}/show", channelId)
        .then()
            .statusCode(200)
            .body("id", equalTo(channelId.intValue()))
            .body("isActive", equalTo(true));
    }

    @Test
    @DisplayName("관리자는 채널을 삭제하면 이후 조회할 수 없다")
    void deleteChannel_asAdmin_makesChannelUnreadable() {
        Long channelId = createChannel("삭제 대상 채널", true);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", channelId)
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", channelId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("게스트는 채널 수정/숨김/삭제를 할 수 없다")
    void manageChannel_asGuest_returns403() {
        Long channelId = createChannel("권한 테스트 채널", true);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateChannelRequest("수정 시도", null, null, null, null, null, null, null, null, null, null))
        .when()
            .put("/{id}", channelId)
        .then()
            .statusCode(403);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .patch("/{id}/hide", channelId)
        .then()
            .statusCode(403);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .delete("/{id}", channelId)
        .then()
            .statusCode(403);
    }

    private Long createChannel(String name, boolean isActive) {
        String slug = "channel-" + System.nanoTime();
        CreateChannelRequest request = new CreateChannelRequest(
            name,
            slug,
            name + " 설명",
            "ALL",
            null,
            null,
            null,
            "ADMIN_MANAGER_ONLY",
            false,
            isActive,
            10
        );

        Long channelId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        testChannelHelper.registerChannel(channelId);
        return channelId;
    }
}
