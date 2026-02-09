package sonmoeum.domain.users.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import sonmoeum.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByUsername(String username);
    Page<User> findAllBy(Pageable pageable);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
