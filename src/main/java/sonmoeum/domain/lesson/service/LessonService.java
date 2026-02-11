package sonmoeum.domain.lesson.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.lesson.repository.LessonRepository;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;
import sonmoeum.domain.lesson.v1.dto.response.LessonSummaryResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;

    public List<LessonSummaryResponse> getAllLessons(LessonRangeRequest request) {
        log.debug("전체 수업 목록 조회 요청");
        List<Lesson> lessonList = lessonRepository
            .findAllByDateBetweenOrderByDateAscPeriodAsc(request.from(), request.to());
        log.debug("전체 수업 목록 조회 완료 - 총 {}개", lessonList.size());
        return lessonList.stream()
            .map(LessonSummaryResponse::from)
            .toList();
    }
}
