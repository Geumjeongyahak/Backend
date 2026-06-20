package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
    long countByIsDeletedFalse();
    long countByDepartmentIdAndIsDeletedFalse(Long departmentId);
    List<User> findAllByDepartmentIdAndIsDeletedFalse(Long departmentId);
    List<User> findAllByIsDeletedFalse(Sort sort);
    Page<User> findAll(Pageable pageable);

    @Query("""
        select u
        from User u
        left join fetch u.classroom
        where u.isDeleted = false
            and u.teacherStartAt is not null
            and u.teacherStartAt <= :today
            and (u.teacherEndAt is null or u.teacherEndAt >= :today)
        order by u.name asc, u.id asc
        """)
    List<User> findCurrentTeachersWithClassroom(@Param("today") LocalDate today);
}
