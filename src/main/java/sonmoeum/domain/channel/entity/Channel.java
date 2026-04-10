package sonmoeum.domain.channel.entity;

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
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.enums.ChannelWriterPolicy;

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
    @Column(nullable = false, length = 100)
    private String slug;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Setter
    @Column(name = "ref_id")
    private Long refId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "writer_policy", nullable = false, length = 50)
    private ChannelWriterPolicy writerPolicy;

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
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Setter
    @Column(name = "last_posted_at")
    private LocalDateTime lastPostedAt;

    @Builder
    public Channel(
            @NonNull String name,
            @NonNull String slug,
            String description,
            @NonNull ChannelType channelType,
            Long refId,
            ChannelWriterPolicy writerPolicy,
            Boolean isDefault,
            Boolean isActive,
            Integer sortOrder
    ) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.channelType = channelType;
        this.refId = refId;
        this.writerPolicy = writerPolicy == null ? ChannelWriterPolicy.ALL_AUTHENTICATED : writerPolicy;
        this.isDefault = isDefault != null && isDefault;
        this.isActive = isActive == null || isActive;
        this.isDeleted = false;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.lastPostedAt = null;
    }
}
