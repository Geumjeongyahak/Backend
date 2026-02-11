package sonmoeum.domain.lesson.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.entity.StudentAttendance;
import sonmoeum.domain.lesson.exception.LessonNotFoundException;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.lesson.repository.StudentAttendanceRepository;
import sonmoeum.domain.lesson.v1.dto.response.StudentAttendanceResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAttendanceService {

    private final LessonRepository lessonRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;

    public List<StudentAttendanceResponse> getStudentAttendances(Long teacherId, Long lessonId, boolean isAdmin) {
        log.debug("학생 출석부 조회 요청 (lessonId={})", lessonId);
        Lesson lesson = (isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("학생 출석부 조회 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });
        List<StudentAttendance> list = studentAttendanceRepository.findAllByLessonId(lesson.getId());
        log.debug("학생 출석부 조회 완료 - 총 {}개", list.size());
        return list.stream()
            .map(StudentAttendanceResponse::from)
            .toList();
    }
}
