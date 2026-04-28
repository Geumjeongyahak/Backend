package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestProxyService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;

    public LessonExchangeRequest getById(Long requestId) {
        return lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));
    }
}
