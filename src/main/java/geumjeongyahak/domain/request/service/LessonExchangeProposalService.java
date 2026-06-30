package geumjeongyahak.domain.request.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeProposal;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.event.DailyScheduleExchangeAcceptedEvent;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.exception.LessonExchangeProposal.*;
import geumjeongyahak.domain.request.exception.LessonExchangeRequest.RequestLessonsNotFoundException;
import geumjeongyahak.domain.request.exception.RequestForbiddenException;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeProposalRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeProposalRequest;
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

    private final LessonExchangeProposalRepository lessonExchangeProposalRepository;
    private final LessonExchangeRequestProxyService lessonExchangeRequestProxyService;
    private final DailyScheduleProxyService dailyScheduleProxyService;
    private final LessonProxyService lessonProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;

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

        LessonExchangeProposalType proposalType = resolveProposalType(request.lessonDate());

        String classroomName = null;
        DailySchedule proposalDailySchedule = null;
        List<Lesson> requestLessons = getRequestLessons(exchangeRequest);

        // 교환형 제안과 대체형 제안을 구분
        if (proposalType == LessonExchangeProposalType.EXCHANGE) {
            proposalDailySchedule = getProposalDailySchedule(proposerId, request.lessonDate());
            validateNoTimeOverlapWithRequest(
                exchangeRequest,
                proposalDailySchedule
            );
            classroomName = proposalDailySchedule.getClassroom().getName();
        }

        validateNoScheduleConflict(proposerId, exchangeRequest.getLessonDate(), requestLessons);

        User proposer = userProxyService.getById(proposerId);

        // 조회 시 lesson을 다시 따라가지 않고 당시 화면 값을 그대로 보여주기 위해 반 이름 snapshot을 저장
        LessonExchangeProposal proposal = new LessonExchangeProposal(
            exchangeRequest,
            proposer,
            proposalType,
            proposalDailySchedule,
            request.content(),
            classroomName
        );

        LessonExchangeProposal saved = lessonExchangeProposalRepository.save(proposal);

        log.debug("수업 교환 제안 생성 완료 (proposalId={}, requestId={}, proposerId={})",
            saved.getId(), requestId, proposerId);

        return LessonExchangeProposalResponse.from(saved);
    }

    public List<LessonExchangeProposalResponse> getLessonExchangeProposals(Long requestId) {
        log.debug("수업 교환 제안 목록 조회 (requestId={})", requestId);

        lessonExchangeRequestProxyService.getById(requestId);

        return lessonExchangeProposalRepository.findAllByRequest_IdAndStatusNotOrderByCreatedAtDesc(
                requestId,
                LessonExchangeProposalStatus.WITHDRAWN
            ).stream()
            .map(LessonExchangeProposalResponse::from)
            .toList();
    }

    @Transactional
    public LessonExchangeProposalResponse updateLessonExchangeProposal(
        Long proposerId,
        Long requestId,
        Long proposalId,
        UpdateLessonExchangeProposalRequest request
    ) {
        log.debug("수업 교환 제안 수정 (requestId={}, proposalId={}, proposerId={})", requestId, proposalId, proposerId);

        LessonExchangeRequest exchangeRequest = lessonExchangeRequestProxyService.getById(requestId);
        LessonExchangeProposal proposal = lessonExchangeProposalRepository.findByIdAndRequest_Id(proposalId, requestId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        validateRequestIsProposable(exchangeRequest);
        validateProposalOwnership(proposal, proposerId);
        validateProposalIsActive(proposal);
        // 수정은 기존 ACTIVE 제안 1건을 갱신하는 흐름이므로,
        // 생성 시 적용한 "요청당 제안자별 ACTIVE 제안 1건만 허용" 정책을 그대로 유지
        // 즉 여기서는 새 ACTIVE 제안을 추가하지 않으므로 중복 ACTIVE 제안 검증을 다시 수행하지 않음

        LessonExchangeProposalType proposalType = resolveProposalType(request.lessonDate());

        String classroomName = null;
        DailySchedule proposalDailySchedule = null;
        List<Lesson> requestLessons = getRequestLessons(exchangeRequest);

        if (proposalType == LessonExchangeProposalType.EXCHANGE) {
            proposalDailySchedule = getProposalDailySchedule(proposerId, request.lessonDate());
            validateNoTimeOverlapWithRequest(
                exchangeRequest,
                proposalDailySchedule
            );
            classroomName = proposalDailySchedule.getClassroom().getName();
        }

        validateNoScheduleConflict(proposerId, exchangeRequest.getLessonDate(), requestLessons);

        // 수정 이후에도 제안 화면에는 최신 수정 기준의 반 이름이 유지되도록 snapshot을 함께 갱신
        proposal.update(
            proposalType,
            proposalDailySchedule,
            request.content(),
            classroomName
        );

        log.debug("수업 교환 제안 수정 완료 (requestId={}, proposalId={}, proposerId={})", requestId, proposalId, proposerId);
        return LessonExchangeProposalResponse.from(proposal);
    }

    @Transactional
    public LessonExchangeProposalResponse acceptLessonExchangeProposal(
        Long requesterId,
        Long requestId,
        Long proposalId
    ) {
        log.debug("수업 교환 제안 수락 (requestId={}, proposalId={}, requesterId={})", requestId, proposalId, requesterId);

        LessonExchangeRequest exchangeRequest = lessonExchangeRequestProxyService.getById(requestId);
        LessonExchangeProposal proposal = lessonExchangeProposalRepository.findByIdAndRequest_Id(proposalId, requestId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        validateRequestOwnership(exchangeRequest, requesterId);
        validateRequestIsAcceptable(exchangeRequest);
        validateProposalIsActive(proposal);

        proposal.accept();
        exchangeRequest.complete();
        closeOtherActiveProposals(exchangeRequest, proposalId);

        if (proposal.getProposalType() == LessonExchangeProposalType.EXCHANGE) {
            publishExchangeAcceptedEvents(
                exchangeRequest.getDailySchedule(),
                proposal.getDailySchedule(),
                requesterId,
                proposal.getProposedBy().getId()
            );
        } else {
            publishSubstitutionAcceptedEvents(exchangeRequest.getDailySchedule(), proposal.getProposedBy().getId());
        }

        log.debug("수업 교환 제안 수락 완료 (requestId={}, proposalId={}, requesterId={})", requestId, proposalId, requesterId);
        return LessonExchangeProposalResponse.from(proposal);
    }

    @Transactional
    public LessonExchangeProposalResponse withdrawLessonExchangeProposal(
        Long proposerId,
        Long requestId,
        Long proposalId
    ) {
        log.debug("수업 교환 제안 철회 (requestId={}, proposalId={}, proposerId={})", requestId, proposalId, proposerId);

        lessonExchangeRequestProxyService.getById(requestId);
        LessonExchangeProposal proposal = lessonExchangeProposalRepository.findByIdAndRequest_Id(proposalId, requestId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        validateProposalOwnership(proposal, proposerId);
        validateProposalIsActive(proposal);

        proposal.withdraw();

        log.debug("수업 교환 제안 철회 완료 (requestId={}, proposalId={}, proposerId={})", requestId, proposalId, proposerId);
        return LessonExchangeProposalResponse.from(proposal);
    }

    private void validateRequestOwnership(LessonExchangeRequest request, Long requesterId) {
        if (!request.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }
    }

    // 수업 교환 요청이 수락 가능 상태인지 확인 (APPROVE 상태 - 관리자의 승인이 있어야 함)
    private void validateRequestIsAcceptable(LessonExchangeRequest request) {
        if (request.getStatus() != LessonExchangeRequestStatus.APPROVED) {
            throw new RequestNotAcceptableException();
        }

        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RequestExpiredForProposalException();
        }
    }

    // 수락하는 제안 이외의 제안들은 모두 CLOSED 상태로 바꿈
    private void closeOtherActiveProposals(LessonExchangeRequest request, Long acceptedProposalId) {
        request.getProposals().stream()
            .filter(proposal -> !proposal.getId().equals(acceptedProposalId))
            .filter(proposal -> proposal.getStatus() == LessonExchangeProposalStatus.ACTIVE)
            .forEach(LessonExchangeProposal::close);
    }

    // 수업 교환 시에 교사를 변경하는 이벤트 발행 (교체형 제안)
    private void publishExchangeAcceptedEvents(
        DailySchedule requestDailySchedule,
        DailySchedule proposalDailySchedule,
        Long requesterId,
        Long newTeacherId
    ) {
        eventPublisher.publish(new DailyScheduleExchangeAcceptedEvent(
            requestDailySchedule.getId(),
            newTeacherId,
            proposalDailySchedule.getLessonDate()
        ));
        eventPublisher.publish(new DailyScheduleExchangeAcceptedEvent(
            proposalDailySchedule.getId(),
            requesterId,
            requestDailySchedule.getLessonDate()
        ));
    }

    // 수업 교환 시에 교사를 변경하는 이벤트 발행 (대체형 제안)
    private void publishSubstitutionAcceptedEvents(
        DailySchedule requestDailySchedule,
        Long newTeacherId
    ) {
        eventPublisher.publish(new DailyScheduleExchangeAcceptedEvent(
            requestDailySchedule.getId(),
            newTeacherId,
            null
        ));
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

    // 하루 단위 교환형 제안은 요청 날짜와 같은 날짜로 생성/수정할 수 없음
    private void validateNoTimeOverlapWithRequest(
        LessonExchangeRequest exchangeRequest,
        DailySchedule proposalDailySchedule
    ) {
        if (exchangeRequest.getLessonDate().equals(proposalDailySchedule.getLessonDate())) {
            throw new ProposalTimeOverlapWithRequestException();
        }
    }

    // 수업 교환 요청의 수업들을 조회
    private List<Lesson> getRequestLessons(LessonExchangeRequest request) {
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(
            request.getDailySchedule().getClassroom().getId(),
            request.getDailySchedule().getLessonDate()
        );

        if (lessons.isEmpty()) {
            throw new RequestLessonsNotFoundException();
        }

        return lessons;
    }

    // 교환형 제안 대상 DailySchedule을 조회하고 제안자 담당 일정인지 확인
    private DailySchedule getProposalDailySchedule(Long proposerId, LocalDate lessonDate) {
        return dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(proposerId, lessonDate);
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

    private void validateProposalOwnership(LessonExchangeProposal proposal, Long proposerId) {
        if (!proposal.getProposedBy().getId().equals(proposerId)) {
            throw new RequestForbiddenException();
        }
    }

    private void validateProposalIsActive(LessonExchangeProposal proposal) {
        if (proposal.getStatus() != LessonExchangeProposalStatus.ACTIVE) {
            throw new InvalidProposalStatusException();
        }
    }

    // 제안 수업 유형을 판단하는 메서드(대체형/교환형)
    private LessonExchangeProposalType resolveProposalType(LocalDate lessonDate) {
        if (lessonDate == null) {
            return LessonExchangeProposalType.SUBSTITUTION;
        }

        return LessonExchangeProposalType.EXCHANGE;
    }
}
