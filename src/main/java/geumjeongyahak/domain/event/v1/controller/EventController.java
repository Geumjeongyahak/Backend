package geumjeongyahak.domain.event.v1.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.event.service.EventService;
import geumjeongyahak.domain.event.v1.dto.request.EventSearchRequest;
import geumjeongyahak.domain.event.v1.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "행사 일정 API")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "행사 목록 조회", description = "행사 일정 목록을 조회합니다. 인증 없이 접근할 수 있습니다.")
    @GetMapping
    public ResponseEntity<PaginationResponse<EventResponse>> getEvents(
        @ParameterObject @Valid EventSearchRequest request
    ) {
        log.debug("GET /api/v1/events");
        return ResponseEntity.ok(eventService.getEvents(request));
    }

    @Operation(summary = "행사 상세 조회", description = "행사 일정 상세 정보를 조회합니다. 인증 없이 접근할 수 있습니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        log.debug("GET /api/v1/events/{}", eventId);
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }
}
