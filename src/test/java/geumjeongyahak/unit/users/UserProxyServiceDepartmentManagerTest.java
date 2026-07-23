package geumjeongyahak.unit.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.DepartmentManagerConflictException;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserProxyServiceDepartmentManagerTest {

    private static final Long DEPARTMENT_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProxyService userProxyService;

    @Test
    void findActiveManagerByDepartmentId_returnsEmptyWhenManagerDoesNotExist() {
        given(userRepository.findAllByDepartmentIdAndRoleAndIsDeletedFalseOrderByIdAsc(
            DEPARTMENT_ID,
            RoleType.MANAGER
        )).willReturn(List.of());

        assertThat(userProxyService.findActiveManagerByDepartmentId(DEPARTMENT_ID)).isEmpty();
    }

    @Test
    void findActiveManagerByDepartmentId_returnsManagerWhenExactlyOneExists() {
        User manager = manager(1L, "부서장");
        given(userRepository.findAllByDepartmentIdAndRoleAndIsDeletedFalseOrderByIdAsc(
            DEPARTMENT_ID,
            RoleType.MANAGER
        )).willReturn(List.of(manager));

        assertThat(userProxyService.findActiveManagerByDepartmentId(DEPARTMENT_ID))
            .contains(manager);
    }

    @Test
    void findActiveManagerByDepartmentId_throwsWhenMultipleManagersExist() {
        given(userRepository.findAllByDepartmentIdAndRoleAndIsDeletedFalseOrderByIdAsc(
            DEPARTMENT_ID,
            RoleType.MANAGER
        )).willReturn(List.of(manager(1L, "첫 번째 부서장"), manager(2L, "두 번째 부서장")));

        assertThatThrownBy(() -> userProxyService.findActiveManagerByDepartmentId(DEPARTMENT_ID))
            .isInstanceOf(DepartmentManagerConflictException.class)
            .hasMessageContaining("활성 부서장이 여러 명");
    }

    private User manager(Long id, String name) {
        User user = User.builder()
            .name(name)
            .role(RoleType.MANAGER)
            .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
