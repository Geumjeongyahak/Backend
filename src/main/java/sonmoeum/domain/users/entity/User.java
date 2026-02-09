package sonmoeum.domain.users.entity;

import jakarta.persistence.*;
import lombok.*;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.base.entity.BaseEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Setter
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Setter
    @Column(length = 512)
    private String passwordHash;

    @Setter
    @Column(length = 20)
    private String phoneNumber;

    @Setter
    @Column(unique = true, length = 100)
    private String email;

    @Setter
    @Column(unique = true, length = 100)
    private String gmail;

    @Setter
    @Column(length = 512)
    private String clientId;


    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER,
            orphanRemoval = true,cascade = CascadeType.ALL)
    private Set<UserRole> roles = new HashSet<>();

    public void addRole(RoleType roleType) {
        this.roles.add(new UserRole(this, roleType));
    }

    public void removeRole(RoleType roleType) {
        this.roles.remove(new UserRole(this, roleType));
    }

    @Builder(builderMethodName = "localBuilder")
    public static User createLocalUser(
            @NonNull String name,
            @NonNull String username,
            @NonNull String passwordHash,
            String email,
            String phoneNumber,
            Collection<RoleType> roles
    ) {
        User user = new User();
        user.name = name;
        user.username = username;
        user.passwordHash = passwordHash;
        user.phoneNumber = phoneNumber;
        user.email = email;
        if (roles != null) {
            for (RoleType roleType : roles) user.addRole(roleType);
        }
        return user;
    }
}
