package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AbsenceRequestProxyService {

    private final AbsenceRequestRepository absenceRequestRepository;

    @Transactional(readOnly = true)
    public boolean existsAbsenceRequestByLessonIds(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return false;
        }
        return absenceRequestRepository.existsByLesson_IdIn(lessonIds);
    }
}
