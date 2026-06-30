package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.lesson.dto.LessonTeacherDate;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestProxyService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final LessonExchangeProposalRepository lessonExchangeProposalRepository;

    public LessonExchangeRequest getById(Long requestId) {
        return lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    public boolean existsActiveExchangeByUserId(Long userId) {
        return lessonExchangeRequestRepository.existsByRequestedBy_IdAndStatusIn(
            userId,
            List.of(
                LessonExchangeRequestStatus.PENDING,
                LessonExchangeRequestStatus.APPROVED
            )
        ) || lessonExchangeProposalRepository.existsByProposedBy_IdAndStatus(
            userId,
            LessonExchangeProposalStatus.ACTIVE
        );
    }

    public boolean existsActiveExchangeByLessonTeacherDates(List<LessonTeacherDate> lessonTeacherDates) {
        if (lessonTeacherDates == null || lessonTeacherDates.isEmpty()) {
            return false;
        }

        List<LessonExchangeRequestStatus> requestStatuses = List.of(
            LessonExchangeRequestStatus.PENDING,
            LessonExchangeRequestStatus.APPROVED
        );
        List<LessonExchangeProposalStatus> proposalStatuses = List.of(
            LessonExchangeProposalStatus.ACTIVE
        );

        return lessonTeacherDates.stream()
            .anyMatch(lesson ->
                lessonExchangeRequestRepository.existsByRequestedBy_IdAndLessonDateAndStatusIn(
                    lesson.teacherId(),
                    lesson.date(),
                    requestStatuses
                )
                    || lessonExchangeProposalRepository.existsByProposedBy_IdAndLessonDateAndStatusIn(
                    lesson.teacherId(),
                    lesson.date(),
                    proposalStatuses
                )
            );
    }
}
