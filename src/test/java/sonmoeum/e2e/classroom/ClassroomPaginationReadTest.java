package sonmoeum.e2e.classroom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.classroom.enums.ClassroomType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: Classroom 페이지네이션 조회 테스트")
public class ClassroomPaginationReadTest extends BaseClassroomTest {

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        createTestClassrooms(15);
    }

    /**
     * 테스트용 교실 다수 생성
     */
    private void createTestClassrooms(int count) {
        for (int i = 1; i <= count; i++) {
            ClassroomType type = (i % 2 == 0) ? ClassroomType.WEEKEND : ClassroomType.WEEKDAY;
            testClassroomHelper.createTestClassroom(
                "Pagination Test Classroom " + i,
                type,
                "페이지네이션 테스트용 교실 " + i
            );
        }
    }

    @Test
    @DisplayName("기본 페이지네이션 조회 성공(200 OK) - 관리자")
    void getAllClassrooms_DefaultPagination_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(0))
            .body("size", greaterThan(0))
            .body("totalElements", greaterThan(0))
            .body("totalPages", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("기본 페이지네이션 조회 성공(200 OK) - 게스트")
    void getAllClassrooms_DefaultPagination_Guest() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(0))
            .body("size", greaterThan(0))
            .body("totalElements", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("페이지 크기 지정 조회 성공(200 OK)")
    void getAllClassrooms_WithPageSize() {
        int pageSize = 5;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("size", pageSize)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", lessThanOrEqualTo(pageSize))
            .body("size", equalTo(pageSize))
            .body("page", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("두 번째 페이지 조회 성공(200 OK)")
    void getAllClassrooms_SecondPage() {
        int pageSize = 5;
        int pageNumber = 1; // 0-based, so 1 is the second page

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", pageNumber)
            .queryParam("size", pageSize)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", equalTo(pageNumber))
            .body("size", equalTo(pageSize))
            .log().all();
    }

    @Test
    @DisplayName("이름 필터 조회 성공(200 OK) - name=Pagination Test Classroom 1")
    void getAllClassrooms_FilterByName() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "Pagination Test Classroom 1")
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("totalElements", greaterThan(0))
            .body("content.name", hasItem(containsString("Pagination Test Classroom 1")))
            .log().all();
    }

    @Test
    @DisplayName("타입 필터 조회 성공(200 OK) - type=WEEKDAY")
    void getAllClassrooms_FilterByType() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("type", "WEEKDAY")
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("totalElements", greaterThan(0))
            // content 배열의 모든 type이 WEEKDAY인지 확인
            .body("content.type", everyItem(is("WEEKDAY")))
            .log().all();
    }

    @Test
    @DisplayName("이름+타입 조합 필터 조회 성공(200 OK) - name + type")
    void getAllClassrooms_FilterByNameAndType() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "Pagination Test Classroom 1")
            .queryParam("type", "WEEKDAY")
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("totalElements", greaterThan(0))
            .body("content.name", everyItem(containsString("Pagination Test Classroom 1")))
            .body("content.type", everyItem(is("WEEKDAY")))
            .log().all();
    }

    @Test
    @DisplayName("정렬 조회 성공(200 OK) - sort by name ASC")
    void getAllClassrooms_SortByName() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("sort", "name,ASC")
            .queryParam("size", 5)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", greaterThan(0))
            .log().all();
    }

    @Test
    @DisplayName("범위를 벗어난 페이지 조회 시 빈 결과 반환(200 OK)")
    void getAllClassrooms_OutOfRangePage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("page", 9999)
            .queryParam("size", 10)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", equalTo(0))
            .body("page", equalTo(9999))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 페이지네이션 조회 실패(401 Unauthorized)")
    void getAllClassrooms_Unauthorized() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get()
        .then()
            .statusCode(401)
            .log().all();
    }
}
