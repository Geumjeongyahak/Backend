package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>{
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByDepartmentId(Long departmentId);
    boolean existsByIdAndDepartmentId(Long userId, Long departmentId);
    long countByDepartmentId(Long departmentId);
    List<User> findAllByDepartmentId(Long departmentId);
    Page<User> findAll(Pageable pageable);
}
