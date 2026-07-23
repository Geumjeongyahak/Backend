package geumjeongyahak.domain.request.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.notification.enums.PushRequestType;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.exception.LessonExchangeRequest.*;
import geumjeongyahak.domain.request.exception.RequestAlreadyProcessedException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.LessonExchangeRequestListRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestDetailResponse;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeRequestService {

    private final LessonExchangeRequestRepository lessonExchangeRequestRepository;
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public LessonExchangeRequestDetailResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug(
            "수업 교환 요청 생성 (requesterId={}, lessonDate={})",
            requesterId,
            request.lessonDate()
        );

        DailySchedule dailySchedule = getTargetDailySchedule(
            requesterId,
            request.lessonDate()
        );
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime lessonStartAt = getLessonStartAt(dailySchedule);
        validateLessonNotStarted(now, lessonStartAt);
        LocalDateTime expiresAt = resolveExpiresAt(request.expiresDate(), lessonStartAt);
        validateExpiresAt(now, lessonStartAt, expiresAt);

        validateNoActiveExchangeRequestExists(
            requesterId,
            dailySchedule.getId()
        );

        User requester = userProxyService.getById(requesterId);

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            dailySchedule,
            requester,
            request.title(),
            dailySchedule.getClassroom().getName(),
            request.content(),
            expiresAt
        );
        LessonExchangeRequest saved = lessonExchangeRequestRepository.save(exchangeRequest);

        log.debug("수업 교환 요청 생성 완료 (id={})", saved.getId());
        return LessonExchangeRequestDetailResponse.from(saved);
    }

    public PaginationResponse<LessonExchangeRequestSummaryResponse> getLessonExchangeRequests(
        Long requesterId, LessonExchangeRequestListRequest request
    ) {
        log.debug("수업 교환 요청 목록 조회 (status={}, mine={}, keyword={})",
            request.getStatus(), request.isMine(), request.getKeyword());

        PageRequest pageRequest = request.toRequest();
        Page<LessonExchangeRequest> requests = lessonExchangeRequestRepository.findAll(
            buildListSpecification(requesterId, request),
            pageRequest
        );

        return PaginationResponse.from(requests, LessonExchangeRequestSummaryResponse::from);
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
        Long approverId,
        Long requestId
    ) {
        log.debug("수업 교환 요청 승인 (requestId={}, approverId={})", requestId, approverId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        validateRequestNotExpired(exchangeRequest, LocalDateTime.now(clock));

        User approver = userProxyService.getById(approverId);
        exchangeRequest.approve(approver);
        eventPublisher.publish(RequestReviewedPushEvent.approved(
            exchangeRequest.getRequestedBy().getId(),
            exchangeRequest.getId(),
            PushRequestType.LESSON_EXCHANGE,
            approverId,
            "수업 교환 요청이 승인되었습니다.",
            "수업 교환 요청이 승인되었습니다. 교환 제안을 받을 수 있습니다.",
            null
        ));

        log.debug("수업 교환 요청 승인 완료 (requestId={}, approverId={})", requestId, approverId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestDetailResponse updateLessonExchangeRequest(
        Long requesterId,
        Long requestId,
        UpdateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 수정 (requestId={}, requesterId={})", requestId, requesterId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!exchangeRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        validateLessonNotStarted(now, getLessonStartAt(exchangeRequest.getDailySchedule()));
        validateRequestNotExpired(exchangeRequest, now);

        DailySchedule dailySchedule = getTargetDailySchedule(
            requesterId,
            request.lessonDate()
        );
        LocalDateTime lessonStartAt = getLessonStartAt(dailySchedule);
        validateLessonNotStarted(now, lessonStartAt);
        LocalDateTime expiresAt = resolveExpiresAt(request.expiresDate(), lessonStartAt);
        validateExpiresAt(now, lessonStartAt, expiresAt);

        validateNoActiveExchangeRequestExists(
            requesterId,
            dailySchedule.getId(),
            exchangeRequest.getId()
        );

        // 수정 이후에도 요청 화면에는 최신 수정 기준의 반 이름이 유지되도록 snapshot을 함께 갱신
        exchangeRequest.update(
            dailySchedule,
            request.title(),
            dailySchedule.getClassroom().getName(),
            request.content(),
            expiresAt
        );

        log.debug("수업 교환 요청 수정 완료 (requestId={}, requesterId={})", requestId, requesterId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestDetailResponse cancelLessonExchangeRequest(
        Long requesterId,
        Long requestId
    ) {
        log.debug("수업 교환 요청 취소 (requestId={}, requesterId={})", requestId, requesterId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!exchangeRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        validateRequestNotExpired(exchangeRequest, LocalDateTime.now(clock));

        exchangeRequest.cancel();

        log.debug("수업 교환 요청 취소 완료 (requestId={}, requesterId={})", requestId, requesterId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public LessonExchangeRequestDetailResponse rejectLessonExchangeRequest(
        Long approverId, Long requestId, String note
    ) {
        log.debug("수업 교환 요청 반려 (requestId={}, approverId={})", requestId, approverId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (exchangeRequest.getStatus() != LessonExchangeRequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        validateRequestNotExpired(exchangeRequest, LocalDateTime.now(clock));

        User approver = userProxyService.getById(approverId);
        exchangeRequest.reject(approver, note);
        eventPublisher.publish(RequestReviewedPushEvent.rejected(
            exchangeRequest.getRequestedBy().getId(),
            exchangeRequest.getId(),
            PushRequestType.LESSON_EXCHANGE,
            approverId,
            "수업 교환 요청이 반려되었습니다.",
            "수업 교환 요청이 반려되었습니다. 반려 사유를 확인해주세요.",
            note
        ));

        log.debug("수업 교환 요청 반려 완료 (requestId={}, approverId={})", requestId, approverId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public int expireExpiredLessonExchangeRequests() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<LessonExchangeRequest> expiredRequests =
            lessonExchangeRequestRepository.findAllByStatusInAndExpiresAtLessThanEqual(
                List.of(LessonExchangeRequestStatus.PENDING, LessonExchangeRequestStatus.APPROVED),
                now
            );

        expiredRequests.forEach(this::expireRequest);

        if (!expiredRequests.isEmpty()) {
            log.info("수업 교환 요청 자동 만료 처리 완료 (count={}, expiredAt={})", expiredRequests.size(), now);
        }

        return expiredRequests.size();
    }

    private LocalDateTime getLessonStartAt(DailySchedule dailySchedule) {
        if (dailySchedule.getActivityStartTime() == null) {
            throw new LessonExchangeRequestLessonStartTimeNotFoundException();
        }
        return dailySchedule.getLessonDate().atTime(dailySchedule.getActivityStartTime());
    }

    private void validateLessonNotStarted(LocalDateTime now, LocalDateTime lessonStartAt) {
        if (!now.isBefore(lessonStartAt)) {
            throw new LessonExchangeRequestLessonAlreadyStartedException();
        }
    }

    private LocalDateTime resolveExpiresAt(LocalDate requestedExpiresDate, LocalDateTime lessonStartAt) {
        if (requestedExpiresDate == null
            || requestedExpiresDate.equals(lessonStartAt.toLocalDate())) {
            return lessonStartAt;
        }

        if (requestedExpiresDate.isAfter(lessonStartAt.toLocalDate())) {
            throw new InvalidRequestExpiresAfterLessonException();
        }

        return requestedExpiresDate.atTime(23, 59, 59);
    }

    private void validateExpiresAt(
        LocalDateTime now,
        LocalDateTime lessonStartAt,
        LocalDateTime expiresAt
    ) {
        if (!expiresAt.isAfter(now)) {
            throw new InvalidRequestExpiresInPastException();
        }

        if (expiresAt.isAfter(lessonStartAt)) {
            throw new InvalidRequestExpiresAfterLessonException();
        }
    }

    private void validateRequestNotExpired(
        LessonExchangeRequest request,
        LocalDateTime now
    ) {
        LocalDateTime lessonStartAt = getLessonStartAt(request.getDailySchedule());
        if (!request.getExpiresAt().isAfter(now) || !lessonStartAt.isAfter(now)) {
            throw new LessonExchangeRequestExpiredException();
        }
    }

    // 중복 요청 여부 (같은 수업에 대해 진행 중인(PENDING, APPROVED) 요청이 있으면 생성 불가)
    // 생성 시에는 제외할 요청이 없으므로, 전체 수업 교환 요청을 그대로 중복 검사 (excludeRequestId = null)
    private void validateNoActiveExchangeRequestExists(Long requesterId, Long dailyScheduleId) {
        validateNoActiveExchangeRequestExists(requesterId, dailyScheduleId, null);
    }

    // 수정 시에는 현재 수정 중인 자기 자신 요청은 중복 검사 대상에서 제외해야 하기 때문에 excludedRequestId 파라미터를 따로 받음
    private void validateNoActiveExchangeRequestExists(
        Long requesterId,
        Long dailyScheduleId,
        Long excludedRequestId
    ) {
        List<LessonExchangeRequestStatus> activeStatuses = List.of(
            LessonExchangeRequestStatus.PENDING,
            LessonExchangeRequestStatus.APPROVED
        );

        List<LessonExchangeRequest> existingRequests =
            lessonExchangeRequestRepository.findAllByRequestedBy_IdAndDailySchedule_IdAndStatusIn(
                requesterId,
                dailyScheduleId,
                activeStatuses
            );

        boolean hasDuplicate = existingRequests.stream()
            .filter(existing -> excludedRequestId == null || !existing.getId().equals(excludedRequestId))
            .findAny()
            .isPresent();

        if (hasDuplicate) {
            throw new DuplicateActiveRequestException();
        }
    }

    private void expireRequest(LessonExchangeRequest request) {
        request.expire();
        closeActiveProposals(request);
    }

    private void closeActiveProposals(LessonExchangeRequest request) {
        request.getProposals().stream()
            .filter(proposal -> proposal.getStatus() == LessonExchangeProposalStatus.ACTIVE)
            .forEach(LessonExchangeProposal::close);
    }

    private DailySchedule getTargetDailySchedule(
        Long requesterId,
        LocalDate lessonDate
    ) {
        return dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(requesterId, lessonDate);
    }

    private Specification<LessonExchangeRequest> buildListSpecification(
        Long requesterId,
        LessonExchangeRequestListRequest request
    ) {
        return Specification.allOf(
            hasStatusOrExcludeCancelled(request.getStatus()),
            matchesRequesterIfMine(requesterId, request.isMine()),
            containsKeyword(request.getKeyword())
        );
    }

    private Specification<LessonExchangeRequest> hasStatusOrExcludeCancelled(LessonExchangeRequestStatus status) {
        return (root, query, criteriaBuilder) -> status != null
            ? criteriaBuilder.equal(root.get("status"), status)
            : criteriaBuilder.notEqual(root.get("status"), LessonExchangeRequestStatus.CANCELLED);
    }

    private Specification<LessonExchangeRequest> matchesRequesterIfMine(Long requesterId, boolean mine) {
        return (root, query, criteriaBuilder) -> mine
            ? criteriaBuilder.equal(root.get("requestedBy").get("id"), requesterId)
            : null;
    }

    private Specification<LessonExchangeRequest> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }

            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("requestedBy").get("name")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("classroomNameSnapshot")), likeKeyword)
            );
        };
    }

}
