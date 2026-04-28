package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.exception.LessonExchangeProposal.*;
import geumjeongyahak.domain.request.exception.LessonExchangeRequest.RequestLessonsNotFoundException;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeProposalRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeProposalResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonExchangeProposalService {

    private static final int FULL_DAY_START_PERIOD = 1;
    private static final int FULL_DAY_END_PERIOD = 3;
    private static final int SINGLE_CLASSROOM_COUNT = 1;

    private final LessonExchangeProposalRepository lessonExchangeProposalRepository;
    private final LessonExchangeRequestProxyService lessonExchangeRequestProxyService;
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public LessonExchangeProposalResponse createLessonExchangeProposal(
        Long proposerId,
        Long requestId,
        CreateLessonExchangeProposalRequest request
    ) {
        log.debug("수업 교환 제안 생성 (requestId={}, proposerId={})", requestId, proposerId);

        LessonExchangeRequest exchangeRequest = lessonExchangeRequestProxyService.getById(requestId);

        validateRequestIsProposable(exchangeRequest);
        validateNotOwnRequest(exchangeRequest, proposerId);
        validateNoDuplicateActiveProposal(requestId, proposerId);

        LessonExchangeProposalType proposalType = resolveProposalType(request);
        LessonExchangeScope proposalScope = resolveProposalScope(request);

        String classroomName = null;

        // 교환형 제안과 대체형 제안을 구분
        if (proposalType == LessonExchangeProposalType.EXCHANGE) {
            List<Lesson> proposalLessons = getProposalLessons(
                proposerId,
                request.lessonDate(),
                proposalScope,
                request.startPeriod(),
                request.endPeriod()
            );

            validateNoTimeOverlapWithRequest(exchangeRequest, request, proposalScope);
            validateLessonsBelongToSingleClassroom(proposalLessons);
            classroomName = resolveClassroomName(proposalLessons);
        } else {
            List<Lesson> requestLessons = getRequestLessons(exchangeRequest);

            validateNoScheduleConflict(
                proposerId,
                exchangeRequest.getLessonDate(),
                requestLessons
            );

            validateLessonsBelongToSingleClassroom(requestLessons);
            classroomName = resolveClassroomName(requestLessons);
        }

        User proposer = userProxyService.getById(proposerId);

        LessonExchangeProposal proposal = new LessonExchangeProposal(
            exchangeRequest,
            proposer,
            proposalType,
            proposalScope,
            request.lessonDate(),
            request.startPeriod(),
            request.endPeriod(),
            request.content()
        );

        LessonExchangeProposal saved = lessonExchangeProposalRepository.save(proposal);

        log.debug("수업 교환 제안 생성 완료 (proposalId={}, requestId={}, proposerId={})",
            saved.getId(), requestId, proposerId);

        return LessonExchangeProposalResponse.from(saved, classroomName);
    }

    public List<LessonExchangeProposalResponse> getLessonExchangeProposals(Long requestId) {
        log.debug("수업 교환 제안 목록 조회 (requestId={})", requestId);

        lessonExchangeRequestProxyService.getById(requestId);

        return lessonExchangeProposalRepository.findAllByRequest_IdOrderByCreatedAtDesc(requestId).stream()
            .map(proposal -> LessonExchangeProposalResponse.from(
                proposal,
                resolveProposalClassroomName(proposal)
            ))
            .toList();
    }

    // 수업 교환 요청이 제안 가능 상태인지 확인 (APPROVED / 만료 기간 전)
    private void validateRequestIsProposable(LessonExchangeRequest request) {
        if (request.getStatus() != LessonExchangeRequestStatus.APPROVED) {
            throw new RequestNotProposableException();
        }

        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RequestExpiredForProposalException();
        }
    }

    // 자기 자신의 수업 교환 요청에는 제안할 수 없음
    private void validateNotOwnRequest(LessonExchangeRequest request, Long proposerId) {
        if (request.getRequestedBy().getId().equals(proposerId)) {
            throw new CannotProposeToOwnRequestException();
        }
    }

    // 수업 교환 요청에 이미 제안을 한 경우에는 제안할 수 없음
    private void validateNoDuplicateActiveProposal(Long requestId, Long proposerId) {
        if (lessonExchangeProposalRepository.existsByRequest_IdAndProposedBy_IdAndStatus(
            requestId,
            proposerId,
            LessonExchangeProposalStatus.ACTIVE
        )) {
            throw new DuplicateActiveProposalException();
        }
    }

    // 교환형 제안일 때, 요청 수업과 제안 수업의 교시가 겹치지 않는지 검증
    private void validateNoTimeOverlapWithRequest(
        LessonExchangeRequest exchangeRequest,
        CreateLessonExchangeProposalRequest request,
        LessonExchangeScope proposalScope
    ) {
        if (!exchangeRequest.getLessonDate().equals(request.lessonDate())) {
            return;
        }

        int requestStartPeriod = resolveStartPeriod(
            exchangeRequest.getScope(),
            exchangeRequest.getStartPeriod()
        );
        int requestEndPeriod = resolveEndPeriod(
            exchangeRequest.getScope(),
            exchangeRequest.getEndPeriod()
        );

        int proposalStartPeriod = resolveStartPeriod(
            proposalScope,
            request.startPeriod()
        );
        int proposalEndPeriod = resolveEndPeriod(
            proposalScope,
            request.endPeriod()
        );

        boolean overlaps =
            requestStartPeriod <= proposalEndPeriod
                && requestEndPeriod >= proposalStartPeriod;

        if (overlaps) {
            throw new ProposalTimeOverlapWithRequestException();
        }
    }

    private int resolveStartPeriod(LessonExchangeScope scope, Integer startPeriod) {
        if (scope == LessonExchangeScope.FULL) {
            return FULL_DAY_START_PERIOD;
        }

        return startPeriod;
    }

    private int resolveEndPeriod(LessonExchangeScope scope, Integer endPeriod) {
        if (scope == LessonExchangeScope.FULL) {
            return FULL_DAY_END_PERIOD;
        }

        return endPeriod;
    }

    // 수업 교환 요청의 수업들을 조회
    private List<Lesson> getRequestLessons(LessonExchangeRequest request) {
        List<Lesson> lessons;

        if (request.getScope() == LessonExchangeScope.FULL) {
            lessons = lessonProxyService.getActiveLessonsByTeacherAndDate(
                request.getRequestedBy().getId(),
                request.getLessonDate()
            );
        } else {
            lessons = lessonProxyService.getActiveLessonsByTeacherAndDateAndPeriodBetween(
                    request.getRequestedBy().getId(),
                    request.getLessonDate(),
                    request.getStartPeriod(),
                    request.getEndPeriod()
            );
        }

        if (lessons.isEmpty()) {
            throw new RequestLessonsNotFoundException();
        }

        return lessons;
    }

    // 수업 교환 제안의 수업들을 조회
    private List<Lesson> getProposalLessons(
        Long proposerId,
        LocalDate lessonDate,
        LessonExchangeScope scope,
        Integer startPeriod,
        Integer endPeriod
    ) {
        if (scope == LessonExchangeScope.FULL) {
            List<Lesson> lessons = lessonProxyService.getActiveLessonsByTeacherAndDate(
                proposerId,
                lessonDate
            );

            if (lessons.isEmpty()) {
                throw new ProposalLessonsNotFoundException();
            }
            return lessons;
        }

        List<Lesson> lessons = lessonProxyService.getActiveLessonsByTeacherAndDateAndPeriodBetween(
            proposerId,
            lessonDate,
            startPeriod,
            endPeriod
        );

        int expectedCount = endPeriod - startPeriod + 1;
        if (lessons.size() != expectedCount) {
            throw new ProposalLessonsNotFoundException();
        }

        return lessons;
    }

    // 제안자가 수업 교환 요청의 기존 수업과 충돌하는 수업이 있는지 검증
    private void validateNoScheduleConflict(
        Long proposerId,
        LocalDate requestDate,
        List<Lesson> requestLessons
    ) {
        for (Lesson requestLesson : requestLessons) {
            boolean hasConflict = lessonProxyService.existsActiveLessonConflict(
                    proposerId,
                    requestDate,
                    requestLesson.getStartTime(),
                    requestLesson.getEndTime()
            );

            if (hasConflict) {
                throw new ProposalScheduleConflictException();
            }
        }
    }

    // 수업들이 하나의 반으로 구성되어 있는지 검증
    private void validateLessonsBelongToSingleClassroom(List<Lesson> lessons) {
        long classroomCount = lessons.stream()
                .map(lesson -> lesson.getSubject().getClassroom().getId())
                .distinct()
                .count();

        if (classroomCount > SINGLE_CLASSROOM_COUNT) {
            throw new MultipleClassroomsInLessonExchangeProposalException();
        }
    }

    // 수업의 반 이름을 가져오는 메서드
    private String resolveClassroomName(List<Lesson> lessons) {
        return lessons.stream()
            .map(lesson -> lesson.getSubject().getClassroom().getName())
            .findFirst()
            .orElse(null);
    }

    private String resolveProposalClassroomName(LessonExchangeProposal proposal) {
        if (proposal.getProposalType() == LessonExchangeProposalType.SUBSTITUTION) {
            return null;
        }

        return resolveClassroomName(getProposalLessons(
            proposal.getProposedBy().getId(),
            proposal.getLessonDate(),
            proposal.getProposalScope(),
            proposal.getStartPeriod(),
            proposal.getEndPeriod()
        ));
    }

    // 제안 수업 유형을 판단하는 메서드(대체형/교환형)
    private LessonExchangeProposalType resolveProposalType(
        CreateLessonExchangeProposalRequest request
    ) {
        if (request.lessonDate() == null) {
            return LessonExchangeProposalType.SUBSTITUTION;
        }

        return LessonExchangeProposalType.EXCHANGE;
    }

    // 제안 수업 범위를 판단하는 메서드(전체/부분)
    private LessonExchangeScope resolveProposalScope(
        CreateLessonExchangeProposalRequest request
    ) {
        if (request.lessonDate() == null) {
            return null;
        }

        if (request.startPeriod() == null && request.endPeriod() == null) {
            return LessonExchangeScope.FULL;
        }

        return LessonExchangeScope.PARTIAL;
    }
}