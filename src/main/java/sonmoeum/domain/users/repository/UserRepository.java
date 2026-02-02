package sonmoeum.domain.users.repository;

import java.util.Optional;

import sonmoeum.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    boolean existsByEmail(String email);
}
