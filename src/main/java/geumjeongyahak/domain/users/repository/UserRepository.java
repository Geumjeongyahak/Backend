package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.auth.enums.RoleType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>{
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndIsDeletedFalse(Long userId);
    boolean existsByEmail(String email);
    boolean existsByIdAndIsDeletedFalse(Long userId);
    boolean existsByClassroomIdAndIsDeletedFalse(Long classroomId);
    boolean existsByDepartmentIdAndIsDeletedFalse(Long departmentId);
    boolean existsByIdAndDepartmentIdAndIsDeletedFalse(Long userId, Long departmentId);
    boolean existsByDepartmentIdAndRoleAndIsDeletedFalse(Long departmentId, RoleType role);
    boolean existsByDepartmentIdAndRoleAndIsDeletedFalseAndIdNot(
        Long departmentId,
        RoleType role,
        Long userId
    );
    long countByIsDeletedFalse();
    long countByRoleAndIsDeletedFalse(RoleType role);
    long countByDepartmentIdAndIsDeletedFalse(Long departmentId);
    List<User> findAllByDepartmentIdAndIsDeletedFalse(Long departmentId);
    List<User> findAllByDepartmentIdAndRoleAndIsDeletedFalseOrderByIdAsc(
        Long departmentId,
        RoleType role
    );
    List<User> findAllByIsDeletedFalse(Sort sort);
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "classroom")
    List<User> findAllByRoleInAndClassroomIsNotNullAndIsDeletedFalseOrderByNameAscIdAsc(
        Collection<RoleType> roles
    );
}
