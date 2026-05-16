package geumjeongyahak.e2e.notification;

import geumjeongyahak.domain.notification.entity.PushSubscription;
import geumjeongyahak.domain.notification.enums.PushDeviceType;
import geumjeongyahak.domain.users.entity.User;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: Push 구독 API 테스트")
@ResourceLock("notification-e2e-shared-state")
class PushSubscriptionE2ETest extends BaseNotificationTest {

    @Test
    @DisplayName("인증 사용자가 Push 구독을 등록하면 활성 구독 레코드가 생성된다")
    void subscribe_authenticatedUser_createsActiveSubscription() {
        String token = "  fcm-token-web  ";

        Long subscriptionId = given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .body(Map.of(
                "token", token,
                "deviceType", "WEB"
            ))
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        User user = userTestHelper.getUser(TEST_NOTIFICATION_USER);
        PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId).orElseThrow();
        assertThat(subscription.getUser().getId()).isEqualTo(user.getId());
        assertThat(subscription.getToken()).isEqualTo(token.trim());
        assertThat(subscription.getDeviceType()).isEqualTo(PushDeviceType.WEB);
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getSubscribedAt()).isNotNull();
        assertThat(subscription.getUnsubscribedAt()).isNull();
    }

    @Test
    @DisplayName("동일 토큰 재구독 시 기존 레코드를 재활성화하고 사용자와 기기 유형을 갱신한다")
    void subscribe_sameToken_reactivatesExistingSubscription() {
        String token = "fcm-token-reuse";

        Long firstId = subscribe(accessToken, token, "WEB");

        given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
        .when()
            .delete("/{subscriptionId}", firstId)
        .then()
            .statusCode(204);

        Long secondId = subscribe(otherAccessToken, token, "IOS");

        User otherUser = userTestHelper.getUser(TEST_NOTIFICATION_OTHER_USER);
        PushSubscription subscription = pushSubscriptionRepository.findById(firstId).orElseThrow();
        assertThat(secondId).isEqualTo(firstId);
        assertThat(pushSubscriptionRepository.findAll()).hasSize(1);
        assertThat(subscription.getUser().getId()).isEqualTo(otherUser.getId());
        assertThat(subscription.getDeviceType()).isEqualTo(PushDeviceType.IOS);
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getUnsubscribedAt()).isNull();
        assertThat(subscription.getFailureCount()).isZero();
    }

    @Test
    @DisplayName("소유자가 Push 구독을 해지하면 active=false로 전환된다")
    void unsubscribe_owner_deactivatesSubscription() {
        Long subscriptionId = subscribe(accessToken, "fcm-token-delete", "ANDROID");

        given()
            .header(AUTH_HEADER, getAuthHeader(accessToken))
        .when()
            .delete("/{subscriptionId}", subscriptionId)
        .then()
            .statusCode(204);

        PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId).orElseThrow();
        assertThat(subscription.isActive()).isFalse();
        assertThat(subscription.getUnsubscribedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 사용자의 Push 구독 해지는 실패하고 기존 구독은 유지된다")
    void unsubscribe_otherUser_returns404AndKeepsSubscriptionActive() {
        Long subscriptionId = subscribe(accessToken, "fcm-token-owned", "WEB");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherAccessToken))
        .when()
            .delete("/{subscriptionId}", subscriptionId)
        .then()
            .statusCode(404);

        PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId).orElseThrow();
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getUnsubscribedAt()).isNull();
    }

    @Test
    @DisplayName("인증 없이 Push 구독을 등록하면 401을 반환한다")
    void subscribe_withoutAuthentication_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "token", "fcm-token-no-auth",
                "deviceType", "WEB"
            ))
        .when()
            .post()
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("관리자 웹 세션에서 Push 구독을 등록하면 활성 구독 레코드가 생성된다")
    void subscribe_adminWebSession_createsActiveSubscription() {
        Cookies cookies = loginAdmin();

        Long subscriptionId = given()
            .basePath("")
            .cookies(cookies)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "token", "admin-web-fcm-token",
                "deviceType", "WEB"
            ))
        .when()
            .post("/admin/push/subscriptions")
        .then()
            .statusCode(200)
            .extract().jsonPath().getLong("id");

        User admin = userTestHelper.getUser(TEST_NOTIFICATION_ADMIN);
        PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId).orElseThrow();
        assertThat(subscription.getUser().getId()).isEqualTo(admin.getId());
        assertThat(subscription.getToken()).isEqualTo("admin-web-fcm-token");
        assertThat(subscription.getDeviceType()).isEqualTo(PushDeviceType.WEB);
        assertThat(subscription.isActive()).isTrue();
    }

    private Long subscribe(String token, String fcmToken, String deviceType) {
        return given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(token))
            .body(Map.of(
                "token", fcmToken,
                "deviceType", deviceType
            ))
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    private Cookies loginAdmin() {
        return given()
            .basePath("")
            .contentType("application/x-www-form-urlencoded")
            .formParam("username", adminEmail)
            .formParam("password", adminPassword)
            .redirects().follow(false)
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .extract().response().detailedCookies();
    }
}
