package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.request.enums.RequestStatus;
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
    public boolean existsPendingByRequesterId(Long requesterId) {
        return absenceRequestRepository.existsByRequestedBy_IdAndStatus(
            requesterId,
            RequestStatus.PENDING
        );
    }

    @Transactional(readOnly = true)
    public boolean existsActiveAbsenceRequestByLessonIds(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return false;
        }
        return absenceRequestRepository.existsByDailyScheduleMatchingLessonIds(
            lessonIds,
            List.of(RequestStatus.PENDING, RequestStatus.APPROVED)
        );
    }
}
