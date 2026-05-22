package geumjeongyahak.domain.student.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.exception.DuplicateStudentException;
import geumjeongyahak.domain.student.exception.StudentNotFoundException;
import geumjeongyahak.domain.student.repository.StudentRepository;
import geumjeongyahak.domain.student.repository.specification.StudentSpecs;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.request.StudentSearchRequest;
import geumjeongyahak.domain.student.v1.dto.request.UpdateStudentRequest;
import geumjeongyahak.domain.student.v1.dto.response.StudentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private static final String DEFAULT_SORT_PROPERTY = "name";

    private final StudentRepository studentRepository;
    private final ClassroomProxyService classroomProxyService;

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("학생 등록 요청: - name: {}", request.name());

        if (studentRepository.existsByNameAndPhoneNumberAndIsDeletedFalse(request.name(), request.phoneNumber())) {
            log.warn("학생 등록 실패 - 이미 등록된 학생입니다. name: {}, phoneNumber: {}",
                request.name(), request.phoneNumber());
            throw new DuplicateStudentException(request.name(), request.phoneNumber());
        }

        List<Classroom> classrooms = getActiveClassrooms(request.classroomIds());
        Student student = new Student(request.name(), request.phoneNumber(), request.description(), classrooms);

        Student savedStudent = studentRepository.save(student);

        log.info("학생 등록 완료 - ID: {}, name: {}", savedStudent.getId(), savedStudent.getName());
        return StudentResponse.from(savedStudent);
    }

    public List<StudentResponse> getAllStudents(StudentSearchRequest request) {
        log.debug("학생 목록 조회 요청");
        if (request.getClassroomId() != null) {
            classroomProxyService.getActiveById(request.getClassroomId());
        }

        List<Student> students = studentRepository.findAll(
            createSearchSpec(request),
            defaultSort()
        );

        log.debug("학생 목록 조회 완료 - 총 {}명", students.size());
        return students.stream()
            .map(StudentResponse::from)
            .toList();
    }

    public StudentResponse getStudentById(Long studentId) {
        log.debug("학생 단건 조회 요청 - ID: {}", studentId);
        return studentRepository.findByIdAndIsDeletedFalse(studentId)
            .map(StudentResponse::from)
            .orElseThrow(() -> {
                log.warn("학생을 찾을 수 없습니다 - ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });
    }

    @Transactional
    public StudentResponse updateStudent(Long studentId, UpdateStudentRequest request) {
        log.info("학생 수정 요청 - ID: {}", studentId);

        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
            .orElseThrow(() -> {
                log.warn("학생 수정 실패 - 학생을 찾을 수 없습니다. ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });

        String newName = (request.name() != null) ? request.name() : student.getName();
        String newPhone = (request.phoneNumber() != null) ? request.phoneNumber() : student.getPhoneNumber();

        if (studentRepository.existsByNameAndPhoneNumberAndIdNotAndIsDeletedFalse(newName, newPhone, studentId)) {
            throw new DuplicateStudentException(newName, newPhone);
        }

        List<Classroom> classrooms = request.classroomIds() != null
            ? getActiveClassrooms(request.classroomIds())
            : null;

        student.updateInfo(request.name(), request.phoneNumber(), request.description(), request.status());
        if (classrooms != null) {
            student.setClassrooms(classrooms);
        }

        log.info("학생 수정 완료 - ID: {}, name: {}", student.getId(), student.getName());
        return StudentResponse.from(student);
    }

    @Transactional
    public void deleteStudentById(Long studentId) {
        log.info("학생 삭제 요청 - ID: {}", studentId);
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
            .orElseThrow(() -> {
                log.warn("학생 삭제 실패 - 학생을 찾을 수 없습니다. ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });

        student.delete();
        log.info("학생 삭제 완료 - ID: {}", studentId);
    }

    private Specification<Student> createSearchSpec(StudentSearchRequest request) {
        return StudentSpecs.withoutDeleted()
            .and(StudentSpecs.containsName(request.getName()))
            .and(StudentSpecs.hasStatus(request.getStatus()))
            .and(StudentSpecs.hasClassroomId(request.getClassroomId()));
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Direction.ASC, DEFAULT_SORT_PROPERTY);
    }

    private List<Classroom> getActiveClassrooms(List<Long> classroomIds) {
        return classroomIds.stream()
            .distinct()
            .map(classroomProxyService::getActiveById)
            .toList();
    }
}
