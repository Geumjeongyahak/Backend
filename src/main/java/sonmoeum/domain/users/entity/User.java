package sonmoeum.domain.users.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import sonmoeum.domain.auth.entity.UserPermission;
import sonmoeum.domain.auth.enums.PermissionGranterType;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.auth.enums.ProviderType;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    @Setter
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = true, length = 512)
    @Setter
    private String passwordHash;
    
    @Column(length = 20)
    @Setter
    private String phoneNumber;

    @Column(nullable = true, length = 512)
    @Setter
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Setter
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Setter
    private RoleType role = RoleType.VOLUNTEER;

    @OneToMany(
        mappedBy = "user", fetch = FetchType.EAGER
    )
    private Set<UserPermission> userPermissions = new HashSet<>();

    public Set<PermissionType> getPermissions() {
        return userPermissions.stream()
                .map(UserPermission::getPermissionType)
                .collect(Collectors.toSet());
    }

    public void addPermission(PermissionType permissionType, PermissionGranterType granterType) {
        this.userPermissions.add(new UserPermission(this, permissionType, granterType));
    }

    public void removePermission(PermissionType permissionType, PermissionGranterType granterType) {
        this.userPermissions.remove(new UserPermission(this, permissionType, granterType));
    }

    public void setPermissions(Collection<PermissionType> permissionTypes) {
        this.userPermissions.clear();
        for (PermissionType permissionType : permissionTypes) {
            this.userPermissions.add(new UserPermission(this, permissionType, PermissionGranterType.USER));
        }
    }
    
    @Builder(
        builderMethodName = "emailUserBuilder",
        builderClassName = "EmailUserBuilder"
    )
    public static User createEmailUser(
        @NonNull String name,
        @NonNull String email,
        @NonNull String passwordHash,
        RoleType role,
        String phoneNumber,
        Collection<PermissionType> permissions
    ) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role != null ? role : RoleType.VOLUNTEER;
        user.phoneNumber = phoneNumber;
        user.userPermissions = new HashSet<>();
        if (permissions != null) {
            for (PermissionType permissionType : permissions) {
                user.addPermission(permissionType, PermissionGranterType.USER);
            }
        }
        return user;
    }

    public static User createOAuthUser(
        String name,
        String email,
        ProviderType providerType,
        String clientId
    ) {
        User user = new User();
        user.name = name;
        user.email = email;

        // OAuth 유저는 비밀번호 로그인 안 함
        user.passwordHash = "";

        user.providerType = providerType; // 예: GOOGLE
        user.clientId = clientId;         // 예: google "sub"
        user.role = RoleType.VOLUNTEER;   // 기본 역할

        user.userPermissions = new HashSet<>();
        return user;
    }
}
