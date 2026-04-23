package geumjeongyahak.domain.request.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.event.LessonExchangeApprovedEvent;
import geumjeongyahak.domain.request.exception.DuplicateActiveRequestException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresAfterLessonException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresInPastException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresPolicyException;
import geumjeongyahak.domain.request.exception.InvalidRequestLessonPolicyException;
import geumjeongyahak.domain.request.exception.RequestAlreadyProcessedException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestDetailResponse;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static final int REQUEST_DEADLINE_DAY = 4;
    private static final int EXPIRE_DEADLINE_DAY = 3;
    private static final int EXPIRE_DEADLINE_HOUR = 23;
    private static final int EXPIRE_DEADLINE_MINUTE = 59;
    private static final int EXPIRE_DEADLINE_SECOND = 59;

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;

    @Transactional
    public LessonExchangeRequestDetailResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 생성 (requesterId={}, lessonId={})", requesterId, request.lessonId());

        Lesson lesson = lessonProxyService.getActiveById(request.lessonId());

        validateRequesterOwnsLesson(lesson, requesterId);
        validateLessonWithPolicy(lesson);
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(lesson, request.expiresAt());
        validateExpiresAtWithinPolicy(lesson, request.expiresAt());
        validateNoActiveExchangeRequestExists(lesson.getId());

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
        return LessonExchangeRequestDetailResponse.from(saved);
    }

    public List<LessonExchangeRequestSummaryResponse> getLessonExchangeRequests(
        Long requesterId, LessonExchangeRequestStatus status, boolean mine
    ) {
        log.debug("수업 교환 요청 목록 조회 (status={}, mine={})", status, mine);

        List<LessonExchangeRequest> list;
        if (status != null) {
            list = mine
                ? lessonExchangeRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                )
                : lessonExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            list = mine
                ? lessonExchangeRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId)
                : lessonExchangeRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(LessonExchangeRequestSummaryResponse::from).toList();
    }

    public LessonExchangeRequestDetailResponse getLessonExchangeRequest(
        Long requestId
    ) {
        log.debug("수업 교환 요청 상세 조회 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestDetailResponse approveLessonExchangeRequest(
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
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestDetailResponse rejectLessonExchangeRequest(
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
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    private void validateRequesterOwnsLesson(Lesson lesson, Long requesterId) {
        if (!lesson.getTeacher().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
    }


    // 교환 대상 수업 정책 반영 여부 (현재 기준 4일 이후 수업부터 교환 요청 가능)
    private void validateLessonWithPolicy(Lesson lesson) {
        LocalDate today = LocalDate.now();
        LocalDate earliestRequestableDate = today.plusDays(REQUEST_DEADLINE_DAY);

        if (lesson.getDate().isBefore(earliestRequestableDate )) {
            throw new InvalidRequestLessonPolicyException();
        }
    }


    // expiresAt이 현재 이후인지
    private void validateExpiresAtIsFuture(LocalDateTime expiresAt) {
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new InvalidRequestExpiresInPastException();
        }
    }


    // expiresAt이 수업 날짜 이후인지
    private void validateExpiresAtBeforeLessonDate(Lesson lesson, LocalDateTime expiresAt) {
        LocalDateTime lessonStartBoundary = lesson.getDate().atStartOfDay();

        if (!expiresAt.isBefore(lessonStartBoundary)) {
            throw new InvalidRequestExpiresAfterLessonException();
        }
    }

    // 만료 정책 반영 여부 (만료 시각은 수업일 3일 전 23:59:59를 넘길 수 없음)
    private void validateExpiresAtWithinPolicy(Lesson lesson, LocalDateTime expiresAt) {
        LocalDateTime maxAllowedExpiresAt = lesson.getDate()
            .minusDays(EXPIRE_DEADLINE_DAY)
            .atTime(EXPIRE_DEADLINE_HOUR, EXPIRE_DEADLINE_MINUTE, EXPIRE_DEADLINE_SECOND);

        if (expiresAt.isAfter(maxAllowedExpiresAt)) {
            throw new InvalidRequestExpiresPolicyException();
        }
    }

    // 6. 중복 요청 여부 (같은 수업에 대해 진행 중인(PENDING, APPROVED) 요청이 있으면 생성 불가)
    private void validateNoActiveExchangeRequestExists(Long lessonId) {
        List<LessonExchangeRequestStatus> activeStatuses = List.of(
            LessonExchangeRequestStatus.PENDING,
            LessonExchangeRequestStatus.APPROVED
        );

        boolean exists = lessonExchangeRequestRepository.existsByLesson_IdAndStatusIn(
            lessonId,
            activeStatuses
        );

        if (exists) {
            throw new DuplicateActiveRequestException();
        }
    }
}
