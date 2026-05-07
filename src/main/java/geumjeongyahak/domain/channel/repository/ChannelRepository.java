package geumjeongyahak.domain.channel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelType;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {
    Optional<Channel> findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType channelType, Long refId);

    @Query("select c.accessLevel from Channel c where c.id = :id and c.isDeleted = false")
    Optional<ChannelAccessLevel> findAccessLevelById(Long id);

    @Query(value = """
            select
                id,
                name,
                description,
                channel_type as channelType,
                binding_type as bindingType,
                ref_id as refId,
                access_level as accessLevel,
                allow_guest_read as allowGuestRead,
                is_default as isDefault,
                is_active as isActive,
                last_posted_at as lastPostedAt,
                created_at as createdAt
            from channels
            where is_deleted = false
              and channel_type <> 'ALL'
            order by created_at desc, id desc
            """, nativeQuery = true)
    List<AdminChannelProjection> findAdminChannelsWithoutAllType();

    interface AdminChannelProjection {
        Long getId();

        String getName();

        String getDescription();

        String getChannelType();

        String getBindingType();

        Long getRefId();

        String getAccessLevel();

        Boolean getAllowGuestRead();

        Boolean getIsDefault();

        Boolean getIsActive();

        LocalDateTime getLastPostedAt();

        LocalDateTime getCreatedAt();
    }
}
