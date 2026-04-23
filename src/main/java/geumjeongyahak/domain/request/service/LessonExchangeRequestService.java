package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.exception.DuplicateActiveRequestException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresAfterLessonException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresInPastException;
import geumjeongyahak.domain.request.exception.InvalidRequestExpiresPolicyException;
import geumjeongyahak.domain.request.exception.InvalidRequestLessonPolicyException;
import geumjeongyahak.domain.request.exception.MultipleClassroomsInLessonExchangeRequestException;
import geumjeongyahak.domain.request.exception.RequestAlreadyProcessedException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.exception.RequestNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestDetailResponse;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
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

    @Transactional
    public LessonExchangeRequestDetailResponse createLessonExchangeRequest(
        Long requesterId,
        CreateLessonExchangeRequestRequest request
    ) {
        log.debug("수업 교환 요청 생성 (requesterId={}, lessonDate={})", requesterId, request.lessonDate());

        List<Lesson> targetLessons = getTargetLessons(
            requesterId,
            request.lessonDate(),
            request.scope(),
            request.startPeriod(),
            request.endPeriod()
        );

        validateNoActiveExchangeRequestExists(
            requesterId,
            request.lessonDate(),
            request.scope(),
            request.startPeriod(),
            request.endPeriod()
        );
        validateLessonWithPolicy(request.lessonDate());
        validateExpiresAtIsFuture(request.expiresAt());
        validateExpiresAtBeforeLessonDate(request.lessonDate(), request.expiresAt());
        validateExpiresAtWithinPolicy(request.lessonDate(), request.expiresAt());

        User requester = userProxyService.getById(requesterId);

        LessonExchangeRequest exchangeRequest = new LessonExchangeRequest(
            requester,
            request.lessonDate(),
            request.title(),
            request.content(),
            request.scope(),
            request.startPeriod(),
            request.endPeriod(),
            request.expiresAt()
        );
        LessonExchangeRequest saved = lessonExchangeRequestRepository.save(exchangeRequest);

        String classroomName = resolveClassroomName(targetLessons);

        log.debug("수업 교환 요청 생성 완료 (id={})", saved.getId());
        return LessonExchangeRequestDetailResponse.from(saved, classroomName);
    }

    public List<LessonExchangeRequestSummaryResponse> getLessonExchangeRequests(
        Long requesterId, LessonExchangeRequestStatus status, boolean mine
    ) {
        log.debug("수업 교환 요청 목록 조회 (status={}, mine={})", status, mine);

        List<LessonExchangeRequest> requests;
        if (status != null) {
            requests = mine
                ? lessonExchangeRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                )
                : lessonExchangeRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = mine
                ? lessonExchangeRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId)
                : lessonExchangeRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        return requests.stream()
            .map(request -> {
                List<Lesson> targetLessons = getTargetLessons(
                    request.getRequestedBy().getId(),
                    request.getLessonDate(),
                    request.getScope(),
                    request.getStartPeriod(),
                    request.getEndPeriod()
                );
                String classroomName = resolveClassroomName(targetLessons);
                return LessonExchangeRequestSummaryResponse.from(request, classroomName);
            })
            .toList();
    }

    public LessonExchangeRequestDetailResponse getLessonExchangeRequest(
        Long requestId
    ) {
        log.debug("수업 교환 요청 상세 조회 (requestId={})", requestId);
        LessonExchangeRequest exchangeRequest = lessonExchangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        List<Lesson> targetLessons = getTargetLessons(
            exchangeRequest.getRequestedBy().getId(),
            exchangeRequest.getLessonDate(),
            exchangeRequest.getScope(),
            exchangeRequest.getStartPeriod(),
            exchangeRequest.getEndPeriod()
        );
        String classroomName = resolveClassroomName(targetLessons);

        return LessonExchangeRequestDetailResponse.from(exchangeRequest, classroomName);
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

        List<Lesson> targetLessons = getTargetLessons(
            exchangeRequest.getRequestedBy().getId(),
            exchangeRequest.getLessonDate(),
            exchangeRequest.getScope(),
            exchangeRequest.getStartPeriod(),
            exchangeRequest.getEndPeriod()
        );
        String classroomName = resolveClassroomName(targetLessons);

        log.debug("수업 교환 요청 승인 완료 (requestId={}, approverId={})", requestId, approverId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest, classroomName);
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

        List<Lesson> targetLessons = getTargetLessons(
            exchangeRequest.getRequestedBy().getId(),
            exchangeRequest.getLessonDate(),
            exchangeRequest.getScope(),
            exchangeRequest.getStartPeriod(),
            exchangeRequest.getEndPeriod()
        );
        String classroomName = resolveClassroomName(targetLessons);

        log.debug("수업 교환 요청 반려 완료 (requestId={}, approverId={})", requestId, approverId);
        return LessonExchangeRequestDetailResponse.from(exchangeRequest, classroomName);
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
    private void validateNoActiveExchangeRequestExists(
        Long requesterId,
        LocalDate lessonDate,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod
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

        // 기존 요청이 없으면 isOverlapping 메서드가 호출되지 않고 false 값이 됨
        boolean hasOverlap = existingRequests.stream()
            .anyMatch(existing
                -> isOverlapping(existing, scope, startPeriod, endPeriod));

        if (hasOverlap) {
            throw new DuplicateActiveRequestException();
        }
    }

    private boolean isOverlapping(
        LessonExchangeRequest existing,
        LessonExchangeScope newScope,
        Integer newStartPeriod,
        Integer newEndPeriod
    ) {
        // 해당 날짜에 이미 전체 교환 요청이 있는 경우
        if (existing.getScope() == LessonExchangeScope.FULL) {
            return true;
        }

        // 해당 날짜에 이미 부분 교환 요청이 있지만 새 교환 요청의 범위가 전체인 경우
        if (newScope == LessonExchangeScope.FULL) {
            return true;
        }

        // 기존의 부분 교환 요청과 새 교환 요청의 범위가 겹치는 경우
        return existing.getStartPeriod() <= newEndPeriod
            && existing.getEndPeriod() >= newStartPeriod;
    }

    private List<Lesson> getTargetLessons(
        Long requesterId,
        LocalDate lessonDate,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod
    ) {
        if (scope == LessonExchangeScope.FULL) {
            List<Lesson> lessons = lessonProxyService.getActiveLessonsByTeacherAndDate(
                requesterId,
                lessonDate
            );

            if (lessons.isEmpty()) {
                throw new RequestForbiddenException();
            }
            return lessons;
        }

        List<Lesson> lessons = lessonProxyService.getActiveLessonsByTeacherAndDateAndPeriodBetween(
            requesterId,
            lessonDate,
            startPeriod,
            endPeriod
        );

        int expectedCount = endPeriod - startPeriod + 1;
        if (lessons.size() != expectedCount) {
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
