package sonmoeum.domain.student.service;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.students.dto.request.CreateStudentRequest;
import sonmoeum.api.v1.students.dto.request.UpdateStudentRequest;
import sonmoeum.api.v1.students.dto.response.StudentResponse;
import sonmoeum.domain.student.entity.Student;
import sonmoeum.domain.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 학생이 존재하지 않습니다."));
        return StudentResponse.from(student);
    }

    public BasePageResponse<StudentResponse> getStudentPagination(BasePageRequest pageRequest) {
        return BasePageResponse.from(
            studentRepository.findAll(pageRequest.toPageRequest())
        ).convertTo(StudentResponse::from);
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        Student student = new Student(
            request.name(),
            request.phoneNumber(),
            request.description()
        );
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 학생이 존재하지 않습니다."));
        
        student.update(request.name(), request.phoneNumber(), request.description());
        
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 학생이 존재하지 않습니다."));
        studentRepository.delete(student);
    }
}
