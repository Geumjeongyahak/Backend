package sonmoeum.domain.department.repository;

import sonmoeum.domain.department.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
}
