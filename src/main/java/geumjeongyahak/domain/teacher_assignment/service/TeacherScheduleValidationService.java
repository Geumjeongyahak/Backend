package geumjeongyahak.domain.teacher_assignment.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectTeacherAssignmentConflictException;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.entity.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherScheduleValidationService {

    private final SubjectProxyService subjectProxyService;

    public List<Subject> getSubjects(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "배정할 시간표 과목을 1개 이상 선택해야 합니다.");
        }
        Set<Long> uniqueSubjectIds = new HashSet<>(subjectIds);
        if (uniqueSubjectIds.size() != subjectIds.size()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "배정할 시간표 과목 ID가 중복되었습니다.");
        }
        return subjectProxyService.getAllByIds(uniqueSubjectIds);
    }

    public void validateSameSchedule(List<Subject> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "배정할 시간표 과목을 1개 이상 선택해야 합니다.");
        }
        Subject representative = subjects.get(0);
        Set<Integer> periods = new HashSet<>();
        for (Subject subject : subjects) {
            validateActive(subject);
            validateSameScheduleKey(subject, representative);
            validateUniquePeriod(periods, subject);
        }
    }

    public void validateUnassigned(List<Subject> subjects) {
        if (subjects.stream().anyMatch(subject -> subject.getTeacher() != null)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "이미 담당 교사가 배정된 시간표 과목이 있습니다.");
        }
    }

    public void validateReplacement(Subject subject, Long teacherId, boolean confirmTeacherReplacement) {
        User currentTeacher = subject.getTeacher();
        if (currentTeacher == null || currentTeacher.getId().equals(teacherId) || confirmTeacherReplacement) {
            return;
        }
        throw new SubjectTeacherAssignmentConflictException("기존 담당 교사가 있어 교체 확인이 필요합니다.");
    }

    private void validateActive(Subject subject) {
        if (!Boolean.TRUE.equals(subject.getIsActive())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "비활성 시간표 과목에는 교사를 배정할 수 없습니다.");
        }
    }

    private void validateSameScheduleKey(Subject subject, Subject representative) {
        if (!subject.getClassroom().getId().equals(representative.getClassroom().getId())
            || subject.getDayOfWeek() != representative.getDayOfWeek()
            || !subject.getStartAt().equals(representative.getStartAt())
            || !subject.getEndAt().equals(representative.getEndAt())) {
            throw new BusinessException(
                CommonErrorCode.INVALID_INPUT,
                "같은 분반, 요일, 운영기간의 시간표 과목만 함께 배정할 수 있습니다."
            );
        }
    }

    private void validateUniquePeriod(Set<Integer> periods, Subject subject) {
        if (!periods.add(subject.getPeriod())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "같은 교시 과목은 한 시간표 묶음에 중복 배정할 수 없습니다.");
        }
    }
}
