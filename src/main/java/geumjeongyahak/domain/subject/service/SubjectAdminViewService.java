package geumjeongyahak.domain.subject.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectAdminViewService {

    private static final String UNASSIGNED_TEACHER_LABEL = "미배정";
    private static final String ACTIVE_LABEL = "활성";
    private static final String INACTIVE_LABEL = "비활성";

    private final SubjectService subjectService;
    private final ClassroomProxyService classroomProxyService;
    private final UserProxyService userProxyService;

    public SubjectAdminPage getSubjects(SubjectFilter filter) {
        List<SubjectRow> rows = subjectService.getAllSubjects(filter.classroomId())
            .stream()
            .filter(subject -> filter.active() == null || filter.active().equals(subject.isActive()))
            .sorted(subjectComparator())
            .map(SubjectRow::from)
            .toList();

        return new SubjectAdminPage(rows);
    }

    public SubjectDetail getSubject(Long subjectId) {
        return SubjectDetail.from(subjectService.getSubject(subjectId));
    }

    public List<ClassroomOption> getClassroomOptions() {
        return classroomProxyService.getActiveClassroomsOrderByName()
            .stream()
            .map(ClassroomOption::from)
            .toList();
    }

    public List<TeacherOption> getTeacherOptions() {
        return userProxyService.getTeacherCandidatesOrderByName()
            .stream()
            .map(TeacherOption::from)
            .toList();
    }

    public List<DayOfWeekOption> getDayOfWeekOptions() {
        return Arrays.stream(DayOfWeek.values())
            .map(dayOfWeek -> new DayOfWeekOption(dayOfWeek, getDayOfWeekLabel(dayOfWeek)))
            .toList();
    }

    @Transactional
    public Long createSubject(
        Long classroomId,
        Long teacherId,
        String name,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period,
        String description
    ) {
        SubjectDetailResponse response = subjectService.createSubject(new CreateSubjectRequest(
            classroomId,
            teacherId,
            name,
            startAt,
            endAt,
            dayOfWeek,
            startTime,
            endTime,
            period,
            normalizeDescription(description)
        ));
        return response.id();
    }

    private Comparator<SubjectDetailResponse> subjectComparator() {
        return Comparator
            .comparing(SubjectDetailResponse::classroomName, Comparator.nullsLast(String::compareTo))
            .thenComparing(SubjectDetailResponse::dayOfWeek, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SubjectDetailResponse::period, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SubjectDetailResponse::startAt, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(SubjectDetailResponse::id, Comparator.nullsLast(Long::compareTo));
    }

    private static String getTeacherLabel(Long teacherId, String teacherName) {
        if (teacherId == null) {
            return UNASSIGNED_TEACHER_LABEL;
        }
        return teacherName;
    }

    private static String getActiveLabel(Boolean isActive) {
        return Boolean.TRUE.equals(isActive) ? ACTIVE_LABEL : INACTIVE_LABEL;
    }

    private static String getDayOfWeekLabel(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return "-";
        }
        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    public record SubjectFilter(
        Long classroomId,
        Boolean active
    ) {
    }

    public record SubjectAdminPage(
        List<SubjectRow> rows
    ) {
    }

    public record SubjectRow(
        Long id,
        Long classroomId,
        String classroomName,
        Long teacherId,
        String teacherName,
        String teacherLabel,
        String name,
        LocalDate startAt,
        LocalDate endAt,
        Integer times,
        DayOfWeek dayOfWeek,
        String dayOfWeekLabel,
        LocalTime startTime,
        LocalTime endTime,
        Integer period,
        LocalDateTime teacherAssignedAt,
        Boolean isActive,
        String activeLabel
    ) {
        private static SubjectRow from(SubjectDetailResponse response) {
            return new SubjectRow(
                response.id(),
                response.classroomId(),
                response.classroomName(),
                response.teacherId(),
                response.teacherName(),
                getTeacherLabel(response.teacherId(), response.teacherName()),
                response.name(),
                response.startAt(),
                response.endAt(),
                response.times(),
                response.dayOfWeek(),
                getDayOfWeekLabel(response.dayOfWeek()),
                response.startTime(),
                response.endTime(),
                response.period(),
                response.teacherAssignedAt(),
                response.isActive(),
                getActiveLabel(response.isActive())
            );
        }
    }

    public record SubjectDetail(
        Long id,
        Long classroomId,
        String classroomName,
        Long teacherId,
        String teacherName,
        String teacherLabel,
        String name,
        LocalDate startAt,
        LocalDate endAt,
        Integer times,
        DayOfWeek dayOfWeek,
        String dayOfWeekLabel,
        LocalTime startTime,
        LocalTime endTime,
        Integer period,
        LocalDateTime teacherAssignedAt,
        String description,
        Boolean isActive,
        String activeLabel
    ) {
        private static SubjectDetail from(SubjectDetailResponse response) {
            return new SubjectDetail(
                response.id(),
                response.classroomId(),
                response.classroomName(),
                response.teacherId(),
                response.teacherName(),
                getTeacherLabel(response.teacherId(), response.teacherName()),
                response.name(),
                response.startAt(),
                response.endAt(),
                response.times(),
                response.dayOfWeek(),
                getDayOfWeekLabel(response.dayOfWeek()),
                response.startTime(),
                response.endTime(),
                response.period(),
                response.teacherAssignedAt(),
                response.description(),
                response.isActive(),
                getActiveLabel(response.isActive())
            );
        }
    }

    public record ClassroomOption(
        Long id,
        String name
    ) {
        private static ClassroomOption from(Classroom classroom) {
            return new ClassroomOption(classroom.getId(), classroom.getName());
        }
    }

    public record TeacherOption(
        Long id,
        String name
    ) {
        private static TeacherOption from(User user) {
            return new TeacherOption(user.getId(), user.getName());
        }
    }

    public record DayOfWeekOption(
        DayOfWeek value,
        String label
    ) {
    }
}
