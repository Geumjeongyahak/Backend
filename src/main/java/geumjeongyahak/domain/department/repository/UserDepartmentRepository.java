package geumjeongyahak.domain.department.repository;

import geumjeongyahak.domain.department.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {

    boolean existsByDepartmentId(Long deptId);

    boolean existsByUserIdAndDepartmentId(Long userId, Long deptId);

    void deleteByUserIdAndDepartmentId(Long userId, Long departmentId);

    @Query("SELECT ud FROM UserDepartment ud JOIN FETCH ud.user WHERE ud.department.id = :departmentId")
    List<UserDepartment> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT ud FROM UserDepartment ud JOIN FETCH ud.department WHERE ud.user.id = :userId")
    List<UserDepartment> findByUserId(@Param("userId") Long userId);
}
