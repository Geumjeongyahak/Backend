package sonmoeum.e2e.classroom;

import org.junit.jupiter.api.*;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.enums.ClassroomType;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("분반 수정 E2E 테스트")
public class ClassroomUpdateTest extends BaseClassroomTest {

    protected Classroom targetClassroom;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        // 분반 생성
        this.targetClassroom = this.testClassroomHelper.createTestClassroom(
                "수정 테스트 분반",
                ClassroomType.WEEKDAY,
                "분반 수정 E2E 테스트를 위한 분반입니다."
        );
    }


    @Test
    @DisplayName("관리자 권한으로 분반 수정 성공(200 OK)")
    void updateClassroom_Success() {
        List.of(
                Map.of("name", "수정된 분반 이름", "type", "WEEKEND", "description", "수정된 설명"),
                Map.of("name", "다른 이름"),
                Map.of("type", "WEEKDAY"),
                Map.of("description", "다른 설명")
        ).forEach(reqBody -> {
            given()
                    .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                    .contentType("application/json")
                    .body(reqBody)
            .when()
                    .put("/{classId}", targetClassroom.getId())
            .then()
                    .statusCode(200)
                    .body("id", equalTo(targetClassroom.getId().intValue()))
                    .body("name", equalTo(reqBody.getOrDefault("name", targetClassroom.getName())))
                    .body("type", equalTo(reqBody.getOrDefault("type", targetClassroom.getType().name())))
                    .body("description", equalTo(reqBody.getOrDefault("description", targetClassroom.getDescription())));

            // 변경 사항을 반영하기 위해 캐시된 분반 정보 갱신
            targetClassroom = this.testClassroomHelper.refresh(targetClassroom.getId());
            if (reqBody.containsKey("name")) {
                Assertions.assertEquals(reqBody.get("name"), targetClassroom.getName());
            }
            if (reqBody.containsKey("type")) {
                Assertions.assertEquals(reqBody.get("type"), targetClassroom.getType().name());
            }
            if (reqBody.containsKey("description")) {
                Assertions.assertEquals(reqBody.get("description"), targetClassroom.getDescription());
            }

        });
    }

    @Test
    @DisplayName("유효성 검사 실패로 분반 수정 실패(400 Bad Request)")
    void updateClassroom_Fail_InvalidInput() {

        // 유효성 검사 실패 케이스들
        List.of(
                Map.of("name", ""), // 빈 이름
                Map.of("type", "INVALID_TYPE") // 잘못된 타입,
        ).forEach(reqBody -> {
            given()
                    .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                    .contentType("application/json")
                    .body(reqBody)
            .when()
                    .put("/{classId}", targetClassroom.getId())
            .then()
                    .statusCode(400)
                    .body("code", equalTo("VAL001"))
                    .log().body();
        });

        // 빈 요청 본문
        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType("application/json")
                .body(Map.of())
        .when()
                .put("/{classId}", targetClassroom.getId())
        .then()
                .statusCode(400)
                .body("code", equalTo("VAL004"))
                .log().body();
    }

    @Test
    @DisplayName("인증 실패로 분반 수정 실패(401 Unauthorized)")
    void updateClassroom_Fail_Unauthenticated() {
        // 토큰 없음
        given()
                .contentType("application/json")
                .body(Map.of("name", "수정된 분반 이름"))
        .when()
                .put("/{classId}", targetClassroom.getId())
        .then()
                .statusCode(401)
                .body("code", equalTo("AUTH001"))
                .log().body();
    }

    @Test
    @DisplayName("권한 부족으로 분반 수정 실패(403 Forbidden)")
    void updateClassroom_Fail_Forbidden() {
        // 일반 사용자 토큰 사용
        given()
                .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
                .contentType("application/json")
                .body(Map.of("name", "수정된 분반 이름"))
        .when()
                .put("/{classId}", targetClassroom.getId())
        .then()
                .statusCode(403)
                .body("code", equalTo("AUTHZ001"))
                .log().body();
    }

    @Test
    @DisplayName("존재하지 않는 분반 수정 실패(404 Not Found)")
    void updateClassroom_Fail_NotFound() {
        Long nonExistentClassroomId = new Random().nextLong(1000, 2000);
        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType("application/json")
                .body(Map.of("name", "수정된 분반 이름"))
        .when()
                .put("/{classId}", nonExistentClassroomId)
        .then()
                .statusCode(404)
                .body("code", equalTo("RES-03-001"))
                .log().body();
    }

    @Test
    @DisplayName("중복된 분반 이름으로 수정 실패(409 Conflict)")
    void updateClassroom_Fail_DuplicateName() {
        // 이름 충돌용 다른 분반 생성
        String conflictName = "기존 분반 이름(충돌 테스트)";
        this.testClassroomHelper.createTestClassroom(conflictName, ClassroomType.WEEKDAY);

        // targetClassroom을 conflictName으로 수정 시도 → 409
        given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType("application/json")
                .body(Map.of("name", conflictName))
        .when()
                .put("/{classId}", targetClassroom.getId())
        .then()
                .statusCode(409)
                .body("code", equalTo("BIZ-03-001"))
                .log().body();
    }

}
