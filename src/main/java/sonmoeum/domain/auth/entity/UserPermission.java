package sonmoeum.domain.auth.entity;

import java.util.Objects;

import sonmoeum.domain.auth.enums.PermissionGranterType;
import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "granter_type", length = 20, nullable = false)
    private PermissionGranterType granterType = PermissionGranterType.USER;

    public PermissionType getPermissionType() {
        return PermissionType.findById(permissionId).orElse(null);
    }

    public UserPermission(User user, PermissionType permissionType, PermissionGranterType granterType) {
        this.user = user;
        this.permissionId = permissionType.getId();
        this.granterType = granterType != null ? granterType : PermissionGranterType.USER;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UserPermission that = (UserPermission) obj;
        
        Long thisUserId = (user != null) ? user.getId() : null;
        Long thatUserId = (that.user != null) ? that.user.getId() : null;
        
        return Objects.equals(thisUserId, thatUserId) && 
               Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        Long userId = (user != null) ? user.getId() : null;
        return Objects.hash(userId, permissionId);
    }
}