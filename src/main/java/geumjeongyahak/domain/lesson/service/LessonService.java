package geumjeongyahak.domain.lesson.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.enums.TeacherAttendanceStatus;
import geumjeongyahak.domain.lesson.exception.InvalidLessonScheduleException;
import geumjeongyahak.domain.lesson.exception.LessonDuplicateException;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.LessonRangeRequest;
import geumjeongyahak.domain.lesson.v1.dto.request.UpdateLessonRequest;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonDetailResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonNoteResponse;
import geumjeongyahak.domain.lesson.v1.dto.response.LessonSummaryResponse;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectNotFoundException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.repository.UserRepository;

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
        log.debug("수업 생성 요청 (requesterId={})", requesterId);

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
        validateTeacherAssignable(teacher);

        // 같은 teacher + 같은 date 기준 겹치는 시간이 있는지 확인
        if (lessonRepository.existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            teacher.getId(),
            request.date(),
            request.endTime(),
            request.startTime()
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
            .findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
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
            .findAllByTeacherIdAndIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId);

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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
        ).orElseThrow(() -> new LessonNotFoundException(lessonId));

        log.debug("수업 노트 조회 완료 (lessonId={})", lessonId);
        return LessonNoteResponse.from(lesson);
    }

    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, UpdateLessonRequest request) {
        log.debug("수업 수정 요청 (lessonId={})", lessonId);
        Lesson lesson = lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            .orElseThrow(() -> {
                log.info("수업 수정 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });

        // 최종 값
        Long newSubjectId = request.subjectId() != null ? request.subjectId() : lesson.getSubject().getId();
        Long newTeacherId = request.teacherId() != null ? request.teacherId() : lesson.getTeacher().getId();
        LocalDate newDate = request.date() != null ? request.date() : lesson.getDate();
        LocalTime newStart = request.startTime() != null ? request.startTime() : lesson.getStartTime();
        LocalTime newEnd = request.endTime() != null ? request.endTime() : lesson.getEndTime();
        Integer newPeriod = request.period() != null ? request.period() : lesson.getPeriod();

        // 시간 유효성 검증
        if (!newStart.isBefore(newEnd)) {
            log.info("수업 수정 실패 - 시작 시간은 종료 시간보다 빨라야 합니다.");
            throw new InvalidLessonScheduleException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }

        // 중복 검사 (merge 기준, 자기 자신 제외)
        boolean overlap = lessonRepository
            .existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                newTeacherId,
                newDate,
                lesson.getId(),
                newEnd,
                newStart
            );

        if (overlap) {
            log.info("수업 수정 실패 - 시간대가 겹치는 수업이 존재합니다.");
            throw new LessonDuplicateException("시간대가 겹치는 수업이 존재합니다.");
        }

        // 연관 엔티티가 바뀌는 경우만 조회
        Subject subject = lesson.getSubject();
        if (!subject.getId().equals(newSubjectId)) {
            subject = subjectRepository.findById(newSubjectId)
                .orElseThrow(() -> {
                    log.info("수업 수정 실패 - 과목을 찾을 수 없습니다. ID: {}", newSubjectId);
                    return new SubjectNotFoundException(newSubjectId);
                });
        }

        User teacher = lesson.getTeacher();
        if (!teacher.getId().equals(newTeacherId)) {
            teacher = userRepository.findById(newTeacherId)
                .orElseThrow(() -> {
                    log.info("수업 수정 실패 - 교사를 찾을 수 없습니다. ID: {}", newTeacherId);
                    return new UserNotFoundException(newTeacherId);
                });
            validateTeacherAssignable(teacher);
        }

        // 변경 반영
        lesson.update(subject, teacher, newDate, newStart, newEnd, newPeriod);
        log.debug("수업 수정 완료 (lessonId={})", lessonId);
        return LessonDetailResponse.from(lesson);
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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
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
            ? lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            : lessonRepository.findByIdAndTeacherIdAndIsDeletedFalse(lessonId, teacherId)
        ).orElseThrow(() -> {
            log.warn("수업 노트 업데이트 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
            return new LessonNotFoundException(lessonId);
        });

        lesson.updateNote(note);
        log.debug("수업 노트 업데이트 완료 (lessonId={})", lessonId);
        return LessonNoteResponse.from(lesson);
    }

    // ── 이벤트 핸들러 전용 내부 메서드 ─────────────────────────────────────────

    /**
     * 과목 생성 이벤트 처리용 - startAt~endAt 사이 dayOfWeek에 해당하는 날짜를 times개 골라 수업을 자동 생성한다.
     * 특정 날짜에 교사 시간 충돌이 있으면 해당 날짜만 스킵하고 계속 진행한다.
     */
    @Transactional
    public void createLessonsFromSubject(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        int times,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        log.debug("과목 수업 자동 생성 (subjectId={}, times={})", subjectId, times);

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new SubjectNotFoundException(subjectId));
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new UserNotFoundException(teacherId));
        validateTeacherAssignable(teacher);

        List<LocalDate> dates = startAt.datesUntil(endAt.plusDays(1))
            .filter(d -> d.getDayOfWeek() == dayOfWeek)
            .limit(times)
            .toList();

        int created = 0;
        for (LocalDate date : dates) {
            boolean conflict = lessonRepository
                .existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                    teacherId, date, endTime, startTime
                );
            if (conflict) {
                log.warn("수업 자동 생성 스킵 - 교사 시간 충돌 (date={}, teacherId={})", date, teacherId);
                continue;
            }
            lessonRepository.save(new Lesson(subject, teacher, date, startTime, endTime, period));
            created++;
        }

        log.debug("수업 자동 생성 완료 (subjectId={}, 생성={}건, 스킵={}건)", subjectId, created, dates.size() - created);
    }

    /**
     * 결석 승인 이벤트 처리용 - 수업 교사 출석 상태를 공결(EXCUSED)로 변경한다.
     */
    @Transactional
    public void applyTeacherExcused(Long lessonId) {
        log.debug("교사 출석 공결 처리 (lessonId={})", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new LessonNotFoundException(lessonId));
        lesson.updateTeacherAttendance(TeacherAttendanceStatus.EXCUSED);
    }

    private void validateTeacherAssignable(User teacher) {
        if (teacher.getRole() != RoleType.VOLUNTEER) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "봉사자 사용자만 교사로 배정할 수 있습니다.");
        }
    }

    /**
     * 수업 교환 제안 수락 이벤트 처리용 - 대상 수업의 담당 교사를 변경한다.
     */
    @Transactional
    public void applyTeacherExchange(Long lessonId, Long newTeacherId) {
        log.debug("담당 교사 교환 처리 (lessonId={}, newTeacherId={})", lessonId, newTeacherId);
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new LessonNotFoundException(lessonId));
        User newTeacher = userRepository.findById(newTeacherId)
            .orElseThrow(() -> new UserNotFoundException(newTeacherId));

        lesson.changeTeacher(newTeacher);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        log.debug("수업 삭제 요청 (lessonId={})", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> {
                log.info("수업 삭제 실패 - 수업을 찾을 수 없습니다. ID: {}", lessonId);
                return new LessonNotFoundException(lessonId);
            });
        if (!lesson.getIsDeleted()) {
            lesson.softDelete();
        }
        log.debug("수업 삭제 완료 (lessonId={})", lessonId);
    }
}
