package geumjeongyahak.e2e.sitecontent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import geumjeongyahak.domain.sitecontent.enums.SiteContentGroup;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: SiteContent 공개 조회 테스트")
class SiteContentReadTest extends SiteContentBaseTest {

    @Test
    @DisplayName("인증 없이 연혁 정보 조회 성공")
    void getHistories_WithoutAuth_Success() {
        SiteHistory history = SiteHistory.builder()
            .title("금정열린배움터 시작")
            .detail("첫 수업을 시작했습니다.")
            .linkLabel("소개")
            .linkHref("https://example.com/history")
            .sortOrder(1)
            .build();
        history.replacePhotos(java.util.List.of(
            new SiteHistory.PhotoValue(null, "https://example.com/photo.jpg", "첫 수업 사진")
        ));
        siteHistoryRepository.save(history);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history", hasSize(1))
            .body("history[0].title", equalTo("금정열린배움터 시작"))
            .body("history[0].detail", equalTo("첫 수업을 시작했습니다."))
            .body("history[0].linkLabel", equalTo("소개"))
            .body("history[0].linkHref", equalTo("https://example.com/history"))
            .body("history[0].photos", hasSize(1))
            .body("history[0].photos[0].src", equalTo("https://example.com/photo.jpg"))
            .body("history[0].photos[0].alt", equalTo("첫 수업 사진"));
    }

    @Test
    @DisplayName("연혁은 정렬 순서대로 반환하고 사진이 없으면 빈 배열로 반환")
    void getHistories_SortedAndEmptyPhotos_Success() {
        SiteHistory second = SiteHistory.builder()
            .title("두 번째")
            .sortOrder(2)
            .build();
        siteHistoryRepository.save(second);

        SiteHistory first = SiteHistory.builder()
            .title("첫 번째")
            .sortOrder(1)
            .build();
        siteHistoryRepository.save(first);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history", hasSize(2))
            .body("history[0].title", equalTo("첫 번째"))
            .body("history[0].detail", nullValue())
            .body("history[0].linkLabel", nullValue())
            .body("history[0].linkHref", nullValue())
            .body("history[0].photos", hasSize(0))
            .body("history[1].title", equalTo("두 번째"));
    }

    @Test
    @DisplayName("인증 없이 기관 부서 정보 조회 성공")
    void getDepartmentInfos_WithoutAuth_Success() {
        SiteContent principal = SiteContent.builder()
            .contentType(SiteContentType.PRINCIPAL)
            .title("교장")
            .name("정해웅")
            .sortOrder(1)
            .build();
        principal.replaceItems(
            List.of("금정열린배움터의 전반적인 운영을 총괄")
        );
        siteContentRepository.save(principal);

        SiteContent department = SiteContent.builder()
            .contentType(SiteContentType.DEPARTMENT)
            .title("교육연구부")
            .sortOrder(2)
            .build();
        department.replaceItems(java.util.List.of("신입 선생님 면접", "각종 일지 관리"));
        siteContentRepository.save(department);

        given()
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("history", hasSize(0));

        given()
        .when()
            .get("/departments")
        .then()
            .statusCode(200)
            .body("principal", notNullValue())
            .body("principal.title", equalTo("교장"))
            .body("principal.name", equalTo("정해웅"))
            .body("principal.contentType", nullValue())
            .body("principal.refId", nullValue())
            .body("principal.responsibilities", hasSize(1))
            .body("departments", hasSize(1))
            .body("departments[0].title", equalTo("교육연구부"))
            .body("departments[0].contentType", nullValue())
            .body("departments[0].refId", nullValue())
            .body("departments[0].responsibilities", hasSize(2));
    }

    @Test
    @DisplayName("부서 정보는 교장과 부서를 분리하고 부서는 정렬 순서대로 반환")
    void getDepartmentInfos_SeparatePrincipalAndSortDepartments_Success() {
        SiteContent department2 = SiteContent.builder()
            .contentType(SiteContentType.DEPARTMENT)
            .title("두 번째 부서")
            .sortOrder(2)
            .build();
        siteContentRepository.save(department2);

        SiteContent principal = SiteContent.builder()
            .contentType(SiteContentType.PRINCIPAL)
            .title("교장")
            .name("정해웅")
            .sortOrder(99)
            .build();
        siteContentRepository.save(principal);

        SiteContent department1 = SiteContent.builder()
            .contentType(SiteContentType.DEPARTMENT)
            .title("첫 번째 부서")
            .sortOrder(1)
            .build();
        siteContentRepository.save(department1);

        given()
        .when()
            .get("/departments")
        .then()
            .statusCode(200)
            .body("principal.title", equalTo("교장"))
            .body("departments", hasSize(2))
            .body("departments[0].title", equalTo("첫 번째 부서"))
            .body("departments[1].title", equalTo("두 번째 부서"));
    }

    @Test
    @DisplayName("인증 없이 기관 반 정보 조회 성공")
    void getClassInfos_WithoutAuth_Success() {
        SiteContent weekday = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title("벚꽃반")
            .group(SiteContentGroup.WEEKDAY)
            .sortOrder(1)
            .build();
        weekday.replaceItems(java.util.List.of("평일 기초 학습반"));
        siteContentRepository.save(weekday);

        SiteContent weekendMorning = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title("주말 오전반")
            .group(SiteContentGroup.WEEKEND_MORNING)
            .sortOrder(1)
            .build();
        weekendMorning.replaceItems(java.util.List.of("주말 오전 운영"));
        siteContentRepository.save(weekendMorning);

        given()
        .when()
            .get("/classes")
        .then()
            .statusCode(200)
            .body("weekday", hasSize(1))
            .body("weekday[0].name", equalTo("벚꽃반"))
            .body("weekday[0].description", hasSize(1))
            .body("weekendMorning", hasSize(1))
            .body("weekendMorning[0].name", equalTo("주말 오전반"))
            .body("weekendAfternoon", hasSize(0));
    }

    @Test
    @DisplayName("반 정보는 그룹별로 분리하고 각 그룹 안에서 정렬 순서대로 반환")
    void getClassInfos_GroupedAndSorted_Success() {
        SiteContent weekday2 = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title("주중 두 번째")
            .group(SiteContentGroup.WEEKDAY)
            .sortOrder(2)
            .build();
        siteContentRepository.save(weekday2);

        SiteContent weekendAfternoon = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title("주말 오후")
            .group(SiteContentGroup.WEEKEND_AFTERNOON)
            .sortOrder(1)
            .build();
        siteContentRepository.save(weekendAfternoon);

        SiteContent weekday1 = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title("주중 첫 번째")
            .group(SiteContentGroup.WEEKDAY)
            .sortOrder(1)
            .build();
        siteContentRepository.save(weekday1);

        given()
        .when()
            .get("/classes")
        .then()
            .statusCode(200)
            .body("weekday", hasSize(2))
            .body("weekday[0].name", equalTo("주중 첫 번째"))
            .body("weekday[0].contentType", nullValue())
            .body("weekday[0].refId", nullValue())
            .body("weekday[1].name", equalTo("주중 두 번째"))
            .body("weekendMorning", hasSize(0))
            .body("weekendAfternoon", hasSize(1))
            .body("weekendAfternoon[0].name", equalTo("주말 오후"));
    }

    @Test
    @DisplayName("데이터가 없어도 배열 필드는 빈 배열로 반환")
    void getInfos_EmptyArrays_Success() {
        given()
        .when()
            .get("/departments")
        .then()
            .statusCode(200)
            .body("principal.id", equalTo(0))
            .body("principal.title", equalTo("교장"))
            .body("principal.name", equalTo(""))
            .body("principal.responsibilities", hasSize(0))
            .body("departments", hasSize(0));

        given()
        .when()
            .get("/classes")
        .then()
            .statusCode(200)
            .body("weekday", hasSize(0))
            .body("weekendMorning", hasSize(0))
            .body("weekendAfternoon", hasSize(0));
    }
}
