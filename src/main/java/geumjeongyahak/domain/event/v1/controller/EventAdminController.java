package geumjeongyahak.domain.event.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.event.service.EventService;
import geumjeongyahak.domain.event.v1.dto.request.CreateEventRequest;
import geumjeongyahak.domain.event.v1.dto.request.UpdateEventRequest;
import geumjeongyahak.domain.event.v1.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@Tag(name = "Event Admin", description = "행사 일정 관리자 API")
public class EventAdminController {

    private static final String EVENT_MANAGE_ACCESS = "hasRole('ADMIN') or hasAuthority('event:manage:*')";

    private final EventService eventService;

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @Operation(
        summary = "행사 등록",
        description = """
            행사 일정을 등록합니다.

            정책:
            - ADMIN 또는 event:manage:* 권한이 필요합니다.
            - 행사 날짜는 단일 날짜(eventDate)로 저장합니다.
            - startTime과 endTime은 둘 다 없거나 둘 다 있어야 합니다.
            - 생성자와 마지막 수정자는 현재 로그인 사용자로 저장됩니다.
            """
    )
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
        @Valid @RequestBody CreateEventRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/events");
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(eventService.createEvent(userDetails.getUserId(), request));
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @Operation(
        summary = "행사 수정",
        description = """
            행사 일정을 부분 수정합니다.

            정책:
            - ADMIN 또는 event:manage:* 권한이 필요합니다.
            - 전달한 필드만 수정합니다.
            - description은 빈 문자열로 수정할 수 있습니다.
            - startTime과 endTime은 둘 다 없거나 둘 다 있어야 합니다.
            - 마지막 수정자는 현재 로그인 사용자로 저장됩니다.
            """
    )
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
        @PathVariable Long eventId,
        @Valid @RequestBody UpdateEventRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/events/{}", eventId);
        return ResponseEntity.ok(eventService.updateEvent(userDetails.getUserId(), eventId, request));
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @Operation(
        summary = "행사 삭제",
        description = """
            행사 일정을 삭제합니다.

            정책:
            - ADMIN 또는 event:manage:* 권한이 필요합니다.
            - 물리 삭제하지 않고 isDeleted=true로 처리합니다.
            - 삭제된 행사는 목록/상세 조회에서 제외됩니다.
            """
    )
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
        @PathVariable Long eventId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/admin/events/{}", eventId);
        eventService.deleteEvent(userDetails.getUserId(), eventId);
        return ResponseEntity.noContent().build();
    }
}
