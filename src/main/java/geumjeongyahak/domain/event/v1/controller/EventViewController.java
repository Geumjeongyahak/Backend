package geumjeongyahak.domain.event.v1.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import geumjeongyahak.domain.event.service.EventAdminViewService;
import geumjeongyahak.domain.event.service.EventAdminViewService.EventFilter;
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
        model.addAttribute("active", "events");
        model.addAttribute("adminName", authentication.getName());
        return "admin/event/events-form";
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
}
