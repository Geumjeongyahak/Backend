package geumjeongyahak.e2e.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.service.HomeroomAssignmentService;
import geumjeongyahak.e2e.BaseE2ETest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("E2E: 담임 업무분장 서비스 테스트")
class HomeroomAssignmentServiceTest extends BaseE2ETest {

    @Autowired
    private HomeroomAssignmentService homeroomAssignmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("담임을 지정하면 기존 부서 업무분장이 해제된다")
    void assign_releasesDepartmentAssignment() {
        User volunteer = createVolunteer("homeroom-with-department@test.com");
        volunteer.setDepartment(departmentRepository.findById(1L).orElseThrow());
        userRepository.saveAndFlush(volunteer);

        homeroomAssignmentService.assign(volunteer.getId(), 1L);

        Map<String, Object> assignment = findAssignment(volunteer.getId());
        assertThat(assignment.get("department_id")).isNull();
        assertThat(((Number) assignment.get("homeroom_classroom_id")).longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("새 담임을 지정하면 기존 담임이 자동 해제된다")
    void assign_replacesExistingHomeroom() {
        User previousHomeroom = createVolunteer("previous-homeroom@test.com");
        User nextHomeroom = createVolunteer("next-homeroom@test.com");
        homeroomAssignmentService.assign(previousHomeroom.getId(), 1L);

        homeroomAssignmentService.assign(nextHomeroom.getId(), 1L);

        assertThat(findAssignment(previousHomeroom.getId()).get("homeroom_classroom_id")).isNull();
        assertThat(((Number) findAssignment(nextHomeroom.getId()).get("homeroom_classroom_id")).longValue())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("한 봉사자를 다른 분반의 담임으로 지정하면 기존 분반에서 이동한다")
    void assign_movesHomeroomToAnotherClassroom() {
        User volunteer = createVolunteer("moving-homeroom@test.com");
        homeroomAssignmentService.assign(volunteer.getId(), 1L);

        homeroomAssignmentService.assign(volunteer.getId(), 2L);

        assertThat(((Number) findAssignment(volunteer.getId()).get("homeroom_classroom_id")).longValue())
            .isEqualTo(2L);
        assertThat(countHomerooms(1L)).isZero();
        assertThat(countHomerooms(2L)).isEqualTo(1);
    }

    @Test
    @DisplayName("부서를 지정하면 기존 담임 업무분장이 해제된다")
    void assignDepartment_releasesHomeroomAssignment() {
        User volunteer = createVolunteer("homeroom-to-department@test.com");
        homeroomAssignmentService.assign(volunteer.getId(), 1L);

        User assignedUser = userRepository.findByIdAndIsDeletedFalse(volunteer.getId()).orElseThrow();
        assignedUser.setDepartment(departmentRepository.findById(1L).orElseThrow());
        userRepository.saveAndFlush(assignedUser);

        Map<String, Object> assignment = findAssignment(volunteer.getId());
        assertThat(((Number) assignment.get("department_id")).longValue()).isEqualTo(1L);
        assertThat(assignment.get("homeroom_classroom_id")).isNull();
    }

    @Test
    @DisplayName("담임 업무분장을 명시적으로 해제할 수 있다")
    void release_clearsHomeroomAssignment() {
        User volunteer = createVolunteer("released-homeroom@test.com");
        homeroomAssignmentService.assign(volunteer.getId(), 1L);

        homeroomAssignmentService.release(volunteer.getId());

        assertThat(findAssignment(volunteer.getId()).get("homeroom_classroom_id")).isNull();
    }

    @Test
    @DisplayName("새 담임 지정에 실패하면 기존 담임이 유지된다")
    void assign_failureKeepsExistingHomeroom() {
        User currentHomeroom = createVolunteer("rollback-homeroom@test.com");
        homeroomAssignmentService.assign(currentHomeroom.getId(), 1L);
        User guest = userTestHelper.createTestUser(
            "guest-homeroom@test.com",
            "Guest Homeroom",
            "password123!",
            RoleType.GUEST
        );

        assertThatThrownBy(() -> homeroomAssignmentService.assign(guest.getId(), 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("봉사자, 매니저 또는 관리자 사용자만");

        assertThat(((Number) findAssignment(currentHomeroom.getId()).get("homeroom_classroom_id")).longValue())
            .isEqualTo(1L);
        assertThat(findAssignment(guest.getId()).get("homeroom_classroom_id")).isNull();
    }

    @Test
    @DisplayName("교원 해제 시 담임 업무분장도 해제된다")
    void releaseTeacherProfile_releasesHomeroomAssignment() {
        User volunteer = createVolunteer("released-teacher-homeroom@test.com");
        homeroomAssignmentService.assign(volunteer.getId(), 1L);

        User assignedUser = userRepository.findByIdAndIsDeletedFalse(volunteer.getId()).orElseThrow();
        assignedUser.releaseTeacherProfile(LocalDate.now());
        userRepository.saveAndFlush(assignedUser);

        assertThat(findAssignment(volunteer.getId()).get("homeroom_classroom_id")).isNull();
    }

    @Test
    @DisplayName("담임 사용자 비활성화 후 같은 분반에 후임 담임을 지정할 수 있다")
    void deactivateHomeroom_allowsReplacement() {
        User previousHomeroom = createVolunteer("deactivated-homeroom@test.com");
        User replacement = createVolunteer("deactivated-homeroom-replacement@test.com");
        homeroomAssignmentService.assign(previousHomeroom.getId(), 1L);

        User assignedUser = userRepository.findByIdAndIsDeletedFalse(previousHomeroom.getId()).orElseThrow();
        assignedUser.softDelete();
        userRepository.saveAndFlush(assignedUser);
        homeroomAssignmentService.assign(replacement.getId(), 1L);

        assertThat(findAssignment(previousHomeroom.getId()).get("homeroom_classroom_id")).isNull();
        assertThat(((Number) findAssignment(replacement.getId()).get("homeroom_classroom_id")).longValue())
            .isEqualTo(1L);
    }

    private User createVolunteer(String email) {
        return userTestHelper.createTestUser(
            email,
            email.substring(0, email.indexOf('@')),
            "password123!",
            RoleType.VOLUNTEER
        );
    }

    private Map<String, Object> findAssignment(Long userId) {
        return jdbcTemplate.queryForMap(
            "SELECT department_id, homeroom_classroom_id FROM users WHERE id = ?",
            userId
        );
    }

    private int countHomerooms(Long classroomId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE homeroom_classroom_id = ? AND is_deleted = FALSE",
            Integer.class,
            classroomId
        );
        return count != null ? count : 0;
    }

}
