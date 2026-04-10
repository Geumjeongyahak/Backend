package sonmoeum.domain.subject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.exception.SubjectNotFoundException;
import sonmoeum.domain.subject.repository.SubjectRepository;

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

    /**
     * 특정 분반에 특정 교사가 연결된 과목이 존재하는지 확인한다.
     */
    @Transactional(readOnly = true)
    public boolean existsByClassroomIdAndTeacherId(Long classroomId, Long teacherId) {
        return subjectRepository.existsByClassroomIdAndTeacherId(classroomId, teacherId);
    }
}
