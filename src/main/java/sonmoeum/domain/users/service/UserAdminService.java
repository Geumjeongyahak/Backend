package sonmoeum.domain.users.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import sonmoeum.domain.base.dto.response.PaginationResponse;
import sonmoeum.domain.users.v1.dto.request.CreateUserRequest;
import sonmoeum.domain.users.v1.dto.request.UpdateUserRequest;
import sonmoeum.domain.users.v1.dto.request.UserPaginationRequest;
import sonmoeum.domain.users.v1.dto.response.UserResponse;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.DuplicateEmailException;
import sonmoeum.domain.users.exception.DuplicateUsernameException;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminService {
    private static final Long BASE_ROLE_LEVEL = 1L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getUserById(Long userId) {
        log.debug("사용자 조회 요청 - ID: {}", userId);
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
    }

    public UserResponse getUserByUsername(String username) {
        log.debug("사용자 조회 요청 - Username: {}", username);
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - Username: {}", username);
                    return new UserNotFoundException(username);
                });
    }

    public PaginationResponse<UserResponse> getAllUsers(
            UserPaginationRequest request
    ) {
        log.debug("전체 사용자 목록 조회 요청");
        var pageResponse = new PaginationResponse<User>(userRepository.findAllBy(request.toRequest()));
        log.debug("전체 사용자 목록 조회 완료 - 총 {}명", pageResponse.getTotalElements());
        return PaginationResponse.mapTo(pageResponse, UserResponse::from);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("사용자 생성 요청 - Username: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            log.warn("사용자 생성 실패 - 중복된 Username: {}", request.username());
            throw new DuplicateUsernameException(request.username());
        }

        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            log.warn("사용자 생성 실패 - 중복된 Email: {}", request.email());
            throw new DuplicateEmailException(request.email());
        }

        RoleType roleType = RoleType.valueOf(request.role());

        User user = User.localBuilder()
                .name(request.name())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .roles(List.of(roleType))
                .build();

        User savedUser = userRepository.save(user);
        log.info("사용자 생성 완료 - ID: {}, Username: {}", savedUser.getId(), savedUser.getUsername());

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("사용자 수정 요청 - ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자 수정 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.email() != null) {
            if (userRepository.existsByEmail(request.email())) {
                log.warn("사용자 수정 실패 - 중복된 Email: {}", request.email());
                throw new DuplicateEmailException(request.email());
            }
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.getRoles().stream()
                    .filter(userRole -> userRole.getRoleType().getLevel() == BASE_ROLE_LEVEL)
                    .findFirst()
                    .ifPresent(userRole -> user.removeRole(userRole.getRoleType()));
            user.addRole(RoleType.valueOf(request.role()));
        }

        log.info("사용자 수정 완료 - ID: {}, Username: {}", user.getId(), user.getUsername());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        log.info("사용자 삭제 요청 - ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("사용자 삭제 실패 - 사용자를 찾을 수 없습니다. ID: {}", userId);
            throw new UserNotFoundException(userId);
        }
        userRepository.deleteById(userId);
        log.info("사용자 삭제 완료 - ID: {}", userId);
    }
}
