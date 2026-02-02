package sonmoeum.domain.auth.entity;

import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.base.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission extends BaseEntity{
    @Enumerated(EnumType.STRING)
    private PermissionType name;
}
