package sonmoeum.domain.student.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sonmoeum.domain.base.dto.response.PaginationResponse;
import sonmoeum.domain.student.entity.Student;
import sonmoeum.domain.student.enums.StudentStatus;
import sonmoeum.domain.student.exception.DuplicateStudentException;
import sonmoeum.domain.student.exception.StudentNotFoundException;
import sonmoeum.domain.student.repository.StudentRepository;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;
import sonmoeum.domain.student.v1.dto.request.StudentPaginationRequest;
import sonmoeum.domain.student.v1.dto.request.UpdateStudentRequest;
import sonmoeum.domain.student.v1.dto.response.StudentResponse;
import sonmoeum.domain.users.v1.dto.response.UserResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("학생 등록 요청: - name: {}", request.name());

        if (studentRepository.existsByNameAndPhoneNumber(request.name(), request.phoneNumber())) {
            log.warn("학생 등록 실패 - 이미 등록된 학생입니다. name: {}, phoneNumber: {}",
                request.name(), request.phoneNumber());
            throw new DuplicateStudentException(request.name(), request.phoneNumber());
        }

        Student student = new Student(request.name(), request.phoneNumber(), request.description());

        Student savedStudent = studentRepository.save(student);

        log.info("학생 등록 완료 - ID: {}, name: {}", savedStudent.getId(), savedStudent.getName());
        return StudentResponse.from(savedStudent);
    }

    public PaginationResponse<StudentResponse> getAllStudents(StudentPaginationRequest request) {
        log.debug("학생 목록 조회 요청");
        PageRequest pageable = request.toRequest();
        Page<Student> page = findStudentsByFilter(request, pageable);

        var pageResponse = new PaginationResponse<>(page);
        log.debug("학생 목록 조회 완료 - 총 {}명", pageResponse.getTotalElements());
        return PaginationResponse.mapTo(pageResponse, StudentResponse::from);
    }

    public StudentResponse getStudentById(Long studentId) {
        log.debug("학생 단건 조회 요청 - ID: {}", studentId);
        return studentRepository.findById(studentId)
            .map(StudentResponse::from)
            .orElseThrow(() -> {
                log.warn("학생을 찾을 수 없습니다 - ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });
    }

    @Transactional
    public StudentResponse updateStudent(Long studentId, UpdateStudentRequest request) {
        log.info("학생 수정 요청 - ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> {
                log.warn("학생 수정 실패 - 학생을 찾을 수 없습니다. ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });

        String newName = (request.name() != null) ? request.name() : student.getName();
        String newPhone = (request.phoneNumber() != null) ? request.phoneNumber() : student.getPhoneNumber();

        if (studentRepository.existsByNameAndPhoneNumberAndIdNot(newName, newPhone, studentId)) {
            throw new DuplicateStudentException(newName, newPhone);
        }

        student.update(request.name(), request.phoneNumber(), request.description(), request.status());

        log.info("학생 수정 완료 - ID: {}, name: {}", student.getId(), student.getName());
        return StudentResponse.from(student);
    }

    @Transactional
    public void deleteStudentById(Long studentId) {
        log.info("학생 삭제 요청 - ID: {}", studentId);
        if (!studentRepository.existsById(studentId)) {
            log.warn("학생 삭제 실패 - 학생을 찾을 수 없습니다. ID: {}", studentId);
            throw new StudentNotFoundException(studentId);
        }
        studentRepository.deleteById(studentId);
        log.info("학생 삭제 완료 - ID: {}", studentId);
    }

    private Page<Student> findStudentsByFilter(StudentPaginationRequest request, Pageable pageable) {
        String name = request.getName();
        StudentStatus status = request.getStatus();

        boolean hasName = StringUtils.hasText(name);
        boolean hasStatus = status != null;

        log.info("name={}, status={}, hasName={}, hasStatus={}", name, status, hasName, hasStatus);

        if (hasName && hasStatus) {
            return studentRepository.findAllByNameContainingAndStatus(name, status, pageable);
        }
        if (hasName) {
            return studentRepository.findAllByNameContaining(name, pageable);
        }
        if (hasStatus) {
            return studentRepository.findAllByStatus(status, pageable);
        }
        return studentRepository.findAllBy(pageable);
    }
}
