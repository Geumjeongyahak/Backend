package sonmoeum.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 수업 조회 테스트")
public class LessonReadTest extends LessonBaseTest {

    // [전체 수업 목록 조회 테스트]

    @Test
    @DisplayName("관리자 권한으로 전체 수업 목록(기간 조회) 성공(200 OK)")
    void getAllLessons_Success_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
            .when()
            .get()
            .then()
            .statusCode(200)
            // 리스트(배열) 반환 검증
            .body("$", is(notNullValue()))
            .body("size()", is(3))
            // 데이터가 있다면(시드에 따라) 최소 필드 형태 검증
            .body("lessonId", everyItem(notNullValue()))
            .body("date", everyItem(notNullValue()))
            .body("period", everyItem(allOf(notNullValue(), anyOf(is(1), is(2), is(3)))))
            .body("[0].date", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 전체 수업 목록(기간 조회) 성공(200 OK)")
    void getAllLessons_Success_Volunteer() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("$", is(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 전체 수업 목록 조회 실패(401 Unauthorized)")
    void getAllLessons_Unauthorized() {
        given()
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-12-31")
            .when()
            .get()
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("기간 교차 검증 실패(from > to) 시 400 Bad Request")
    void getAllLessons_InvalidRange_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("from", "2026-03-02")
            .queryParam("to", "2026-02-01")
            .when()
            .get()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("기간 범위 초과(예: 31일 초과) 시 400 Bad Request")
    void getAllLessons_RangeTooLarge_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            // 2026-02-01 ~ 2026-03-15 (43일) => 범위 초과
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-03-15")
            .when()
            .get()
            .then()
            .statusCode(400)
            .log().all();
    }

    // [내 수업 목록 조회 테스트]

    @Test
    @DisplayName("내 수업 목록 조회 성공(200 OK) - 로그인 사용자")
    void getMyLessons_Success() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
            .when()
            .get("/my")
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            // 리스트(배열) 반환
            .body("size()", is(2))
            .body("lessonId", everyItem(notNullValue()))
            .body("date", everyItem(notNullValue()))
            .body("period", everyItem(anyOf(is(1), is(2), is(3))))
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(401 Unauthorized) - 인증 없음")
    void getMyLessons_Unauthorized() {
        given()
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-02-28")
            .when()
            .get("/my")
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(400 Bad Request) - from > to")
    void getMyLessons_InvalidRange_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .queryParam("from", "2026-03-02")
            .queryParam("to", "2026-02-01")
            .when()
            .get("/my")
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("내 수업 목록 조회 실패(400 Bad Request) - 기간 범위 초과")
    void getMyLessons_RangeTooLarge_BadRequest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            // (예시) 31일 초과로 요청
            .queryParam("from", "2026-02-01")
            .queryParam("to", "2026-03-15")
            .when()
            .get("/my")
            .then()
            .statusCode(400)
            .log().all();
    }
}
