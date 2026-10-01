package geumjeongyahak.domain.lesson.service.schedule;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.subject.event.SubjectsCopiedEvent;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 과목 이벤트(생성·교사 배정/해제·일정 변경·삭제)에 따라 그 과목의 수업을 한꺼번에 바꾼다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubjectLessonScheduleService {

    private final LessonRepository lessonRepository;
    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;
    private final LessonGenerator lessonGenerator;
    private final DailyScheduleSyncPublisher syncPublisher;

    /**
     * startAt~endAt 사이 dayOfWeek인 날짜에 수업을 만든다.
     * 교사 시간이 겹치는 날짜만 건너뛴다 ({@link LessonGenerator}).
     */
    public void createLessons(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        User teacher = userProxyService.getById(teacherId);
        teacher.validateCanTeach();

        List<Lesson> created = lessonGenerator.generate(
            subjectProxyService.getById(subjectId), teacher, startAt, endAt, dayOfWeek, startTime, endTime, period
        );
        syncPublisher.publishFor(created);
        log.debug("과목 수업 자동 생성 완료 (subjectId={}, 생성={}건)", subjectId, created.size());
    }

    /** 시간표 복사로 생긴 과목들의 수업을 만들고, 바뀐 (분반, 날짜)는 모두 모아 한 번씩만 동기화한다 (#242). */
    public void createLessonsForAll(List<SubjectsCopiedEvent.CopiedSubject> subjects) {
        List<Lesson> created = subjects.stream()
            .flatMap(copied -> {
                User teacher = userProxyService.getById(copied.teacherId());
                teacher.validateCanTeach();
                return lessonGenerator.generate(
                    subjectProxyService.getById(copied.subjectId()),
                    teacher,
                    copied.startAt(),
                    copied.endAt(),
                    copied.dayOfWeek(),
                    copied.startTime(),
                    copied.endTime(),
                    copied.period()
                ).stream();
            })
            .toList();
        syncPublisher.publishFor(created);
        log.debug("복사 과목 수업 생성 완료 (과목={}건, 수업={}건)", subjects.size(), created.size());
    }

    public void assignTeacher(Long subjectId, Long teacherId, LocalDate from) {
        User teacher = userProxyService.getById(teacherId);
        teacher.validateCanTeach();
        changeFutureScheduledLessons(subjectId, from, lesson -> lesson.changeTeacher(teacher));
    }

    public void changeTime(Long subjectId, LocalDate from, LocalTime startTime, LocalTime endTime, Integer period) {
        changeFutureScheduledLessons(subjectId, from, lesson -> lesson.changeSchedule(startTime, endTime, period));
    }

    public void deleteFutureLessons(Long subjectId, LocalDate from) {
        changeFutureScheduledLessons(subjectId, from, Lesson::softDelete);
    }

    /** from 이후 예정 수업을 지우고, 담당 교사가 있으면 새 기간으로 다시 만든다. */
    public void recreateLessons(
        Long subjectId,
        Long teacherId,
        LocalDate from,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
        deleteFutureLessons(subjectId, from);
        if (teacherId != null) {
            createLessons(subjectId, teacherId, startAt, endAt, dayOfWeek, startTime, endTime, period);
        }
    }

    private void changeFutureScheduledLessons(Long subjectId, LocalDate from, Consumer<Lesson> change) {
        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );
        lessons.forEach(change);
        syncPublisher.publishFor(lessons);
        log.debug("과목 미래 예정 수업 일괄 변경 (subjectId={}, from={}, 대상={}건)", subjectId, from, lessons.size());
    }
}
