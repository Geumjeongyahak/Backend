package geumjeongyahak.domain.users.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geumjeongyahak.domain.auth.enums.RoleType;

import java.util.Objects;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    public UserRole(User user, RoleType roleType) {
        this.user = user;
        this.roleId = roleType.getId();
    }

    public RoleType getRoleType() {
        return RoleType.findById(roleId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserRole that = (UserRole) obj;

        if (user.getId() == null) return that.user.getId() == null && roleId.equals(that.roleId);
        return user.getId().equals(that.user.getId()) && roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        if (user == null) return Objects.hash(roleId);
        return Objects.hash(user.getId(), roleId);
    }
}
