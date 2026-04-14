package geumjeongyahak.e2e.util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.UserDepartment;
import geumjeongyahak.domain.department.repository.DepartmentRepository;
import geumjeongyahak.domain.department.repository.UserDepartmentRepository;
import geumjeongyahak.domain.users.entity.User;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestDepartmentHelper {
    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final Map<String, Department> departmentCache;
    private final Map<String, UserDepartment> userDepartmentCache;

    public TestDepartmentHelper(
            DepartmentRepository departmentRepository,
            UserDepartmentRepository userDepartmentRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.departmentCache = new HashMap<>();
        this.userDepartmentCache = new HashMap<>();
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
        String key = user.getId() + "_" + department.getId();
        UserDepartment userDepartment = new UserDepartment(user, department);
        UserDepartment saved = userDepartmentRepository.save(userDepartment);
        userDepartmentRepository.flush();  // 즉시 DB에 반영
        userDepartmentCache.put(key, saved);
    }

    public void clearAll() {
        // 캐시된 Department의 모든 UserDepartment 삭제 (API로 참여시킨 것 포함)
        if (!userDepartmentCache.isEmpty()) {
            userDepartmentRepository.deleteAll(
                userDepartmentCache.values().stream()
                        .filter(ud -> ud.getId() != null)
                        .toList()
            );
        }
        // Department 삭제
        if  (!departmentCache.isEmpty()) {
            departmentRepository.deleteAll(
                departmentCache.values().stream()
                        .filter(d -> d.getId() != null)
                        .toList()
            );
        }
    }
}
