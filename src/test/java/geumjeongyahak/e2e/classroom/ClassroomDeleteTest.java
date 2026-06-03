package geumjeongyahak.e2e.classroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: Classroom 삭제 테스트")
public class ClassroomDeleteTest extends BaseClassroomTest {

    private Long subjectLinkedClassroomId;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupSubjectFixture() {
        if (subjectLinkedClassroomId != null) {
            jdbcTemplate.update("DELETE FROM subjects WHERE class_id = ?", subjectLinkedClassroomId);
        }
    }

    @Test
    @DisplayName("관리자 권한으로 Classroom 삭제 성공(204 No Content)")
    void deleteClassroom_Success_Admin() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom Delete 1",
            ClassroomType.WEEKDAY,
            "관리자 삭제 테스트용 교실"
        );
        Long classroomId = classroom.getId();
        Channel linkedChannel = channelRepository.save(Channel.builder()
            .name("Test Classroom Delete 1")
            .description("삭제 테스트용 분반 연동 채널")
            .channelType(ChannelType.CLASSROOM)
            .bindingType(ChannelBindingType.DOMAIN_LINKED)
            .refId(classroomId)
            .accessLevel(ChannelAccessLevel.READ_WRITE)
            .isActive(true)
            .build());
        testChannelHelper.registerChannel(linkedChannel.getId());

        // When & Then: 관리자가 교실 삭제
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", classroomId)
        .then()
            .statusCode(204)
            .log().all();

        // 삭제 후 조회 시 404 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(404)
            .log().all();

        Channel deactivatedChannel = channelRepository.findById(linkedChannel.getId()).orElseThrow();
        assertThat(deactivatedChannel.isActive()).isFalse();
        assertThat(deactivatedChannel.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("게스트 권한으로 Classroom 삭제 실패(403 Forbidden)")
    void deleteClassroom_Forbidden_Guest() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom Delete 2",
            ClassroomType.WEEKEND,
            "게스트 삭제 테스트용 교실"
        );
        Long classroomId = classroom.getId();

        // When & Then: 게스트가 교실 삭제 시도 (권한 없음)
        given()
            .header(AUTH_HEADER, getAuthHeader(guestAccessToken))
        .when()
            .delete("/{id}", classroomId)
        .then()
            .statusCode(403)
            .log().all();

        // 삭제 실패 후에도 교실이 여전히 존재하는지 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(200)
            .log().all();
    }

    @Test
    @DisplayName("활성 과목이 연결된 Classroom 삭제 실패(409 Conflict)")
    void deleteClassroom_withActiveSubjects_returns409() {
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom Delete With Subject",
            ClassroomType.WEEKDAY,
            "활성 과목 연결 삭제 차단 테스트"
        );
        subjectLinkedClassroomId = classroom.getId();
        jdbcTemplate.update("""
            INSERT INTO subjects (
                class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, NULL, '삭제 차단 과목', DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                    TIME '19:20:00', TIME '20:00:00', 1, NULL, '분반 삭제 차단 테스트', TRUE)
            """, subjectLinkedClassroomId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", subjectLinkedClassroomId)
        .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 Classroom 삭제 실패(404 Not Found)")
    void deleteClassroom_NotFound() {
        // Given: 존재하지 않는 ID
        Long nonExistentId = 99999L;

        // When & Then: 존재하지 않는 교실 삭제 시도
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{id}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 Classroom 삭제 실패(401 Unauthorized)")
    void deleteClassroom_Unauthorized() {
        // Given: 테스트용 교실 생성
        Classroom classroom = testClassroomHelper.createTestClassroom(
            "Test Classroom Delete 3",
            ClassroomType.WEEKDAY,
            "인증 없는 삭제 테스트용 교실"
        );
        Long classroomId = classroom.getId();

        // When & Then: 인증 없이 교실 삭제 시도
        given()
        .when()
            .delete("/{id}", classroomId)
        .then()
            .statusCode(401)
            .log().all();

        // 삭제 실패 후에도 교실이 여전히 존재하는지 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{id}", classroomId)
        .then()
            .statusCode(200)
            .log().all();
    }
}
