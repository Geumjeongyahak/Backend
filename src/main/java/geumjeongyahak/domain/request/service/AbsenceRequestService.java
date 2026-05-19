package geumjeongyahak.domain.request.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.notification.enums.PushRequestType;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
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
import geumjeongyahak.domain.request.v1.dto.request.UpdateAbsenceRequestRequest;
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
            "결석 요청 생성 (requesterId={}, lessonDate={})",
            requesterId,
            request.lessonDate()
        );

        DailySchedule dailySchedule = dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(
            requesterId,
            request.lessonDate()
        );
        User requester = userProxyService.getById(requesterId);

        validateRequesterIsDailyScheduleTeacher(dailySchedule, requesterId);
        validateExpiresAtIsFuture(dailySchedule.getLessonDate().atStartOfDay());
        validateNoActiveAbsenceRequest(dailySchedule.getId(), requesterId);

        AbsenceRequest absenceRequest = new AbsenceRequest(
            dailySchedule,
            requester,
            request.title(),
            request.reason()
        );
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

        Page<AbsenceRequest> page = absenceRequestRepository.findAll(
            buildListSpecification(requesterId, isAdmin, status, pageRequest),
            pageRequest.toRequest()
        );

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
    public AbsenceRequestResponse updateAbsenceRequest(
        Long requesterId,
        Long requestId,
        UpdateAbsenceRequestRequest request
    ) {
        log.debug("결석 요청 수정 (requesterId={}, requestId={})", requesterId, requestId);
        AbsenceRequest absenceRequest = absenceRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!absenceRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
        if (absenceRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        absenceRequest.update(request.title(), request.reason());

        log.debug("결석 요청 수정 완료 (requestId={})", requestId);
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
        eventPublisher.publish(RequestReviewedPushEvent.approved(
            absenceRequest.getRequestedBy().getId(),
            absenceRequest.getId(),
            PushRequestType.ABSENCE,
            approverId,
            "결석 요청이 승인되었습니다.",
            "결석 요청이 승인되어 해당 수업 출석이 공결 처리되었습니다.",
            null
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
        eventPublisher.publish(RequestReviewedPushEvent.rejected(
            absenceRequest.getRequestedBy().getId(),
            absenceRequest.getId(),
            PushRequestType.ABSENCE,
            approverId,
            "결석 요청이 반려되었습니다.",
            "결석 요청이 반려되었습니다. 반려 사유를 확인해주세요.",
            note
        ));

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

    private Specification<AbsenceRequest> buildListSpecification(
        Long requesterId,
        boolean canReadAll,
        RequestStatus status,
        AbsenceRequestPaginationRequest pageRequest
    ) {
        return Specification.allOf(
            hasStatus(status),
            matchesRequesterScope(requesterId, canReadAll),
            containsKeyword(pageRequest.getKeyword())
        );
    }

    private Specification<AbsenceRequest> hasStatus(RequestStatus status) {
        return (root, query, criteriaBuilder) ->
            status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<AbsenceRequest> matchesRequesterScope(Long requesterId, boolean canReadAll) {
        return (root, query, criteriaBuilder) -> {
            if (canReadAll) {
                return null;
            }
            return criteriaBuilder.equal(root.get("requestedBy").get("id"), requesterId);
        };
    }

    private Specification<AbsenceRequest> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }

            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("requestedBy").get("name")), likeKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("dailySchedule").get("classroom").get("name")), likeKeyword)
            );
        };
    }
}
