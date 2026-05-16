package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public LessonExchangeRequestDetailResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug(
            "수업 교환 요청 생성 (requesterId={}, dailyScheduleId={})",
            requesterId,
            request.dailyScheduleId()
        );

        DailySchedule dailySchedule = getTargetDailySchedule(
            requesterId,
            request.dailyScheduleId()
        );

        validateNoActiveExchangeRequestExists(
            requesterId,
            dailySchedule.getId()
        );
        validateLessonWithPolicy(dailySchedule.getLessonDate());
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(dailySchedule.getLessonDate(), request.expiresAt());
        validateExpiresAtWithinPolicy(dailySchedule.getLessonDate(), request.expiresAt());

        User requester = userProxyService.getById(requesterId);

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            dailySchedule,
            requester,
            request.title(),
            dailySchedule.getClassroom().getName(),
            request.content(),
            request.expiresAt()
        );
        LessonExchangeRequest saved = lessonExchangeRequestRepository.save(exchangeRequest);

        log.debug("수업 교환 요청 생성 완료 (id={})", saved.getId());
        return LessonExchangeRequestDetailResponse.from(saved);
    }

    public PaginationResponse<LessonExchangeRequestSummaryResponse> getLessonExchangeRequests(
        Long requesterId, LessonExchangeRequestListRequest request
    ) {
        log.debug("수업 교환 요청 목록 조회 (status={}, mine={})", request.getStatus(), request.isMine());

        PageRequest pageRequest = request.toRequest();
        Page<LessonExchangeRequest> requests = request.getStatus() != null
            ? request.isMine()
                ? lessonExchangeRequestRepository.findAllByStatusAndRequestedBy_Id(
                    request.getStatus(), requesterId, pageRequest
                )
                : lessonExchangeRequestRepository.findAllByStatus(request.getStatus(), pageRequest)
            : request.isMine()
                ? lessonExchangeRequestRepository.findAllByRequestedBy_IdAndStatusNot(
                    requesterId,
                    LessonExchangeRequestStatus.CANCELLED,
                    pageRequest
                )
                : lessonExchangeRequestRepository.findAllByStatusNot(
                    LessonExchangeRequestStatus.CANCELLED,
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

        User approver = userProxyService.getById(approverId);
        exchangeRequest.approve(approver);

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

        DailySchedule dailySchedule = getTargetDailySchedule(
            requesterId,
            request.dailyScheduleId()
        );

        validateNoActiveExchangeRequestExists(
            requesterId,
            dailySchedule.getId(),
            exchangeRequest.getId()
        );
        validateLessonWithPolicy(dailySchedule.getLessonDate());
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(dailySchedule.getLessonDate(), request.expiresAt());
        validateExpiresAtWithinPolicy(dailySchedule.getLessonDate(), request.expiresAt());

        // 수정 이후에도 요청 화면에는 최신 수정 기준의 반 이름이 유지되도록 snapshot을 함께 갱신
        exchangeRequest.update(
            dailySchedule,
            request.title(),
            dailySchedule.getClassroom().getName(),
            request.content(),
            request.expiresAt()
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

        User approver = userProxyService.getById(approverId);
        exchangeRequest.reject(approver, note);

        log.debug("수업 교환 요청 반려 완료 (requestId={}, approverId={})", requestId, approverId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest);
    }

    @Transactional
    public int expireExpiredLessonExchangeRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<LessonExchangeRequest> expiredRequests =
            lessonExchangeRequestRepository.findAllByStatusInAndExpiresAtBefore(
                List.of(LessonExchangeRequestStatus.PENDING, LessonExchangeRequestStatus.APPROVED),
                now
            );

        expiredRequests.forEach(this::expireRequest);

        if (!expiredRequests.isEmpty()) {
            log.info("수업 교환 요청 자동 만료 처리 완료 (count={}, expiredAt={})", expiredRequests.size(), now);
        }

        return expiredRequests.size();
    }

    // 교환 대상 수업 정책 반영 여부 (현재 기준 4일 이후 수업부터 교환 요청 가능)
    private void validateLessonWithPolicy(LocalDate lessonDate) {
        LocalDate today = LocalDate.now();
        LocalDate earliestRequestableDate = today.plusDays(REQUEST_DEADLINE_DAY);

        if (lessonDate.isBefore(earliestRequestableDate)) {
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
    private void validateExpiresAtBeforeLessonDate(LocalDate lessonDate, LocalDateTime expiresAt) {
        LocalDateTime lessonStartBoundary = lessonDate.atStartOfDay();

        if (!expiresAt.isBefore(lessonStartBoundary)) {
            throw new InvalidRequestExpiresAfterLessonException();
        }
    }

    // 만료 정책 반영 여부 (만료 시각은 수업일 3일 전 23:59:59를 넘길 수 없음)
    private void validateExpiresAtWithinPolicy(LocalDate lessonDate, LocalDateTime expiresAt) {
        LocalDateTime maxAllowedExpiresAt = lessonDate
            .minusDays(EXPIRE_DEADLINE_DAY)
            .atTime(EXPIRE_DEADLINE_HOUR, EXPIRE_DEADLINE_MINUTE, EXPIRE_DEADLINE_SECOND);

        if (expiresAt.isAfter(maxAllowedExpiresAt)) {
            throw new InvalidRequestExpiresPolicyException();
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
        Long dailyScheduleId
    ) {
        DailySchedule dailySchedule = dailyScheduleProxyService.getActiveById(dailyScheduleId);
        if (!dailySchedule.getTeacher().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        return dailySchedule;
    }

}
