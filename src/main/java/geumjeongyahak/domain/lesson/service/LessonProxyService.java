package geumjeongyahak.domain.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;

/**
 * Lesson 도메인의 Proxy Service.
 * 다른 도메인(request 등)에서 Lesson 엔티티에 접근할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class LessonProxyService {

    private final LessonRepository lessonRepository;

    /**
     * 삭제되지 않은 수업 조회. 없으면 예외 발생.
     */
    @Transactional(readOnly = true)
    public Lesson getActiveById(Long lessonId) {
        return lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }
}
