package geumjeongyahak.domain.users.repository;

import geumjeongyahak.domain.users.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDepartmentId(Long departmentId);
    boolean existsByIdAndDepartmentId(Long userId, Long departmentId);
    long countByDepartmentId(Long departmentId);
    List<User> findAllByDepartmentId(Long departmentId);
    Page<User> findAll(Pageable pageable);

    @Query("""
        select u
        from User u
        left join fetch u.classroom
        where u.teacherStartAt is not null
            and u.teacherStartAt <= :today
            and (u.teacherEndAt is null or u.teacherEndAt >= :today)
        order by u.name asc, u.id asc
        """)
    List<User> findCurrentTeachersWithClassroom(@Param("today") LocalDate today);
}
