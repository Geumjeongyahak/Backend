package geumjeongyahak.domain.request.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.event.LessonExchangeApprovedEvent;
import geumjeongyahak.domain.request.exception.RequestAlreadyProcessedException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;

    @Transactional
    public LessonExchangeRequestResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 생성 (requesterId={}, lessonId={})", requesterId, request.lessonId());

        Lesson lesson = lessonProxyService.getActiveById(request.lessonId());
        if (!lesson.getTeacher().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        User requester = userProxyService.getById(requesterId);

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            lesson,
            requester,
            request.title(),
            request.content(),
            request.scope(),
            request.startPeriod(),
            request.endPeriod(),
            request.expiresAt()
        );
        LessonExchangeRequest saved = lessonExchangeRequestRepository.save(exchangeRequest);

        log.debug("수업 교환 요청 생성 완료 (id={})", saved.getId());
        return LessonExchangeRequestResponse.from(saved);
    }

    public List<LessonExchangeRequestResponse> getLessonExchangeRequests(
        Long requesterId, boolean isAdmin, RequestStatus status
    ) {
        log.debug("수업 교환 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<LessonExchangeRequest> list;
        if (status != null) {
            list = isAdmin
                ? lessonExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : lessonExchangeRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                );
        } else {
            list = isAdmin
                ? lessonExchangeRequestRepository.findAllByOrderByCreatedAtDesc()
                : lessonExchangeRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(LessonExchangeRequestResponse::from).toList();
    }

    public LessonExchangeRequestResponse getLessonExchangeRequest(
        Long requesterId, Long requestId, boolean isAdmin
    ) {
        log.debug("수업 교환 요청 상세 조회 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !exchangeRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return LessonExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestResponse approveLessonExchangeRequest(
        Long approverId, Long requestId, Long exchangeWithUserId
    ) {
        log.debug("수업 교환 요청 승인 (requestId={}, exchangeWithUserId={})", requestId, exchangeWithUserId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);

        // exchangeWithUserId 유효성 검증 (존재 여부 확인)
        if (!userProxyService.existsById(exchangeWithUserId)) {
            throw new UserNotFoundException(exchangeWithUserId);
        }

        exchangeRequest.approve(approver);

        eventPublisher.publish(new LessonExchangeApprovedEvent(
            exchangeRequest.getLesson().getId(),
            exchangeRequest.getRequestedBy().getId(),
            exchangeWithUserId,
            approverId
        ));

        log.debug("수업 교환 요청 승인 완료 (requestId={})", requestId);
        return LessonExchangeRequestResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestResponse rejectLessonExchangeRequest(
        Long approverId, Long requestId, String note
    ) {
        log.debug("수업 교환 요청 반려 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        exchangeRequest.reject(approver, note);

        log.debug("수업 교환 요청 반려 완료 (requestId={})", requestId);
        return LessonExchangeRequestResponse.from(exchangeRequest);
    }
}
