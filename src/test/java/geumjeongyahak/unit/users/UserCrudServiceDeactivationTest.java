package geumjeongyahak.unit.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.department.service.DepartmentPermissionProxyService;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestProxyService;
import geumjeongyahak.domain.request.service.AbsenceRequestProxyService;
import geumjeongyahak.domain.request.service.LessonExchangeRequestProxyService;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.teacher_application.service.TeacherApplicationProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.event.UserDeactivatedEvent;
import geumjeongyahak.domain.users.exception.UserDeactivationConflictException;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.service.UserCrudService;
import geumjeongyahak.domain.users.service.UserPermissionService;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserCrudServiceDeactivationTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentProxyService departmentProxyService;
    @Mock
    private ClassroomProxyService classroomProxyService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserCredentialService credentialService;
    @Mock
    private UserProxyService userProxyService;
    @Mock
    private DepartmentPermissionProxyService departmentPermissionProxyService;
    @Mock
    private UserPermissionService userPermissionService;
    @Mock
    private SubjectProxyService subjectProxyService;
    @Mock
    private TeacherApplicationProxyService teacherApplicationProxyService;
    @Mock
    private PurchaseRequestProxyService purchaseRequestProxyService;
    @Mock
    private AbsenceRequestProxyService absenceRequestProxyService;
    @Mock
    private LessonExchangeRequestProxyService lessonExchangeRequestProxyService;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UserCrudService userCrudService;

    @Test
    void deleteUserById_rejectsSelfDeactivation() {
        assertThatThrownBy(() -> userCrudService.deleteUserById(REQUESTER_ID, REQUESTER_ID))
            .isInstanceOf(UserDeactivationConflictException.class)
            .hasMessage("본인 계정은 비활성화할 수 없습니다.");

        verify(userRepository, never()).findByIdAndIsDeletedFalse(REQUESTER_ID);
    }

    @Test
    void deleteUserById_rejectsLastActiveAdmin() {
        User admin = user(RoleType.ADMIN);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(admin));
        given(userRepository.countByRoleAndIsDeletedFalse(RoleType.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userCrudService.deleteUserById(REQUESTER_ID, TARGET_USER_ID))
            .isInstanceOf(UserDeactivationConflictException.class)
            .hasMessage("마지막 활성 관리자 계정은 비활성화할 수 없습니다.");

        assertThat(admin.isDeleted()).isFalse();
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteUserById_rejectsPendingTeacherApplication() {
        User user = user(RoleType.GUEST);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(user));
        given(teacherApplicationProxyService.existsPendingByApplicantId(TARGET_USER_ID))
            .willReturn(true);

        assertPendingRequestConflict(user);
    }

    @Test
    void deleteUserById_rejectsPendingPurchaseRequest() {
        User user = user(RoleType.GUEST);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(user));
        given(purchaseRequestProxyService.existsActiveByRequesterId(TARGET_USER_ID))
            .willReturn(true);

        assertPendingRequestConflict(user);
    }

    @Test
    void deleteUserById_rejectsPendingAbsenceRequest() {
        User user = user(RoleType.GUEST);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(user));
        given(absenceRequestProxyService.existsPendingByRequesterId(TARGET_USER_ID))
            .willReturn(true);

        assertPendingRequestConflict(user);
    }

    @Test
    void deleteUserById_rejectsPendingLessonExchangeRequest() {
        User user = user(RoleType.GUEST);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(user));
        given(lessonExchangeRequestProxyService.existsActiveExchangeByUserId(TARGET_USER_ID))
            .willReturn(true);

        assertPendingRequestConflict(user);
    }

    @Test
    void deleteUserById_deactivatesEligibleUserAndPublishesEvent() {
        User user = user(RoleType.GUEST);
        given(userRepository.findByIdAndIsDeletedFalse(TARGET_USER_ID))
            .willReturn(Optional.of(user));

        userCrudService.deleteUserById(REQUESTER_ID, TARGET_USER_ID);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        ArgumentCaptor<UserDeactivatedEvent> eventCaptor =
            ArgumentCaptor.forClass(UserDeactivatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(TARGET_USER_ID);
    }

    private void assertPendingRequestConflict(User user) {
        assertThatThrownBy(() -> userCrudService.deleteUserById(REQUESTER_ID, TARGET_USER_ID))
            .isInstanceOf(UserDeactivationConflictException.class)
            .hasMessage("처리 중인 신청 또는 요청이 있는 사용자는 비활성화할 수 없습니다.");

        assertThat(user.isDeleted()).isFalse();
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private User user(RoleType role) {
        User user = User.builder()
            .name("비활성화 대상")
            .email("target@test.com")
            .role(role)
            .build();
        ReflectionTestUtils.setField(user, "id", TARGET_USER_ID);
        return user;
    }
}
