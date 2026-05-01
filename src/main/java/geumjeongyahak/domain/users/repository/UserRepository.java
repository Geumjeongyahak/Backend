package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
    java.util.Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByDepartmentId(Long departmentId);
    boolean existsByIdAndDepartmentId(Long userId, Long departmentId);
    java.util.List<User> findAllByDepartmentId(Long departmentId);
    Page<User> findAll(Pageable pageable);
}
