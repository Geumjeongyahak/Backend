package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateSelfRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPaginationRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserSimpleResponse;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;
import geumjeongyahak.domain.users.v1.dto.response.TeacherAssignmentResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.DuplicateEmailException;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.repository.specification.UserSpecs;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrudService {
    private final UserRepository userRepository;
    private final DepartmentProxyService departmentProxyService;
    private final ClassroomProxyService classroomProxyService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialService credentialService;
    private final UserProxyService userProxyService;
    private final SubjectProxyService subjectProxyService;


    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        log.debug("사용자 조회 요청 - ID: {}", userId);
        User user = userProxyService.getById(userId);
        log.debug("사용자 조회 요청 완료 - ID: {}", userId);

        List<TeacherAssignmentResponse> assignments = getTeacherAssignments(user.getId());
        return UserDetailResponse.from(user, assignments);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserSimpleResponse> getAllUsersPagination(UserPaginationRequest request) {
        log.debug("전체 사용자 목록 조회 요청 - role: {}, name: {}, currentTeacher: {}",
            request.getRole(), request.getName(), request.getCurrentTeacher());

        Specification<User> spec = Specification.allOf();

        if (request.getRole() != null) {
            spec = spec.and(UserSpecs.hasRole(RoleType.valueOf(request.getRole())));
        }
        if (request.getName() != null) {
            spec = spec.and(UserSpecs.containsName(request.getName()));
        }
        if (request.isCurrentTeacherOnly()) {
            spec = spec.and(UserSpecs.isCurrentTeacher(LocalDate.now()));
        }

        var users = userRepository.findAll(spec, request.toRequest());
        Map<Long, List<TeacherAssignmentResponse>> assignmentsByTeacherId =
            getTeacherAssignmentsByTeacherId(users.getContent());

        log.debug("전체 사용자 목록 조회 완료 - 총 {}명", users.getTotalElements());
        return PaginationResponse.from(
            users,
            user -> UserSimpleResponse.from(
                user,
                assignmentsByTeacherId.getOrDefault(user.getId(), List.of())
            )
        );
    }

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        log.debug("사용자 생성 요청 - email: {}", request.email());

        if (userProxyService.existsByEmail(request.email())) {
            log.debug("사용자 생성 실패 - 중복된 Email: {}", request.email());
            throw new DuplicateEmailException(request.email());
        }

        User.UserBuilder userBuilder = User.builder()
            .name(request.name())
            .email(request.email())
            .phoneNumber(request.phoneNumber())
            .role(RoleType.valueOf(request.role()));

        if (request.departmentId() != null) {
            userBuilder.department(departmentProxyService.getById(request.departmentId()));
        }
        if (request.classroomId() != null) {
            userBuilder.classroom(classroomProxyService.getActiveById(request.classroomId()));
        }

        User savedUser = userRepository.save(userBuilder.build());

        credentialService.createLocalCredential(
            savedUser,
            request.email(),
            request.password()
        );
        log.info("사용자 생성 완료 - ID: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        return UserDetailResponse.from(savedUser, List.of());
    }

    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
        log.debug("사용자 수정 요청 - ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.debug("사용자 수정 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                return new UserNotFoundException(userId);
            });
        validateTeacherAssignmentRoleChange(user, request.role());
        updateUserInternal(user,
            Optional.ofNullable(request.name()),
            Optional.ofNullable(request.phoneNumber()),
            Optional.ofNullable(request.password()),
            Optional.ofNullable(request.email()),
            Optional.ofNullable(request.role()),
            Optional.ofNullable(request.departmentId()),
            Optional.ofNullable(request.classroomId())
        );
        log.info("사용자 수정 완료 - ID: {}, email: {}", user.getId(), user.getEmail());
        return UserDetailResponse.from(user, getTeacherAssignments(user.getId()));
    }

    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateSelfRequest request) {
        log.debug("본인 사용자 수정 요청 - ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.debug("본인 사용자 수정 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
        updateUserInternal(user,
                Optional.ofNullable(request.name()),
                Optional.ofNullable(request.phoneNumber()),
                Optional.ofNullable(request.password()),
                Optional.ofNullable(request.email()),
                Optional.empty(), // 본인 수정 시 역할 변경 불가
                Optional.empty(), // 본인 수정 시 부서 변경 불가
                Optional.empty()  // 본인 수정 시 분반 변경 불가
        );
        log.debug("본인 사용자 수정 완료 - ID: {}, email: {}", user.getId(), user.getEmail());
        return UserDetailResponse.from(user, getTeacherAssignments(user.getId()));
    }

    private void updateUserInternal(
            User user,
            Optional<String> name,
            Optional<String> phoneNumber,
            Optional<String> password,
            Optional<String> email,
            Optional<String> role,
            Optional<Long> departmentId,
            Optional<Long> classroomId
    ) {
        name.ifPresent(user::setName);
        phoneNumber.ifPresent(user::setPhoneNumber);
        password.ifPresent(pw -> credentialService.updateLocalPassword(user, passwordEncoder.encode(pw)));
        email.ifPresent(em -> {
            if (em.equals(user.getEmail())) {
                return;
            }
            if (userRepository.existsByEmail(em)) {
                log.warn("사용자 수정 실패 - 중복된 Email: {}", em);
                throw new DuplicateEmailException(em);
            }
            user.setEmail(em);
            credentialService.updateLocalCredentialEmail(user, em);
        });
        role.map(RoleType::valueOf).ifPresent(user::setRole);
        departmentId.ifPresent(deptId -> {
            user.setDepartment(departmentProxyService.getById(deptId));
        });
        classroomId.ifPresent(classroomIdValue -> {
            user.setClassroom(classroomProxyService.getActiveById(classroomIdValue));
        });
    }

    private List<TeacherAssignmentResponse> getTeacherAssignments(Long teacherId) {
        return subjectProxyService.getActiveSubjectsByTeacherId(teacherId).stream()
            .map(TeacherAssignmentResponse::from)
            .toList();
    }

    private Map<Long, List<TeacherAssignmentResponse>> getTeacherAssignmentsByTeacherId(List<User> users) {
        List<Long> userIds = users.stream()
            .map(User::getId)
            .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return subjectProxyService.getActiveSubjectsByTeacherIds(userIds).stream()
            .collect(Collectors.groupingBy(
                subject -> subject.getTeacher().getId(),
                Collectors.mapping(TeacherAssignmentResponse::from, Collectors.toList())
            ));
    }

    private void validateTeacherAssignmentRoleChange(User user, String requestedRole) {
        if (requestedRole == null) {
            return;
        }
        RoleType nextRole = RoleType.valueOf(requestedRole);
        if (isTeacherAssignableRole(nextRole)) {
            return;
        }
        if (subjectProxyService.existsActiveSubjectByTeacherId(user.getId())) {
            throw new BusinessException(
                CommonErrorCode.INVALID_STATE,
                "담당 중인 활성 과목이 있는 사용자는 교사 배정이 불가능한 역할로 변경할 수 없습니다."
            );
        }
    }

    private boolean isTeacherAssignableRole(RoleType role) {
        return role == RoleType.VOLUNTEER || role == RoleType.MANAGER || role == RoleType.ADMIN;
    }

    @Transactional
    public void deleteUserById(Long userId) {
        log.debug("사용자 삭제 요청 - ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.debug("사용자 삭제 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
            throw new UserNotFoundException(userId);
        }
        if (subjectProxyService.existsActiveSubjectByTeacherId(userId)) {
            throw new BusinessException(
                CommonErrorCode.INVALID_STATE,
                "담당 중인 활성 과목이 있는 사용자는 삭제할 수 없습니다."
            );
        }
        userRepository.deleteById(userId);
        log.info("사용자 삭제 완료 - ID: {}", userId);
    }
}
