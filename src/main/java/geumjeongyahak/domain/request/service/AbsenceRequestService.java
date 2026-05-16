package geumjeongyahak.domain.request.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.request.entity.AbsenceRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.event.AbsenceApprovedEvent;
import geumjeongyahak.domain.request.exception.AbsenceRequest.DuplicateActiveAbsenceRequestException;
import geumjeongyahak.domain.request.exception.AbsenceRequest.InvalidAbsenceRequestExpiresInPastException;
import geumjeongyahak.domain.request.exception.RequestAlreadyProcessedException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.request.v1.dto.request.AbsenceRequestPaginationRequest;
import geumjeongyahak.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.AbsenceRequestResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceRequestService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;


    @Transactional
    public AbsenceRequestResponse createAbsenceRequest(Long requesterId, CreateAbsenceRequestRequest request) {
        log.debug(
            "결석 요청 생성 (requesterId={}, dailyScheduleId={})",
            requesterId,
            request.dailyScheduleId()
        );

        DailySchedule dailySchedule = dailyScheduleProxyService.getActiveById(request.dailyScheduleId());
        User requester = userProxyService.getById(requesterId);

        validateRequesterIsDailyScheduleTeacher(dailySchedule, requesterId);
        validateExpiresAtIsFuture(dailySchedule.getLessonDate().atStartOfDay());
        validateNoActiveAbsenceRequest(dailySchedule.getId(), requesterId);

        AbsenceRequest absenceRequest = new AbsenceRequest(dailySchedule, requester, request.reason());
        AbsenceRequest saved = absenceRequestRepository.save(absenceRequest);

        log.debug("결석 요청 생성 완료 (id={})", saved.getId());
        return AbsenceRequestResponse.from(saved);
    }

    public PaginationResponse<AbsenceRequestResponse> getAbsenceRequests(
        Long requesterId,
        boolean isAdmin,
        RequestStatus status,
        AbsenceRequestPaginationRequest pageRequest
    ) {
        log.debug("결석 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        Page<AbsenceRequest> page;
        if (status != null) {
            page = isAdmin
                ? absenceRequestRepository.findAllByStatus(status, pageRequest.toRequest())
                : absenceRequestRepository.findAllByStatusAndRequestedBy_Id(
                    status, requesterId, pageRequest.toRequest()
                );
        } else {
            page = isAdmin
                ? absenceRequestRepository.findAll(pageRequest.toRequest())
                : absenceRequestRepository.findAllByRequestedBy_Id(requesterId, pageRequest.toRequest());
        }

        return PaginationResponse.from(page, AbsenceRequestResponse::from);
    }

    public AbsenceRequestResponse getAbsenceRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("결석 요청 상세 조회 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse approveAbsenceRequest(Long approverId, Long requestId) {
        log.debug("결석 요청 승인 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        absenceRequest.approve(approver);
        eventPublisher.publish(new AbsenceApprovedEvent(
            absenceRequest.getId(),
            absenceRequest.getDailySchedule().getId()
        ));

        log.debug("결석 요청 승인 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public AbsenceRequestResponse rejectAbsenceRequest(Long approverId, Long requestId, String note) {
        log.debug("결석 요청 반려 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);
        absenceRequest.reject(approver, note);

        log.debug("결석 요청 반려 완료 (requestId={})", requestId);
        return AbsenceRequestResponse.from(absenceRequest);
    }

    @Transactional
    public void deleteAbsenceRequest(Long requesterId, Long requestId) {
        log.debug("결석 요청 취소 (requestId={})", requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        absenceRequest.cancel();
        log.debug("결석 요청 취소 완료 (requestId={})", requestId);
    }

    @Transactional
    public int expireExpiredAbsenceRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<AbsenceRequest> expiredRequests =
            absenceRequestRepository.findAllByStatusInAndExpiresAtBefore(
                List.of(RequestStatus.PENDING),
                now
            );

        expiredRequests.forEach(AbsenceRequest::expire);

        if (!expiredRequests.isEmpty()) {
            log.info("결석 요청 자동 만료 처리 완료 (count={}, expiredAt={})", expiredRequests.size(), now);
        }

        return expiredRequests.size();
    }

    private void validateRequesterIsDailyScheduleTeacher(DailySchedule dailySchedule, Long requesterId) {
        if (!dailySchedule.getTeacher().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
    }

    private void validateNoActiveAbsenceRequest(Long dailyScheduleId, Long requesterId) {
        boolean exists = absenceRequestRepository.existsByDailySchedule_IdAndRequestedBy_IdAndStatusIn(
            dailyScheduleId,
            requesterId,
            List.of(RequestStatus.PENDING, RequestStatus.APPROVED)
        );
        if (exists) {
            throw new DuplicateActiveAbsenceRequestException();
        }
    }

    private void validateExpiresAtIsFuture(LocalDateTime expiresAt) {
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new InvalidAbsenceRequestExpiresInPastException();
        }
    }
}
