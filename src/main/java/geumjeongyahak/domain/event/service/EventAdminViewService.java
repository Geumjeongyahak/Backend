package geumjeongyahak.domain.event.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.event.entity.Event;
import geumjeongyahak.domain.event.exception.EventFormValidationException;
import geumjeongyahak.domain.event.exception.EventNotFoundException;
import geumjeongyahak.domain.event.repository.EventRepository;
import geumjeongyahak.domain.event.v1.dto.request.CreateEventRequest;
import geumjeongyahak.domain.event.v1.dto.request.UpdateEventRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAdminViewService {

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final Validator validator;

    public AdminPage<AdminEventRow> getEvents(EventFilter filter) {
        List<AdminEventRow> rows = eventRepository.findAllByIsDeletedFalseOrderByEventDateAscStartTimeAscIdAsc()
            .stream()
            .filter(event -> matchesDateRange(event, filter.startDate(), filter.endDate()))
            .map(AdminEventRow::from)
            .toList();

        return AdminPage.from(sortEvents(rows, filter.sort()), filter.page(), filter.size());
    }

    public AdminEventDetail getEvent(Long eventId) {
        return eventRepository.findByIdAndIsDeletedFalse(eventId)
            .map(AdminEventDetail::from)
            .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Transactional
    public Long createEvent(Long requesterId, CreateEventRequest request) {
        validate(request);
        return eventService.createEvent(requesterId, request).id();
    }

    @Transactional
    public void updateEvent(Long requesterId, Long eventId, UpdateEventRequest request) {
        validate(request);
        eventService.updateEvent(requesterId, eventId, request);
    }

    @Transactional
    public void deleteEvent(Long requesterId, Long eventId) {
        eventService.deleteEvent(requesterId, eventId);
    }

    private boolean matchesDateRange(Event event, LocalDate startDate, LocalDate endDate) {
        LocalDate eventDate = event.getEventDate();
        if (startDate != null && eventDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !eventDate.isAfter(endDate);
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("행사 입력값이 올바르지 않습니다.");
            throw new EventFormValidationException(message);
        }
    }

    private List<AdminEventRow> sortEvents(List<AdminEventRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminEventRow::id, Comparator.nullsLast(Long::compareTo)),
            "eventDate", Comparator.comparing(AdminEventRow::eventDate, Comparator.nullsLast(LocalDate::compareTo)),
            "startTime", Comparator.comparing(AdminEventRow::startTime, Comparator.nullsLast(LocalTime::compareTo)),
            "title", Comparator.comparing(AdminEventRow::title, Comparator.nullsLast(String::compareToIgnoreCase)),
            "lastModifiedByName", Comparator.comparing(AdminEventRow::lastModifiedByName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "updatedAt", Comparator.comparing(AdminEventRow::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "eventDate,ASC;startTime,ASC;id,ASC");
    }

    public record EventFilter(
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer size,
        String sort
    ) {
    }

    public record AdminEventRow(
        Long id,
        String title,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String lastModifiedByName,
        LocalDateTime updatedAt
    ) {
        private static AdminEventRow from(Event event) {
            return new AdminEventRow(
                event.getId(),
                event.getTitle(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getUpdatedBy().getName(),
                event.getUpdatedAt()
            );
        }
    }

    public record AdminEventDetail(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String createdByName,
        String lastModifiedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        private static AdminEventDetail from(Event event) {
            return new AdminEventDetail(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCreatedBy().getName(),
                event.getUpdatedBy().getName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
            );
        }
    }
}
