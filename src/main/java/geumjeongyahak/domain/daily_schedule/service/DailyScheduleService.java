package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.DuplicateDailyStudentAttendanceException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleForbiddenException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleVolunteerHoursForbiddenException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleJournalStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailySchedulePersonalInfoException;
import geumjeongyahak.domain.daily_schedule.exception.LessonNotInDailyScheduleException;
import geumjeongyahak.domain.daily_schedule.exception.StudentNotInDailyScheduleException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleVolunteerHoursRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleJournalRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendanceItemRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendancesRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleSummaryResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleVolunteerHoursResponse;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;
    private final DailyStudentAttendanceRepository dailyStudentAttendanceRepository;
    private final LessonProxyService lessonProxyService;
    private final StudentProxyService studentProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public void synchronizeByLesson(Lesson lesson) {
        Long classroomId = lesson.getSubject().getClassroom().getId();
        synchronizeByClassroomAndDate(classroomId, lesson.getDate());
    }

    @Transactional
    public void synchronizeByClassroomAndDate(Long classroomId, LocalDate lessonDate) {
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(classroomId, lessonDate);
        if (lessons.isEmpty()) {
            deleteOrphanDailySchedule(classroomId, lessonDate);
            log.debug("DailySchedule 동기화 스킵 - 활성 수업 없음 (classroomId={}, lessonDate={})", classroomId, lessonDate);
            return;
        }

        Lesson representativeLesson = lessons.get(0);
        Classroom classroom = representativeLesson.getSubject().getClassroom();
        User teacher = representativeLesson.getTeacher();
        LocalTime activityStartTime = lessons.stream()
            .map(Lesson::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(null);
        LocalTime activityEndTime = lessons.stream()
            .map(Lesson::getEndTime)
            .max(LocalTime::compareTo)
            .orElse(null);
        Integer volunteerServiceMinutes = calculateVolunteerServiceMinutes(activityStartTime, activityEndTime);

        DailySchedule dailySchedule = dailyScheduleRepository
            .findByClassroomIdAndLessonDate(classroomId, lessonDate)
            .map(existing -> {
                existing.restore();
                return existing;
            })
            .orElseGet(() -> dailyScheduleRepository.save(new DailySchedule(
                classroom,
                teacher,
                lessonDate,
                activityStartTime,
                activityEndTime
            )));

        dailySchedule.updateTeacher(teacher);
        dailySchedule.updateActivityTime(activityStartTime, activityEndTime);
        initializeTeacherAttendance(dailySchedule, volunteerServiceMinutes);
        initializeStudentAttendances(dailySchedule, classroomId);
    }

    public List<DailyScheduleSummaryResponse> getDailySchedules(DailyScheduleListRequest request) {
        log.debug(
            "DailySchedule 목록 조회 요청 (from={}, to={}, classroomId={}, teacherId={}, status={})",
            request.from(),
            request.to(),
            request.classroomId(),
            request.teacherId(),
            request.status()
        );

        List<DailyScheduleSummaryResponse> responses = dailyScheduleRepository
            .findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(request.from(), request.to())
            .stream()
            .filter(dailySchedule -> request.classroomId() == null
                || dailySchedule.getClassroom().getId().equals(request.classroomId()))
            .filter(dailySchedule -> request.teacherId() == null
                || dailySchedule.getTeacher().getId().equals(request.teacherId()))
            .filter(dailySchedule -> request.status() == null || dailySchedule.getStatus() == request.status())
            .map(this::toSummaryResponse)
            .toList();
        log.debug("DailySchedule 목록 조회 완료 - 총 {}건", responses.size());
        return responses;
    }

    public DailyScheduleDetailResponse getDailySchedule(
        Long dailyScheduleId,
        Long viewerId,
        boolean canViewSensitiveInfo
    ) {
        log.debug("DailySchedule 상세 조회 요청 (dailyScheduleId={})", dailyScheduleId);
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 상세 조회 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate()
        );
        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId)
            .orElse(null);
        List<DailyStudentAttendance> studentAttendances = dailyStudentAttendanceRepository
            .findAllByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId)
            .stream()
            .sorted(Comparator.comparing(attendance -> attendance.getStudent().getName()))
            .toList();

        DailyScheduleDetailResponse response = DailyScheduleDetailResponse.of(
            dailySchedule,
            teacherAttendance,
            lessons,
            studentAttendances,
            canViewDailyScheduleSensitiveInfo(dailySchedule, viewerId, canViewSensitiveInfo)
        );
        log.debug(
            "DailySchedule 상세 조회 완료 (dailyScheduleId={}, lessonCount={}, studentAttendanceCount={})",
            dailyScheduleId,
            lessons.size(),
            studentAttendances.size()
        );
        return response;
    }

    public DailyScheduleVolunteerHoursResponse getVolunteerHours(
        Long requesterId,
        boolean canReadAnyDailySchedule,
        DailyScheduleVolunteerHoursRequest request
    ) {
        Long targetTeacherId = request.teacherId() != null ? request.teacherId() : requesterId;
        log.debug(
            "DailySchedule 봉사 시간 조회 요청 (requesterId={}, targetTeacherId={}, from={}, to={})",
            requesterId,
            targetTeacherId,
            request.from(),
            request.to()
        );
        if (!targetTeacherId.equals(requesterId) && !canReadAnyDailySchedule) {
            log.info(
                "DailySchedule 봉사 시간 조회 실패 - 다른 교사의 봉사 시간을 조회할 권한이 없습니다. requesterId={}, targetTeacherId={}",
                requesterId,
                targetTeacherId
            );
            throw new DailyScheduleVolunteerHoursForbiddenException(requesterId, targetTeacherId);
        }

        Long totalMinutes = dailyTeacherAttendanceRepository.sumVolunteerServiceMinutes(
            targetTeacherId,
            request.from(),
            request.to(),
            DailyScheduleStatus.COMPLETED,
            DailyTeacherAttendanceStatus.ABSENT
        );
        log.debug(
            "DailySchedule 봉사 시간 조회 완료 (targetTeacherId={}, totalMinutes={})",
            targetTeacherId,
            totalMinutes
        );
        return DailyScheduleVolunteerHoursResponse.of(
            targetTeacherId,
            request.from(),
            request.to(),
            totalMinutes
        );
    }

    @Transactional
    public DailyScheduleDetailResponse updateJournal(
        Long dailyScheduleId,
        Long authorId,
        boolean canWriteAnyDailySchedule,
        UpdateDailyScheduleJournalRequest request
    ) {
        log.debug(
            "DailySchedule 수업 일지 저장 요청 (dailyScheduleId={}, authorId={}, lessonJournalCount={})",
            dailyScheduleId,
            authorId,
            request.lessonJournals().size()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 수업 일지 저장 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "수업 일지 저장");
        validateJournalState(dailySchedule);
        validateJournalPersonalInfo(dailySchedule.getId(), request);
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate()
        );
        Map<Long, Lesson> lessonsById = lessons
            .stream()
            .collect(toMap(Lesson::getId, Function.identity()));

        for (UpdateDailyScheduleJournalRequest.LessonJournalRequest lessonJournal : request.lessonJournals()) {
            Lesson lesson = lessonsById.get(lessonJournal.lessonId());
            if (lesson == null) {
                log.info(
                    "DailySchedule 수업 일지 저장 실패 - 하루 일정에 연결되지 않은 수업입니다. dailyScheduleId={}, lessonId={}",
                    dailyScheduleId,
                    lessonJournal.lessonId()
                );
                throw new LessonNotInDailyScheduleException(dailyScheduleId, lessonJournal.lessonId());
            }
            lesson.updateNote(lessonJournal.note());
        }

        dailySchedule.updateJournalPersonalInfo(
            request.residentRegistrationNumberPrefix(),
            request.personalInfoConsent()
        );
        completeIfReady(dailySchedule, lessons);
        log.debug("DailySchedule 수업 일지 저장 완료 (dailyScheduleId={})", dailyScheduleId);
        return getDailySchedule(dailyScheduleId, authorId, canWriteAnyDailySchedule);
    }

    @Transactional
    public DailyScheduleDetailResponse updateStudentAttendances(
        Long dailyScheduleId,
        Long authorId,
        boolean canWriteAnyDailySchedule,
        boolean canViewSensitiveInfo,
        UpdateDailyStudentAttendancesRequest request
    ) {
        log.debug(
            "DailySchedule 학생 출석 처리 요청 (dailyScheduleId={}, authorId={}, attendanceCount={})",
            dailyScheduleId,
            authorId,
            request.attendances().size()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 학생 출석 처리 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "학생 출석 처리");
        validateAttendanceState(dailySchedule, "학생 출석 처리");

        List<DailyStudentAttendance> attendances = dailyStudentAttendanceRepository
            .findAllByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId);
        Map<Long, DailyStudentAttendance> attendancesByStudentId = attendances.stream()
            .collect(toMap(attendance -> attendance.getStudent().getId(), Function.identity()));
        Set<Long> requestedStudentIds = new HashSet<>();

        for (UpdateDailyStudentAttendanceItemRequest item : request.attendances()) {
            if (!requestedStudentIds.add(item.studentId())) {
                log.info(
                    "DailySchedule 학생 출석 처리 실패 - 중복된 학생 출석 요청입니다. dailyScheduleId={}, studentId={}",
                    dailyScheduleId,
                    item.studentId()
                );
                throw new DuplicateDailyStudentAttendanceException(dailyScheduleId, item.studentId());
            }

            DailyStudentAttendance target = attendancesByStudentId.get(item.studentId());
            if (target == null) {
                log.info(
                    "DailySchedule 학생 출석 처리 실패 - 하루 일정에 연결되지 않은 학생입니다. dailyScheduleId={}, studentId={}",
                    dailyScheduleId,
                    item.studentId()
                );
                throw new StudentNotInDailyScheduleException(dailyScheduleId, item.studentId());
            }
            target.updateStatus(item.status());
        }

        log.debug("DailySchedule 학생 출석 처리 완료 (dailyScheduleId={})", dailyScheduleId);
        return getDailySchedule(dailyScheduleId, authorId, canViewSensitiveInfo);
    }

    @Transactional
    public DailyScheduleDetailResponse updateTeacherAttendance(
        Long dailyScheduleId,
        Long authorId,
        boolean canWriteAnyDailySchedule,
        boolean canViewSensitiveInfo,
        UpdateDailyTeacherAttendanceRequest request
    ) {
        log.debug(
            "DailySchedule 교사 출석 처리 요청 (dailyScheduleId={}, authorId={}, status={})",
            dailyScheduleId,
            authorId,
            request.status()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 교사 출석 처리 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "교사 출석 처리");
        validateAttendanceState(dailySchedule, "교사 출석 처리");

        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId)
            .orElseGet(() -> dailyTeacherAttendanceRepository.save(new DailyTeacherAttendance(
                dailySchedule,
                calculateVolunteerServiceMinutes(dailySchedule.getActivityStartTime(), dailySchedule.getActivityEndTime())
            )));
        LocalDateTime attendedAt = request.status() == DailyTeacherAttendanceStatus.ABSENT
            ? null
            : LocalDateTime.now();

        teacherAttendance.updateAttendance(
            request.status(),
            attendedAt,
            attendedAt != null ? request.latitude() : null,
            attendedAt != null ? request.longitude() : null
        );
        completeIfReady(dailySchedule, lessonProxyService.getActiveLessonsByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate()
        ));

        log.debug("DailySchedule 교사 출석 처리 완료 (dailyScheduleId={})", dailyScheduleId);
        return getDailySchedule(dailyScheduleId, authorId, canViewSensitiveInfo);
    }

    @Transactional
    public void applyApprovedAbsence(Long dailyScheduleId) {
        log.debug(
            "승인된 결석 요청 반영 - DailySchedule 교사 출석 공결 처리 (dailyScheduleId={})",
            dailyScheduleId
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info(
                    "승인된 결석 요청 반영 실패 - 하루 일정을 찾을 수 없습니다. dailyScheduleId={}",
                    dailyScheduleId
                );
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
            .orElseGet(() -> dailyTeacherAttendanceRepository.save(new DailyTeacherAttendance(
                dailySchedule,
                calculateVolunteerServiceMinutes(dailySchedule.getActivityStartTime(), dailySchedule.getActivityEndTime())
            )));

        teacherAttendance.updateAttendance(DailyTeacherAttendanceStatus.EXCUSED, null, null, null);
        log.debug(
            "승인된 결석 요청 반영 완료 - DailySchedule 교사 출석 공결 처리 (dailyScheduleId={})",
            dailySchedule.getId()
        );
    }

    @Transactional
    public void applyTeacherExchange(Long dailyScheduleId, Long newTeacherId) {
        log.debug(
            "DailySchedule 담당 교사 교환 처리 (dailyScheduleId={}, newTeacherId={})",
            dailyScheduleId,
            newTeacherId
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 담당 교사 교환 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        User newTeacher = userProxyService.getById(newTeacherId);

        dailySchedule.updateTeacher(newTeacher);
        lessonProxyService.updateActiveLessonsTeacherByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate(),
            newTeacher
        );

        log.debug("DailySchedule 담당 교사 교환 완료 (dailyScheduleId={}, newTeacherId={})", dailyScheduleId, newTeacherId);
    }

    private boolean canViewDailyScheduleSensitiveInfo(
        DailySchedule dailySchedule,
        Long viewerId,
        boolean canViewSensitiveInfo
    ) {
        return canViewSensitiveInfo || dailySchedule.getTeacher().getId().equals(viewerId);
    }

    private void completeIfReady(DailySchedule dailySchedule, List<Lesson> lessons) {
        if (dailySchedule.getStatus() == DailyScheduleStatus.CANCELLED) {
            return;
        }

        boolean teacherAttendanceCompleted = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
            .map(attendance -> attendance.getStatus() != DailyTeacherAttendanceStatus.ABSENT)
            .orElse(false);
        boolean journalCompleted = !lessons.isEmpty()
            && lessons.stream().allMatch(lesson -> lesson.getNote() != null && !lesson.getNote().isBlank());

        if (teacherAttendanceCompleted && journalCompleted) {
            dailySchedule.updateStatus(DailyScheduleStatus.COMPLETED);
            lessonProxyService.updateActiveLessonsStatusByClassroomAndDate(
                dailySchedule.getClassroom().getId(),
                dailySchedule.getLessonDate(),
                LessonStatus.COMPLETED
            );
        }
    }

    private void validateJournalState(DailySchedule dailySchedule) {
        if (dailySchedule.getStatus() != DailyScheduleStatus.CANCELLED) {
            return;
        }

        log.info(
            "DailySchedule 수업 일지 저장 실패 - 휴강 상태에서는 저장할 수 없습니다. dailyScheduleId={}, status={}",
            dailySchedule.getId(),
            dailySchedule.getStatus()
        );
        throw new InvalidDailyScheduleJournalStateException(dailySchedule.getId(), dailySchedule.getStatus());
    }

    private void validateAttendanceState(DailySchedule dailySchedule, String operation) {
        if (dailySchedule.getStatus() != DailyScheduleStatus.CANCELLED) {
            return;
        }

        log.info(
            "DailySchedule {} 실패 - 휴강 상태에서는 처리할 수 없습니다. dailyScheduleId={}, status={}",
            operation,
            dailySchedule.getId(),
            dailySchedule.getStatus()
        );
        throw new InvalidDailyScheduleAttendanceStateException(dailySchedule.getId(), dailySchedule.getStatus());
    }

    private void validateJournalPersonalInfo(
        Long dailyScheduleId,
        UpdateDailyScheduleJournalRequest request
    ) {
        if (request.personalInfoConsent()
            && request.residentRegistrationNumberPrefix() != null
            && !request.residentRegistrationNumberPrefix().isBlank()) {
            return;
        }

        if (!request.personalInfoConsent()
            && (request.residentRegistrationNumberPrefix() == null
            || request.residentRegistrationNumberPrefix().isBlank())) {
            return;
        }

        log.info(
            "DailySchedule 수업 일지 저장 실패 - 개인정보 입력값이 유효하지 않습니다. dailyScheduleId={}, personalInfoConsent={}, hasResidentPrefix={}",
            dailyScheduleId,
            request.personalInfoConsent(),
            request.residentRegistrationNumberPrefix() != null && !request.residentRegistrationNumberPrefix().isBlank()
        );
        throw new InvalidDailySchedulePersonalInfoException(dailyScheduleId);
    }

    private void validateDailyScheduleWritable(
        DailySchedule dailySchedule,
        Long authorId,
        boolean canWriteAnyDailySchedule,
        String operation
    ) {
        if (canWriteAnyDailySchedule || dailySchedule.getTeacher().getId().equals(authorId)) {
            return;
        }

        log.info(
            "DailySchedule {} 실패 - 담당 교사 또는 관리 권한자가 아닙니다. dailyScheduleId={}, teacherId={}, authorId={}",
            operation,
            dailySchedule.getId(),
            dailySchedule.getTeacher().getId(),
            authorId
        );
        throw new DailyScheduleForbiddenException(dailySchedule.getId(), authorId);
    }

    private Integer calculateVolunteerServiceMinutes(LocalTime activityStartTime, LocalTime activityEndTime) {
        if (activityStartTime == null || activityEndTime == null) {
            return null;
        }
        return Math.toIntExact(Duration.between(activityStartTime, activityEndTime).toMinutes());
    }

    private void initializeTeacherAttendance(DailySchedule dailySchedule, Integer volunteerServiceMinutes) {
        DailyTeacherAttendance attendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleId(dailySchedule.getId())
            .orElseGet(() -> dailyTeacherAttendanceRepository.save(new DailyTeacherAttendance(
                dailySchedule,
                volunteerServiceMinutes
            )));
        attendance.restore();
        attendance.updateVolunteerServiceMinutes(volunteerServiceMinutes);
    }

    private void initializeStudentAttendances(DailySchedule dailySchedule, Long classroomId) {
        List<Student> activeStudents = studentProxyService.getActiveStudentsByClassroomId(classroomId);
        for (Student student : activeStudents) {
            DailyStudentAttendance attendance = dailyStudentAttendanceRepository
                .findByDailyScheduleIdAndStudentId(dailySchedule.getId(), student.getId())
                .orElseGet(() -> dailyStudentAttendanceRepository.save(new DailyStudentAttendance(dailySchedule, student)));
            attendance.restore();
        }
    }

    private void deleteOrphanDailySchedule(Long classroomId, LocalDate lessonDate) {
        dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(classroomId, lessonDate)
            .ifPresent(dailySchedule -> {
                dailyTeacherAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
                    .forEach(DailyTeacherAttendance::softDelete);
                dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
                    .forEach(DailyStudentAttendance::softDelete);
                dailySchedule.softDelete();
            });
    }

    private DailyScheduleSummaryResponse toSummaryResponse(DailySchedule dailySchedule) {
        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
            .orElse(null);
        int lessonCount = lessonProxyService.getActiveLessonsByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate()
        ).size();
        return DailyScheduleSummaryResponse.of(dailySchedule, teacherAttendance, lessonCount);
    }
}
