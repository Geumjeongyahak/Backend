package geumjeongyahak.domain.subject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectNotFoundException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Subject 도메인의 Proxy Service.
 * 다른 도메인(request 등)에서 Subject 엔티티에 접근할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class SubjectProxyService {

    private final SubjectRepository subjectRepository;

    /**
     * 과목 조회. 없으면 예외 발생.
     */
    @Transactional(readOnly = true)
    public Subject getById(Long subjectId) {
        return subjectRepository.findById(subjectId)
            .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllByIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueSubjectIds = new HashSet<>(subjectIds);
        List<Subject> subjects = subjectRepository.findAllByIdIn(uniqueSubjectIds);
        if (subjects.size() != uniqueSubjectIds.size()) {
            throw new SubjectNotFoundException(subjectIds.iterator().next());
        }
        return subjects.stream()
            .sorted(Comparator.comparing(Subject::getPeriod)
                .thenComparing(Subject::getStartTime)
                .thenComparing(Subject::getId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Subject> getUnassignedActiveSubjectsOrderByStartAtAndId() {
        return subjectRepository.findAllByTeacherIsNullAndIsActiveTrueOrderByStartAtAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<Subject> getActiveSubjectsByTeacherId(Long teacherId) {
        return subjectRepository.findAllByTeacherIdAndIsActiveTrueOrderByStartAtAscIdAsc(teacherId);
    }

    @Transactional(readOnly = true)
    public List<Subject> getActiveSubjectsByTeacherIds(Collection<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }
        return subjectRepository.findAllByTeacherIdInAndIsActiveTrueOrderByTeacherIdAscStartAtAscIdAsc(teacherIds);
    }

    /**
     * 특정 분반에 특정 교사가 연결된 과목이 존재하는지 확인한다.
     */
    @Transactional(readOnly = true)
    public boolean existsByClassroomIdAndTeacherId(Long classroomId, Long teacherId) {
        return subjectRepository.existsByClassroomIdAndTeacherId(classroomId, teacherId);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSubjectByClassroomIdAndTeacherId(Long classroomId, Long teacherId) {
        return subjectRepository.existsByClassroomIdAndTeacherIdAndIsActiveTrue(classroomId, teacherId);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSubjectByClassroomId(Long classroomId) {
        return subjectRepository.existsByClassroomIdAndIsActiveTrue(classroomId);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSubjectByTeacherId(Long teacherId) {
        return subjectRepository.existsByTeacherIdAndIsActiveTrue(teacherId);
    }
}
