package geumjeongyahak.domain.teacher_assignment.service;

import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.service.SubjectService;
import geumjeongyahak.domain.subject.v1.dto.request.AssignSubjectTeacherRequest;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectDetailResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherAssignmentService {

    private final SubjectService subjectService;
    private final UserProxyService userProxyService;
    private final TeacherScheduleValidationService teacherScheduleValidationService;
    private final TeacherAssignmentPermissionService teacherAssignmentPermissionService;

    @Transactional
    public List<SubjectDetailResponse> assignSchedule(
        List<Long> subjectIds,
        Long teacherId,
        boolean confirmTeacherReplacement
    ) {
        List<Subject> subjects = getSubjects(subjectIds);
        validateSameSchedule(subjects);
        subjects.forEach(subject -> teacherScheduleValidationService.validateReplacement(
            subject,
            teacherId,
            confirmTeacherReplacement
        ));

        User teacher = userProxyService.getById(teacherId);
        fillDefaultClassroomIfMissing(teacher, subjects.get(0));

        List<SubjectDetailResponse> responses = subjects.stream()
            .map(subject -> subjectService.assignTeacher(
                subject.getId(),
                new AssignSubjectTeacherRequest(teacherId)
            ))
            .toList();
        teacherAssignmentPermissionService.addClassroomChannelWritePermission(
            teacher.getId(),
            subjects.get(0).getClassroom().getId()
        );
        return responses;
    }

    @Transactional
    public List<SubjectDetailResponse> assignScheduleToUnassignedSubjects(List<Subject> subjects, Long teacherId) {
        validateSameSchedule(subjects);
        teacherScheduleValidationService.validateUnassigned(subjects);
        return assignSchedule(subjects.stream().map(Subject::getId).toList(), teacherId, false);
    }

    @Transactional
    public List<SubjectDetailResponse> unassignSchedule(List<Long> subjectIds) {
        List<Subject> subjects = getSubjects(subjectIds);
        validateSameSchedule(subjects);
        Map<Long, Set<Long>> teacherIdsByClassroomId =
            teacherAssignmentPermissionService.collectAssignedTeacherIdsByClassroom(subjects);

        List<SubjectDetailResponse> responses = subjects.stream()
            .map(subject -> subjectService.assignTeacher(subject.getId(), new AssignSubjectTeacherRequest(null)))
            .toList();
        teacherAssignmentPermissionService.removeUnusedClassroomChannelWritePermissions(teacherIdsByClassroomId);
        return responses;
    }

    public List<Subject> getSubjects(List<Long> subjectIds) {
        return teacherScheduleValidationService.getSubjects(subjectIds);
    }

    public void validateSameSchedule(List<Subject> subjects) {
        teacherScheduleValidationService.validateSameSchedule(subjects);
    }

    private void fillDefaultClassroomIfMissing(User teacher, Subject subject) {
        if (teacher.getClassroom() == null) {
            teacher.setClassroom(subject.getClassroom());
        }
    }
}
