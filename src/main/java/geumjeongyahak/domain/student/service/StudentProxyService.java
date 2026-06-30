package geumjeongyahak.domain.student.service;

import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.enums.StudentStatus;
import geumjeongyahak.domain.student.repository.StudentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student 도메인의 Proxy Service.
 * 다른 도메인에서 학생 엔티티에 접근할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class StudentProxyService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<Student> getActiveStudentsByClassroomId(Long classroomId) {
        return studentRepository.findAllByClassroomIdAndStatusAndIsDeletedFalse(classroomId, StudentStatus.ENROLLED);
    }
}
