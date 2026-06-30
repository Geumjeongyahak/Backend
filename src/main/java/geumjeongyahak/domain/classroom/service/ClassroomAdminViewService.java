package geumjeongyahak.domain.classroom.service;


import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;
import geumjeongyahak.domain.classroom.v1.dto.request.CreateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.request.UpdateClassroomRequest;
import geumjeongyahak.domain.classroom.v1.dto.response.ClassroomDetailResponse;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomAdminViewService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomCrudService classroomCrudService;

    public List<AdminClassroomRow> getClassrooms(ClassroomFilter filter) {
        List<AdminClassroomRow> rows = classroomRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .filter(classroom -> !classroom.isDeleted())
            .filter(classroom -> matchesName(classroom, filter.name()))
            .filter(classroom -> filter.type() == null || classroom.getType() == filter.type())
            .map(AdminClassroomRow::from)
            .toList();

        return AdminSorts.sort(rows, filter.sort(), Map.of(
            "id", Comparator.comparing(AdminClassroomRow::id),
            "name", Comparator.comparing(AdminClassroomRow::name, Comparator.nullsLast(String::compareToIgnoreCase)),
            "type", Comparator.comparing(AdminClassroomRow::type, Comparator.nullsLast(String::compareToIgnoreCase)),
            "createdAt", Comparator.comparing(AdminClassroomRow::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "name,ASC");
    }

    private boolean matchesName(Classroom classroom, String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return contains(classroom.getName(), normalized)
            || contains(classroom.getDescription(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public ClassroomType[] getClassroomTypes() {
        return ClassroomType.values();
    }

    @Transactional
    public Long createClassroom(String name, String type, String description) {
        return classroomCrudService.createClassroom(new CreateClassroomRequest(name, type, description)).id();
    }

    public ClassroomDetailResponse getClassroom(Long classroomId) {
        return classroomCrudService.getClassroomDetail(classroomId);
    }

    @Transactional
    public void updateClassroom(Long classroomId, String name, String type, String description) {
        classroomCrudService.updateClassroom(classroomId, new UpdateClassroomRequest(name, type, description));
    }

    public record AdminClassroomRow(
        Long id,
        String name,
        String type,
        String description,
        LocalDateTime createdAt
    ) {
        private static AdminClassroomRow from(Classroom classroom) {
            return new AdminClassroomRow(
                classroom.getId(),
                classroom.getName(),
                classroom.getType().name(),
                classroom.getDescription(),
                classroom.getCreatedAt()
            );
        }
    }

    public record ClassroomFilter(
        String name,
        ClassroomType type,
        String sort
    ) {
    }
}
