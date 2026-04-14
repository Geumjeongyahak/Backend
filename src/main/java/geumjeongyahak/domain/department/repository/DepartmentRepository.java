package geumjeongyahak.domain.department.repository;

import geumjeongyahak.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
}
