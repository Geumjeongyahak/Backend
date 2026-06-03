package geumjeongyahak.domain.users.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.users.enums.PermissionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
    name = "user_permissions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_permission",
            columnNames = {"user_id", "permission_code"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private PermissionSource source = PermissionSource.MANUAL;

    public UserPermission(
        User user, String permissionCode
    ) {
        this(user, permissionCode, PermissionSource.MANUAL);
    }

    public UserPermission(
        User user, String permissionCode, PermissionSource source
    ) {
        this.user = user;
        this.permissionCode = permissionCode;
        this.source = source != null ? source : PermissionSource.MANUAL;
    }

    public boolean isGlobalScope() {
        return permissionCode != null && permissionCode.endsWith(":*");
    }

    public String toAuthorityCode() {
        return permissionCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserPermission that = (UserPermission) obj;
        Long thisUserId = this.user != null ? this.user.getId() : null;
        Long thatUserId = that.user != null ? that.user.getId() : null;
        return Objects.equals(thisUserId, thatUserId)
            && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user != null ? user.getId() : null, permissionCode);
    }
}
