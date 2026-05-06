package geumjeongyahak.domain.channel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelType;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {
    Optional<Channel> findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType channelType, Long refId);

    @Query("select c.accessLevel from Channel c where c.id = :id and c.isDeleted = false")
    Optional<ChannelAccessLevel> findAccessLevelById(Long id);
}
