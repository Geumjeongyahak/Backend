package geumjeongyahak.domain.lesson.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.entity.StudentAttendance;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.exception.StudentNotEnrolledException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.repository.StudentAttendanceRepository;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateStudentAttendancesRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.StudentAttendanceResponse;

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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
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

    @Transactional
    public List<StudentAttendanceResponse> updateStudentAttendances(
        Long teacherId,
        Long lessonId,
        UpdateStudentAttendancesRequest request,
        boolean isAdmin
    ) {
        log.debug("학생 출석 처리 요청 (lessonId={})", lessonId);
        Lesson lesson = (isAdmin
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("학생 출석 처리 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });

        List<StudentAttendance> attendances = studentAttendanceRepository
            .findAllByLessonId(lesson.getId());
        Map<Long, StudentAttendance> attendanceMap = attendances.stream()
            .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));

        for (var item : request.attendances()) {
            StudentAttendance target = attendanceMap.get(item.studentId());
            if (target == null) {
                log.warn("학생 출석 처리 실패 - 학생을 찾을 수 없습니다. ID: {}", item.studentId());
                throw new StudentNotEnrolledException(item.studentId());
            }

            target.updateStatus(item.status());
            target.updateMemo(item.memo());
        }

        log.debug("학생 출석 처리 완료");
        return attendances.stream()
            .map(StudentAttendanceResponse::from)
            .toList();
    }
}
