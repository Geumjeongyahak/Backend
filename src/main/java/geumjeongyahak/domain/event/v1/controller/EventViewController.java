package geumjeongyahak.domain.event.v1.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import java.time.LocalDate;
import java.time.LocalTime;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import geumjeongyahak.domain.event.service.EventAdminViewService;
import geumjeongyahak.domain.event.service.EventAdminViewService.EventFilter;
import geumjeongyahak.domain.event.v1.dto.request.CreateEventRequest;
import geumjeongyahak.domain.event.v1.dto.request.UpdateEventRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/event/events")
@RequiredArgsConstructor
public class EventViewController {

    private static final String EVENT_MANAGE_ACCESS = "hasRole('ADMIN') or hasAuthority('event:manage:*')";

    private final EventAdminViewService eventAdminViewService;

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @GetMapping
    public String events(
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate endDate,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        EventFilter filter = new EventFilter(startDate, endDate, page, size, sort);
        model.addAttribute("active", "events");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("eventsPage", eventAdminViewService.getEvents(filter));
        return "admin/event/events";
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @GetMapping("/new")
    public String newEvent(Model model, Authentication authentication) {
        addBaseAttributes(model, authentication);
        return "admin/event/events-form";
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @PostMapping
    public String createEvent(
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate eventDate,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes,
        Model model,
        Authentication authentication
    ) {
        CreateEventRequest request = new CreateEventRequest(
            title,
            description,
            eventDate,
            parseOptionalTime(startTime),
            parseOptionalTime(endTime)
        );
        try {
            Long eventId = eventAdminViewService.createEvent(userDetails.getUserId(), request);
            redirectAttributes.addFlashAttribute("message", "행사를 등록했습니다.");
            return "redirect:/admin/event/events/" + eventId + "/edit";
        } catch (BusinessException | IllegalArgumentException exception) {
            addBaseAttributes(model, authentication);
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("form", request);
            return "admin/event/events-form";
        }
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @GetMapping("/{eventId}/edit")
    public String editEvent(
        @PathVariable Long eventId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "events");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("event", eventAdminViewService.getEvent(eventId));
        return "admin/event/events-edit";
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @PostMapping("/{eventId}")
    public String updateEvent(
        @PathVariable Long eventId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam @DateTimeFormat(iso = DATE) LocalDate eventDate,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes,
        Model model,
        Authentication authentication
    ) {
        UpdateEventRequest request = new UpdateEventRequest(
            title,
            description,
            eventDate,
            parseOptionalTime(startTime),
            parseOptionalTime(endTime)
        );
        try {
            eventAdminViewService.updateEvent(userDetails.getUserId(), eventId, request);
            redirectAttributes.addFlashAttribute("message", "행사를 수정했습니다.");
            return "redirect:/admin/event/events/" + eventId + "/edit";
        } catch (BusinessException | IllegalArgumentException exception) {
            addBaseAttributes(model, authentication);
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("event", eventAdminViewService.getEvent(eventId));
            model.addAttribute("form", request);
            return "admin/event/events-edit";
        }
    }

    @PreAuthorize(EVENT_MANAGE_ACCESS)
    @PostMapping("/{eventId}/delete")
    public String deleteEvent(
        @PathVariable Long eventId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        eventAdminViewService.deleteEvent(userDetails.getUserId(), eventId);
        redirectAttributes.addFlashAttribute("message", "행사를 삭제했습니다.");
        return "redirect:/admin/event/events";
    }

    private void addBaseAttributes(Model model, Authentication authentication) {
        model.addAttribute("active", "events");
        model.addAttribute("adminName", authentication.getName());
    }

    private LocalTime parseOptionalTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value);
    }

}
