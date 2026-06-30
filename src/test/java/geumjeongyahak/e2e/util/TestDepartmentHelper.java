package geumjeongyahak.e2e.util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestDepartmentHelper {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final Map<String, Department> departmentCache;

    public TestDepartmentHelper(
            DepartmentRepository departmentRepository,
            UserRepository userRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.departmentCache = new HashMap<>();
    }

    public Department createTestDepartment(String name, String description) {
        return departmentCache.computeIfAbsent(name, key -> {
            Department department = Department.builder()
                    .name(name)
                    .description(description)
                    .build();
            return departmentRepository.save(department);
        });
    }

    public void setDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId).orElseThrow();
        departmentCache.put(department.getName(), department);
    }

    public Department getDepartment(String name) {
        return departmentCache.get(name);
    }

    public void joinDepartment(User user, Department department) {
        user.setDepartment(department);
        userRepository.saveAndFlush(user);
    }

    public void clearAll() {
        if (!departmentCache.isEmpty()) {
            departmentRepository.deleteAll(
                departmentCache.values().stream()
                        .filter(d -> d.getId() != null)
                        .toList()
            );
        }
        departmentCache.clear();
    }
}
