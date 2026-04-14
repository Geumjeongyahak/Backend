package geumjeongyahak.domain.classroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.exception.ClassroomNotFoundException;
import geumjeongyahak.domain.classroom.repository.ClassroomRepository;

/**
 * Classroom 도메인의 Proxy Service.
 * 다른 도메인에서 분반 참조 확인이 필요할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class ClassroomProxyService {

    private final ClassroomRepository classroomRepository;

    /**
     * 삭제되지 않은 분반 조회. 없으면 예외 발생.
     */
    @Transactional(readOnly = true)
    public Classroom getActiveById(Long classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ClassroomNotFoundException(classroomId));

        if (classroom.isDeleted()) {
            throw new ClassroomNotFoundException(classroomId);
        }

        return classroom;
    }
}
