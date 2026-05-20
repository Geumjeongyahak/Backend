package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.exception.DuplicateDailyStudentAttendanceException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleForbiddenException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleJournalAlreadyExistsException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleVolunteerHoursForbiddenException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleAttendanceStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleJournalStateException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailyScheduleJournalLessonsException;
import geumjeongyahak.domain.daily_schedule.exception.InvalidDailySchedulePersonalInfoException;
import geumjeongyahak.domain.daily_schedule.exception.LessonNotInDailyScheduleException;
import geumjeongyahak.domain.daily_schedule.exception.StudentNotInDailyScheduleException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.CreateDailyScheduleJournalRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailySchedulePaginationRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleVolunteerHoursRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyScheduleJournalRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendanceItemRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyStudentAttendancesRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleDetailResponse;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.DailyScheduleLessonResponse;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

        List<DailySchedule> dailySchedules = dailyScheduleRepository
            .findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(request.from(), request.to())
            .stream()
            .filter(dailySchedule -> request.classroomId() == null
                || dailySchedule.getClassroom().getId().equals(request.classroomId()))
            .filter(dailySchedule -> request.teacherId() == null
                || dailySchedule.getTeacher().getId().equals(request.teacherId()))
            .filter(dailySchedule -> request.status() == null || dailySchedule.getStatus() == request.status())
            .toList();
        Map<DailyScheduleLessonKey, List<Lesson>> lessonsByScheduleKey = getLessonsByScheduleKey(dailySchedules);
        List<DailyScheduleSummaryResponse> responses = dailySchedules
            .stream()
            .map(dailySchedule -> new DailyScheduleWithLessons(
                dailySchedule,
                lessonsByScheduleKey.getOrDefault(DailyScheduleLessonKey.from(dailySchedule), List.of())
            ))
            .map(this::toSummaryResponse)
            .toList();
        log.debug("DailySchedule 목록 조회 완료 - 총 {}건", responses.size());
        return responses;
    }

    public PaginationResponse<DailyScheduleSummaryResponse> getJournalDailySchedules(
        DailySchedulePaginationRequest request,
        Long requesterId
    ) {
        List<DailySchedule> dailySchedules = dailyScheduleRepository
            .findAllByIsDeletedFalseOrderByLessonDateDescIdDesc()
            .stream()
            .filter(dailySchedule -> !Boolean.TRUE.equals(request.getMine())
                || dailySchedule.getTeacher().getId().equals(requesterId))
            .toList();
        Map<DailyScheduleLessonKey, List<Lesson>> lessonsByScheduleKey = getLessonsByScheduleKey(dailySchedules);
        List<DailyScheduleSummaryResponse> responses = dailySchedules
            .stream()
            .map(dailySchedule -> new DailyScheduleWithLessons(
                dailySchedule,
                lessonsByScheduleKey.getOrDefault(DailyScheduleLessonKey.from(dailySchedule), List.of())
            ))
            .filter(dailyScheduleWithLessons -> hasWrittenJournal(dailyScheduleWithLessons.lessons()))
            .filter(dailyScheduleWithLessons -> matchesKeyword(dailyScheduleWithLessons, request.getKeyword()))
            .map(this::toSummaryResponse)
            .toList();
        PageRequest pageRequest = request.toRequest();
        int start = Math.min((int) pageRequest.getOffset(), responses.size());
        int end = Math.min(start + pageRequest.getPageSize(), responses.size());
        return new PaginationResponse<>(new PageImpl<>(
            responses.subList(start, end),
            pageRequest,
            responses.size()
        ));
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
    public DailyScheduleDetailResponse createJournal(
        Long authorId,
        boolean canWriteAnyDailySchedule,
        CreateDailyScheduleJournalRequest request
    ) {
        log.debug(
            "DailySchedule 수업 일지 생성 요청 (authorId={}, classroomId={}, lessonDate={}, lessonJournalCount={})",
            authorId,
            request.classroomId(),
            request.lessonDate(),
            request.lessonJournals().size()
        );
        DailySchedule dailySchedule = dailyScheduleRepository
            .findByClassroomIdAndLessonDateAndIsDeletedFalse(request.classroomId(), request.lessonDate())
            .orElseThrow(() -> {
                log.info(
                    "DailySchedule 수업 일지 생성 실패 - 하루 일정을 찾을 수 없습니다. classroomId={}, lessonDate={}",
                    request.classroomId(),
                    request.lessonDate()
                );
                return new DailyScheduleNotFoundException(
                    "하루 일정을 찾을 수 없습니다. (classroomId: " + request.classroomId()
                        + ", lessonDate: " + request.lessonDate() + ")"
                );
            });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "수업 일지 생성");
        validateJournalState(dailySchedule);
        validateJournalPersonalInfo(
            dailySchedule.getId(),
            request.personalInfoConsent(),
            request.residentRegistrationNumberPrefix()
        );
        List<Lesson> lessons = getDailyScheduleLessons(dailySchedule);
        if (hasWrittenJournal(lessons)) {
            log.info(
                "DailySchedule 수업 일지 생성 실패 - 이미 작성된 수업 일지가 있습니다. dailyScheduleId={}",
                dailySchedule.getId()
            );
            throw new DailyScheduleJournalAlreadyExistsException(dailySchedule.getId());
        }
        saveJournal(
            dailySchedule,
            lessons,
            request.personalInfoConsent(),
            request.residentRegistrationNumberPrefix(),
            request.lessonJournals()
        );
        log.debug("DailySchedule 수업 일지 생성 완료 (dailyScheduleId={})", dailySchedule.getId());
        return getDailySchedule(dailySchedule.getId(), authorId, canWriteAnyDailySchedule);
    }

    @Transactional
    public DailyScheduleDetailResponse updateJournal(
        Long dailyScheduleId,
        Long authorId,
        boolean canWriteAnyDailySchedule,
        UpdateDailyScheduleJournalRequest request
    ) {
        log.debug(
            "DailySchedule 수업 일지 수정 요청 (dailyScheduleId={}, authorId={}, lessonJournalCount={})",
            dailyScheduleId,
            authorId,
            request.lessonJournals().size()
        );
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 수업 일지 수정 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
        });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "수업 일지 수정");
        validateJournalState(dailySchedule);
        validateJournalPersonalInfo(
            dailySchedule.getId(),
            request.personalInfoConsent(),
            request.residentRegistrationNumberPrefix()
        );
        List<Lesson> lessons = getDailyScheduleLessons(dailySchedule);
        saveJournal(
            dailySchedule,
            lessons,
            request.personalInfoConsent(),
            request.residentRegistrationNumberPrefix(),
            request.lessonJournals()
        );
        log.debug("DailySchedule 수업 일지 수정 완료 (dailyScheduleId={})", dailyScheduleId);
        return getDailySchedule(dailyScheduleId, authorId, canWriteAnyDailySchedule);
    }

    @Transactional
    public void deleteJournal(Long dailyScheduleId, Long authorId, boolean canWriteAnyDailySchedule) {
        log.debug("DailySchedule 수업 일지 삭제 요청 (dailyScheduleId={}, authorId={})", dailyScheduleId, authorId);
        DailySchedule dailySchedule = dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> {
                log.info("DailySchedule 수업 일지 삭제 실패 - 하루 일정을 찾을 수 없습니다. ID: {}", dailyScheduleId);
                return new DailyScheduleNotFoundException(dailyScheduleId);
            });
        validateDailyScheduleWritable(dailySchedule, authorId, canWriteAnyDailySchedule, "수업 일지 삭제");
        validateJournalState(dailySchedule);
        List<Lesson> lessons = getDailyScheduleLessons(dailySchedule);
        lessons.forEach(lesson -> lesson.updateNote(null));
        dailySchedule.updateJournalPersonalInfo(null, false);
        markIncompleteAfterJournalDelete(dailySchedule, lessons);
        log.debug("DailySchedule 수업 일지 삭제 완료 (dailyScheduleId={})", dailyScheduleId);
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

    private void markIncompleteAfterJournalDelete(DailySchedule dailySchedule, List<Lesson> lessons) {
        if (dailySchedule.getStatus() != DailyScheduleStatus.COMPLETED) {
            return;
        }

        dailySchedule.updateStatus(DailyScheduleStatus.SCHEDULED);
        lessons.forEach(lesson -> lesson.updateStatus(LessonStatus.SCHEDULED));
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
        Boolean personalInfoConsent,
        String residentRegistrationNumberPrefix
    ) {
        if (Boolean.TRUE.equals(personalInfoConsent)
            && residentRegistrationNumberPrefix != null
            && !residentRegistrationNumberPrefix.isBlank()) {
            return;
        }

        if (Boolean.FALSE.equals(personalInfoConsent)
            && (residentRegistrationNumberPrefix == null || residentRegistrationNumberPrefix.isBlank())) {
            return;
        }

        log.info(
            "DailySchedule 수업 일지 저장 실패 - 개인정보 입력값이 유효하지 않습니다. dailyScheduleId={}, personalInfoConsent={}, hasResidentPrefix={}",
            dailyScheduleId,
            personalInfoConsent,
            residentRegistrationNumberPrefix != null && !residentRegistrationNumberPrefix.isBlank()
        );
        throw new InvalidDailySchedulePersonalInfoException(dailyScheduleId);
    }

    private List<Lesson> getDailyScheduleLessons(DailySchedule dailySchedule) {
        return lessonProxyService.getActiveLessonsByClassroomAndDate(
            dailySchedule.getClassroom().getId(),
            dailySchedule.getLessonDate()
        );
    }

    private void saveJournal(
        DailySchedule dailySchedule,
        List<Lesson> lessons,
        Boolean personalInfoConsent,
        String residentRegistrationNumberPrefix,
        List<UpdateDailyScheduleJournalRequest.LessonJournalRequest> lessonJournals
    ) {
        validateJournalLessons(dailySchedule.getId(), lessons, lessonJournals);
        Map<Long, Lesson> lessonsById = lessons.stream()
            .collect(toMap(Lesson::getId, Function.identity()));

        for (UpdateDailyScheduleJournalRequest.LessonJournalRequest lessonJournal : lessonJournals) {
            Lesson lesson = lessonsById.get(lessonJournal.lessonId());
            if (lesson == null) {
                log.info(
                    "DailySchedule 수업 일지 저장 실패 - 하루 일정에 연결되지 않은 수업입니다. dailyScheduleId={}, lessonId={}",
                    dailySchedule.getId(),
                    lessonJournal.lessonId()
                );
                throw new LessonNotInDailyScheduleException(dailySchedule.getId(), lessonJournal.lessonId());
            }
            lesson.updateNote(lessonJournal.note());
        }

        dailySchedule.updateJournalPersonalInfo(residentRegistrationNumberPrefix, personalInfoConsent);
        completeIfReady(dailySchedule, lessons);
    }

    private void validateJournalLessons(
        Long dailyScheduleId,
        List<Lesson> lessons,
        List<UpdateDailyScheduleJournalRequest.LessonJournalRequest> lessonJournals
    ) {
        Set<Long> lessonIds = lessons.stream()
            .map(Lesson::getId)
            .collect(Collectors.toSet());
        Set<Long> requestedLessonIds = lessonJournals.stream()
            .map(UpdateDailyScheduleJournalRequest.LessonJournalRequest::lessonId)
            .collect(Collectors.toSet());

        if (lessonIds.equals(requestedLessonIds) && lessonJournals.size() == requestedLessonIds.size()) {
            return;
        }

        log.info(
            "DailySchedule 수업 일지 저장 실패 - 연결된 모든 교시의 수업 일지를 입력해야 합니다. dailyScheduleId={}, lessonIds={}, requestedLessonIds={}",
            dailyScheduleId,
            lessonIds,
            requestedLessonIds
        );
        throw new InvalidDailyScheduleJournalLessonsException(dailyScheduleId);
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

    private boolean hasWrittenJournal(List<Lesson> lessons) {
        return lessons.stream().anyMatch(lesson -> hasText(lesson.getNote()));
    }

    private boolean matchesKeyword(DailyScheduleWithLessons dailyScheduleWithLessons, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        DailySchedule dailySchedule = dailyScheduleWithLessons.dailySchedule();
        return containsIgnoreCase(dailySchedule.getClassroom().getName(), normalizedKeyword)
            || containsIgnoreCase(dailySchedule.getTeacher().getName(), normalizedKeyword)
            || dailyScheduleWithLessons.lessons().stream()
                .anyMatch(lesson -> containsIgnoreCase(lesson.getSubject().getName(), normalizedKeyword)
                    || containsIgnoreCase(lesson.getNote(), normalizedKeyword));
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase().contains(normalizedKeyword);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<DailyScheduleLessonKey, List<Lesson>> getLessonsByScheduleKey(List<DailySchedule> dailySchedules) {
        Set<Long> classroomIds = dailySchedules.stream()
            .map(dailySchedule -> dailySchedule.getClassroom().getId())
            .collect(Collectors.toSet());
        Set<LocalDate> lessonDates = dailySchedules.stream()
            .map(DailySchedule::getLessonDate)
            .collect(Collectors.toSet());

        return lessonProxyService.getActiveLessonsByClassroomIdsAndDates(classroomIds, lessonDates)
            .stream()
            .collect(Collectors.groupingBy(DailyScheduleLessonKey::from));
    }

    private DailyScheduleSummaryResponse toSummaryResponse(DailyScheduleWithLessons dailyScheduleWithLessons) {
        DailySchedule dailySchedule = dailyScheduleWithLessons.dailySchedule();
        DailyTeacherAttendance teacherAttendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
            .orElse(null);
        List<DailyScheduleLessonResponse> lessons = dailyScheduleWithLessons.lessons()
            .stream()
            .map(DailyScheduleLessonResponse::from)
            .toList();
        return DailyScheduleSummaryResponse.of(dailySchedule, teacherAttendance, lessons);
    }

    private record DailyScheduleWithLessons(
        DailySchedule dailySchedule,
        List<Lesson> lessons
    ) {
    }

    private record DailyScheduleLessonKey(
        Long classroomId,
        LocalDate lessonDate
    ) {
        private static DailyScheduleLessonKey from(DailySchedule dailySchedule) {
            return new DailyScheduleLessonKey(
                dailySchedule.getClassroom().getId(),
                dailySchedule.getLessonDate()
            );
        }

        private static DailyScheduleLessonKey from(Lesson lesson) {
            return new DailyScheduleLessonKey(
                lesson.getSubject().getClassroom().getId(),
                lesson.getDate()
            );
        }
    }
}
