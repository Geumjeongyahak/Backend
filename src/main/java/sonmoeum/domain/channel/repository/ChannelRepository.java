package sonmoeum.domain.channel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sonmoeum.domain.channel.entity.Channel;

public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {

    boolean existsBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIdNotAndIsDeletedFalse(String slug, Long id);
}
