package sonmoeum.domain.lesson.service;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.enums.TeacherAttendanceStatus;
import sonmoeum.domain.lesson.exception.LessonNotFoundException;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;
import sonmoeum.domain.lesson.v1.dto.response.LessonDetailResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonSummaryResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;

    public List<LessonSummaryResponse> getAllLessons(LessonRangeRequest request) {
        log.debug("전체 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
        log.debug("전체 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(LessonSummaryResponse::from)
            .toList();
    }

    public List<LessonSummaryResponse> getMyLessons(
        Long userId,
        LessonRangeRequest request
    ) {
        log.debug("내 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByTeacherIdAndDateBetweenOrderByDateAscPeriodAsc(
                userId, request.from(), request.to()
            );
        log.debug("내 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(LessonSummaryResponse::from)
            .toList();
    }

    public LessonDetailResponse getLessonDetail(Long teacherId, Long lessonId, boolean isAdmin) {
        log.debug("수업 상세 조회 요청");
        Optional<Lesson> lessonOpt = isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId);

        return lessonOpt
            .map(LessonDetailResponse::from)
            .orElseThrow(() -> {
                log.warn("수업 상세 조회 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });
    }

    @Transactional
    public LessonDetailResponse updateTeacherAttendance(
        Long teacherId,
        Long lessonId,
        TeacherAttendanceStatus status,
        boolean isAdmin
    ) {
        log.debug("교사 출석 처리 요청 (status={})", status);
        Lesson lesson = (isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("교사 출석 처리 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });
        lesson.updateTeacherAttendance(status);
        log.debug("교사 출석 처리 완료");
        return LessonDetailResponse.from(lesson);
    }
}
