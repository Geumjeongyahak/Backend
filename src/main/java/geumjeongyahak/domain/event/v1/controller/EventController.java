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

    @Operation(
        summary = "행사 목록 조회",
        description = """
            행사 일정 목록을 페이지네이션으로 조회합니다.

            정책:
            - 인증 없이 접근할 수 있습니다.
            - startDate와 endDate를 함께 전달하면 해당 기간의 행사만 조회합니다.
            - startDate와 endDate를 모두 생략하면 전체 행사를 조회합니다.
            - 삭제된 행사는 조회되지 않습니다.
            - 기본 정렬은 eventDate ASC, startTime ASC, id ASC입니다.
            """
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<EventResponse>> getEvents(
        @ParameterObject @Valid EventSearchRequest request
    ) {
        log.debug("GET /api/v1/events");
        return ResponseEntity.ok(eventService.getEvents(request));
    }

    @Operation(
        summary = "행사 상세 조회",
        description = """
            행사 일정 상세 정보를 조회합니다.

            정책:
            - 인증 없이 접근할 수 있습니다.
            - 삭제된 행사는 조회되지 않습니다.
            """
    )
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        log.debug("GET /api/v1/events/{}", eventId);
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }
}
