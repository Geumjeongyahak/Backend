package geumjeongyahak.domain.student.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService.AdminClassroomRow;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService.ClassroomFilter;
import geumjeongyahak.domain.student.enums.StudentStatus;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.request.StudentSearchRequest;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAdminViewService {

    private final StudentService studentService;
    private final ClassroomAdminViewService classroomAdminViewService;

    public AdminPage<StudentResponse> getStudents(StudentFilter filter) {
        StudentSearchRequest request = new StudentSearchRequest();
        request.setName(filter.name());
        request.setStatus(filter.status());
        request.setClassroomId(filter.classroomId());

        List<StudentResponse> rows = studentService.getAllStudents(request);
        return AdminPage.from(sortStudents(rows, filter.sort()), filter.page(), filter.size());
    }

    private List<StudentResponse> sortStudents(List<StudentResponse> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
                "id", Comparator.comparing(StudentResponse::id),
                "name", Comparator.comparing(StudentResponse::name, Comparator.nullsLast(String::compareToIgnoreCase)),
                "classroomName", Comparator.comparing(StudentResponse::classroomName, Comparator.nullsLast(String::compareToIgnoreCase)),
                "status", Comparator.comparing(StudentResponse::status, Comparator.nullsLast(String::compareToIgnoreCase))
        ), "name,ASC");
    }

    public StudentResponse getStudent(Long studentId) {
        return studentService.getStudentById(studentId);
    }

    @Transactional
    public Long createStudent(String name, String phoneNumber, String description, Long classroomId) {
        return studentService.createStudent(new CreateStudentRequest(
                name,
                phoneNumber,
                description,
                classroomId
        )).id();
    }

    @Transactional
    public void updateStudent(
            Long studentId,
            String name,
            String phoneNumber,
            String description,
            StudentStatus status,
            Long classroomId
    ) {
        studentService.updateStudent(studentId, new UpdateStudentRequest(
                name,
                phoneNumber,
                description,
                status,
                classroomId
        ));
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        studentService.deleteStudentById(studentId);
    }

    public List<ClassroomOption> getClassroomOptions() {
        return classroomAdminViewService.getClassrooms(new ClassroomFilter(null, null, "name,ASC"))
                .stream()
                .map(ClassroomOption::from)
                .toList();
    }

    public StudentStatus[] getStatuses() {
        return StudentStatus.values();
    }

    public record StudentFilter(
            String name,
            StudentStatus status,
            Long classroomId,
            Integer page,
            Integer size,
            String sort
    ) {
    }

    public record ClassroomOption(
            Long id,
            String name
    ) {
        private static ClassroomOption from(AdminClassroomRow classroom) {
            return new ClassroomOption(classroom.id(), classroom.name());
        }
    }
}
