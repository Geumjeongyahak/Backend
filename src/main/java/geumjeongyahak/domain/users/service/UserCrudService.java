package geumjeongyahak.domain.users.service;

import geumjeongyahak.domain.department.service.DepartmentProxyService;
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
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.DuplicateEmailException;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrudService {
    private final UserRepository userRepository;
    private final DepartmentProxyService departmentProxyService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialService credentialService;
    private final UserProxyService userProxyService;


    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        log.debug("사용자 조회 요청 - ID: {}", userId);
        User user = userProxyService.getById(userId);
        log.debug("사용자 조회 요청 완료 - ID: {}", userId);

        return UserDetailResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserSimpleResponse> getAllUsersPagination(UserPaginationRequest request) {
        log.debug("전체 사용자 목록 조회 요청");
        var pageResponse = new PaginationResponse<>(userRepository.findAll(request.toRequest()));
        log.debug("전체 사용자 목록 조회 완료 - 총 {}명", pageResponse.getTotalElements());
        
        return PaginationResponse.mapTo(pageResponse, UserSimpleResponse::from);
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

        User savedUser = userRepository.save(userBuilder.build());
        
        
        credentialService.createLocalCredential(
            savedUser,
            request.email(),
            request.password()
        );
        log.info("사용자 생성 완료 - ID: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        return UserDetailResponse.from(savedUser);
    }

    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
        log.debug("사용자 수정 요청 - ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.debug("사용자 수정 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                return new UserNotFoundException(userId);
            });
        updateUserInternal(user,
            Optional.ofNullable(request.name()),
            Optional.ofNullable(request.phoneNumber()),
            Optional.ofNullable(request.password()),
            Optional.ofNullable(request.email()),
            Optional.ofNullable(request.role()),
            Optional.ofNullable(request.departmentId())
        );
        log.info("사용자 수정 완료 - ID: {}, email: {}", user.getId(), user.getEmail());
        return UserDetailResponse.from(user);
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
                Optional.empty()  // 본인 수정 시 부서 변경 불가
        );
        log.debug("본인 사용자 수정 완료 - ID: {}, email: {}", user.getId(), user.getEmail());
        return UserDetailResponse.from(user);
    }

    private void updateUserInternal(
            User user,
            Optional<String> name,
            Optional<String> phoneNumber,
            Optional<String> password,
            Optional<String> email,
            Optional<String> role,
            Optional<Long> departmentId
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
    }

    @Transactional
    public void deleteUserById(Long userId) {
        log.debug("사용자 삭제 요청 - ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.debug("사용자 삭제 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
            throw new UserNotFoundException(userId);
        }
        userRepository.deleteById(userId);
        log.info("사용자 삭제 완료 - ID: {}", userId);
    }
}
