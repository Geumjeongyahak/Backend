package sonmoeum.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sonmoeum.domain.post.entity.Post;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Optional<Post> findByIdAndChannelId(Long id, Long channelId);

    Optional<Post> findFirstByChannelIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(Long channelId);
}
