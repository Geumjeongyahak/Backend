package geumjeongyahak.domain.users.service.dto;

import geumjeongyahak.domain.department.entity.Department;

public record DepartmentOption(
    Long id,
    String name
) {
    public static DepartmentOption from(Department department) {
        return new DepartmentOption(department.getId(), department.getName());
    }
}
