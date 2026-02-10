package sonmoeum.domain.student.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.student.entity.Student;
import sonmoeum.domain.student.exception.DuplicateStudentException;
import sonmoeum.domain.student.exception.StudentNotFoundException;
import sonmoeum.domain.student.repository.StudentRepository;
import sonmoeum.domain.student.v1.dto.request.CreateStudentRequest;
import sonmoeum.domain.student.v1.dto.response.StudentResponse;

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

    public StudentResponse getStudentById(Long studentId) {
        log.debug("학생 단건 조회 요청 - ID: {}", studentId);
        return studentRepository.findById(studentId)
            .map(StudentResponse::from)
            .orElseThrow(() -> {
                log.warn("학생을 찾을 수 없습니다 - ID: {}", studentId);
                return new StudentNotFoundException(studentId);
            });
    }
}
