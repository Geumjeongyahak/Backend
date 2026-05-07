package geumjeongyahak.domain.channel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "channels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseEntity {

    @Setter
    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "binding_type", nullable = false, length = 20)
    private ChannelBindingType bindingType;

    @Setter
    @Column(name = "ref_id")
    private Long refId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private ChannelAccessLevel accessLevel;

    @Setter
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Setter
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Setter
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Setter
    @Column(name = "allow_guest_read", nullable = false)
    private boolean allowGuestRead;


    @Setter
    @Column(name = "last_posted_at")
    private LocalDateTime lastPostedAt;

    @Builder
    public Channel(
            @NonNull String name,
            String description,
            @NonNull ChannelType channelType,
            @NonNull ChannelBindingType bindingType,
            Long refId,
            ChannelAccessLevel accessLevel,
            Boolean allowGuestRead,
            Boolean isDefault,
            Boolean isActive
    ) {
        this.name = name;
        this.description = description;
        this.channelType = channelType;
        this.bindingType = bindingType;
        this.refId = refId;
        this.accessLevel = accessLevel == null ? ChannelAccessLevel.READ_WRITE : accessLevel;
        this.allowGuestRead = allowGuestRead != null && allowGuestRead;
        this.isDefault = isDefault != null && isDefault;
        this.isActive = isActive == null || isActive;
        this.isDeleted = false;
        this.lastPostedAt = null;
    }

    public boolean isDomainLinked() {
        return this.bindingType == ChannelBindingType.DOMAIN_LINKED;
    }
}
