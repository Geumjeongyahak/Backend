package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
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
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public LessonExchangeRequestDetailResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 생성 (requesterId={}, lessonDate={})", requesterId, request.lessonDate());

        List<Lesson> targetLessons = getTargetLessons(
            requesterId,
            request.lessonDate()
        );

        validateNoActiveExchangeRequestExists(
            requesterId,
            request.lessonDate()
        );
        validateLessonWithPolicy(request.lessonDate());
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(request.lessonDate(), request.expiresAt());
        validateExpiresAtWithinPolicy(request.lessonDate(), request.expiresAt());

        User requester = userProxyService.getById(requesterId);
        // 조회 시 lesson을 다시 따라가지 않고 당시 화면 값을 그대로 보여주기 위해 반 이름 snapshot을 저장
        String classroomName = resolveClassroomName(targetLessons);

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            requester,
            request.lessonDate(),
            request.title(),
            classroomName,
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

        List<Lesson> targetLessons = getTargetLessons(
            requesterId,
            request.lessonDate()
        );

        validateNoActiveExchangeRequestExists(
            requesterId,
            request.lessonDate(),
            exchangeRequest.getId()
        );
        validateLessonWithPolicy(request.lessonDate());
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(request.lessonDate(), request.expiresAt());
        validateExpiresAtWithinPolicy(request.lessonDate(), request.expiresAt());

        // 수정 이후에도 요청 화면에는 최신 수정 기준의 반 이름이 유지되도록 snapshot을 함께 갱신
        exchangeRequest.update(
            request.lessonDate(),
            request.title(),
            resolveClassroomName(targetLessons),
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
    private void validateNoActiveExchangeRequestExists(
        Long requesterId,
        LocalDate lessonDate
    ) {
        validateNoActiveExchangeRequestExists(requesterId, lessonDate, null);
    }

    // 수정 시에는 현재 수정 중인 자기 자신 요청은 중복 검사 대상에서 제외해야 하기 때문에 excludedRequestId 파라미터를 따로 받음
    private void validateNoActiveExchangeRequestExists(
        Long requesterId,
        LocalDate lessonDate,
        Long excludedRequestId
    ) {
        List<LessonExchangeRequestStatus> activeStatuses = List.of(
            LessonExchangeRequestStatus.PENDING,
            LessonExchangeRequestStatus.APPROVED
        );

        List<LessonExchangeRequest> existingRequests =
            lessonExchangeRequestRepository.findAllByRequestedBy_IdAndLessonDateAndStatusIn(
                requesterId,
                lessonDate,
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

    private List<Lesson> getTargetLessons(
        Long requesterId,
        LocalDate lessonDate
    ) {
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByTeacherAndDate(
            requesterId,
            lessonDate
        );

        if (lessons.isEmpty()) {
            throw new RequestForbiddenException();
        }

        return lessons;
    }

    private String resolveClassroomName(List<Lesson> lessons) {
        List<String> classroomNames = lessons.stream()
            .map(lesson -> lesson.getSubject().getClassroom().getName())
            .distinct()
            .toList();

        if (classroomNames.isEmpty()) {
            return null;
        }

        if (classroomNames.size() > 1) {
            throw new MultipleClassroomsInLessonExchangeRequestException();
        }

        return classroomNames.get(0);
    }

}
