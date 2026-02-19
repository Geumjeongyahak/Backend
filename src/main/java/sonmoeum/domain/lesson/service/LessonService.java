package sonmoeum.domain.lesson.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.enums.LessonStatus;
import sonmoeum.domain.lesson.enums.TeacherAttendanceStatus;
import sonmoeum.domain.lesson.exception.LessonDuplicateException;
import sonmoeum.domain.lesson.exception.LessonNotFoundException;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.lesson.v1.dto.request.CreateLessonRequest;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;
import sonmoeum.domain.lesson.v1.dto.response.LessonDetailResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonNoteResponse;
import sonmoeum.domain.lesson.v1.dto.response.LessonSummaryResponse;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.exception.SubjectNotFoundException;
import sonmoeum.domain.subject.repository.SubjectRepository;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;


    @Transactional
    public LessonDetailResponse createLesson(
        Long requesterId,
        CreateLessonRequest request
    ) {
        log.debug("수업 생성 요청 (requesterId={}})", requesterId);

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> {
                log.info("수업 생성 실패 - 과목을 찾을 수 없습니다. ID: {}", request.subjectId());
                return new SubjectNotFoundException(request.subjectId());
            });

        User teacher = userRepository.findById(request.teacherId())
            .orElseThrow(() -> {
                log.info("수업 생성 실패 - 교사를 찾을 수 없습니다. ID: {}", request.teacherId());
                return new UserNotFoundException(request.teacherId());
            });

        // 같은 teacher + 같은 date 기준 겹치는 시간이 있는지 확인
        if (lessonRepository.existsByTeacherIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            teacher.getId(),
            request.date(),
            request.startTime(),
            request.endTime()
        )) {
            log.info("수업 생성 실패 - 시간대가 겹치는 수업이 존재합니다.");
            throw new LessonDuplicateException("시간대가 겹치는 수업이 존재합니다.");
        }

        Lesson lesson = new Lesson(
            subject,
            teacher,
            request.date(),
            request.startTime(),
            request.endTime(),
            request.period()
        );

        Lesson saved = lessonRepository.save(lesson);
        log.debug("수업 생성 완료 (lessonId={})", saved.getId());

        return LessonDetailResponse.from(saved);
    }

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

    @Transactional(readOnly = true)
    public LessonNoteResponse getNote(Long teacherId, Long lessonId, boolean isAdmin) {
        log.debug("수업 노트 조회 요청 (lessonId={})", lessonId);
        Lesson lesson = (isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId)
        ).orElseThrow(() -> new LessonNotFoundException(lessonId));

        log.debug("수업 노트 조회 완료 (lessonId={})", lessonId);
        return LessonNoteResponse.from(lesson);
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

    @Transactional
    public LessonDetailResponse updateLessonStatus(
        Long teacherId,
        Long lessonId,
        LessonStatus status,
        boolean isAdmin
    ) {
        log.debug("수업 상태 변경 요청 (lessonId={})", lessonId);
        Lesson lesson = (isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("수업 상태 변경 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });
        lesson.updateStatus(status);
        log.debug("수업 상태 변경 완료 (status={})", status);
        return LessonDetailResponse.from(lesson);
    }

    @Transactional
    public LessonNoteResponse upsertNote(
        Long teacherId,
        Long lessonId,
        String note,
        boolean isAdmin
    ) {
        log.debug("수업 노트 업데이트 요청 (lessonId={})", lessonId);
        Lesson lesson = (isAdmin
            ? lessonRepository.findById(lessonId)
            : lessonRepository.findByIdAndTeacherId(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("수업 노트 업데이트 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });

        lesson.updateNote(note);
        log.debug("수업 노트 업데이트 완료 (lessonId={})", lessonId);
        return LessonNoteResponse.from(lesson);
    }
}
