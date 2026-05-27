package geumjeongyahak.domain.event.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.event.entity.Event;
import geumjeongyahak.domain.event.exception.EventNotFoundException;
import geumjeongyahak.domain.event.repository.EventRepository;
import geumjeongyahak.domain.event.v1.dto.request.CreateEventRequest;
import geumjeongyahak.domain.event.v1.dto.request.EventSearchRequest;
import geumjeongyahak.domain.event.v1.dto.request.UpdateEventRequest;
import geumjeongyahak.domain.event.v1.dto.response.EventResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserProxyService userProxyService;

    public PaginationResponse<EventResponse> getEvents(EventSearchRequest request) {
        log.debug("행사 목록 조회 요청");
        DateRange dateRange = resolveDateRange(request);
        Page<Event> events = dateRange == null
            ? eventRepository.findAllByIsDeletedFalse(request.toRequest())
            : eventRepository.findAllByIsDeletedFalseAndEventDateBetween(
                dateRange.startDate(),
                dateRange.endDate(),
                request.toRequest()
            );
        return PaginationResponse.from(events, EventResponse::from);
    }

    public EventResponse getEvent(Long eventId) {
        log.debug("행사 상세 조회 요청 (eventId={})", eventId);
        return eventRepository.findByIdAndIsDeletedFalse(eventId)
            .map(EventResponse::from)
            .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Transactional
    public EventResponse createEvent(Long requesterId, CreateEventRequest request) {
        log.debug("행사 생성 요청 (requesterId={})", requesterId);
        User requester = userProxyService.getById(requesterId);
        Event event = new Event(
            request.title(),
            request.description(),
            request.eventDate(),
            request.startTime(),
            request.endTime(),
            requester
        );
        Event saved = eventRepository.save(event);
        log.debug("행사 생성 완료 (eventId={})", saved.getId());
        return EventResponse.from(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long requesterId, Long eventId, UpdateEventRequest request) {
        log.debug("행사 수정 요청 (requesterId={}, eventId={})", requesterId, eventId);
        User requester = userProxyService.getById(requesterId);
        Event event = eventRepository.findByIdAndIsDeletedFalse(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));

        event.update(
            request.title() != null ? request.title() : event.getTitle(),
            request.description() != null ? request.description() : event.getDescription(),
            request.eventDate() != null ? request.eventDate() : event.getEventDate(),
            request.startTime() != null ? request.startTime() : event.getStartTime(),
            request.endTime() != null ? request.endTime() : event.getEndTime(),
            requester
        );
        log.debug("행사 수정 완료 (eventId={})", eventId);
        return EventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long requesterId, Long eventId) {
        log.debug("행사 삭제 요청 (requesterId={}, eventId={})", requesterId, eventId);
        User requester = userProxyService.getById(requesterId);
        Event event = eventRepository.findByIdAndIsDeletedFalse(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        event.delete(requester);
        log.debug("행사 삭제 완료 (eventId={})", eventId);
    }

    private DateRange resolveDateRange(EventSearchRequest request) {
        boolean hasStart = request.getStartDate() != null;
        boolean hasEnd = request.getEndDate() != null;

        if (hasStart || hasEnd) {
            return new DateRange(request.getStartDate(), request.getEndDate());
        }

        return null;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
