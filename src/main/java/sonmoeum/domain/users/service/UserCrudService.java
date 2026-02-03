package sonmoeum.domain.users.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.users.dto.request.CreateEmailUserRequest;
import sonmoeum.api.v1.users.dto.request.UpdateUserRequest;
import sonmoeum.api.v1.users.dto.response.UserResponse;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrudService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));
        return UserResponse.from(user);
    }

    public BasePageResponse<UserResponse> getUserPagination(
        BasePageRequest pageRequest
    ) {
        return BasePageResponse.from(
            userRepository.findAll(pageRequest.toPageRequest())
            ).convertTo(UserResponse::from);
    }   

    public UserResponse createUser(CreateEmailUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateKeyException("사용자의 이메일이 이미 존재합니다.");
        }
        User user = User.emailUserBuilder()
            .name(request.name())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .phoneNumber(request.phoneNumber())
            .role(RoleType.valueOf(request.role()))
            .permissions(request.permissions()
                .stream()
                .map(PermissionType::valueOf)
                .toList()
            )
            .build();
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.role() != null) {
            user.setRole(RoleType.valueOf(request.role()));
        }
        if (request.permissions() != null) {
            user.setPermissions(request.permissions()
                .stream()
                .map(PermissionType::valueOf)
                .toList()
            );
        }
        return UserResponse.from(userRepository.save(user));
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다."));
        userRepository.delete(user);
    }

}
