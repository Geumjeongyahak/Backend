package geumjeongyahak.domain.meeting_record.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import geumjeongyahak.domain.meeting_record.service.MeetingRecordAdminViewService;
import geumjeongyahak.domain.meeting_record.service.MeetingRecordAdminViewService.MeetingRecordFilter;
import geumjeongyahak.domain.meeting_record.v1.dto.request.MeetingRecordForm;
import geumjeongyahak.domain.meeting_record.v1.dto.response.MeetingRecordDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/meeting-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MeetingRecordViewController {

    private final MeetingRecordAdminViewService meetingRecordAdminViewService;

    @GetMapping
    public String meetingRecords(
        @RequestParam(required = false) MeetingRecordStatus status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        MeetingRecordFilter filter = new MeetingRecordFilter(userDetails.getUserId(), status, keyword, page, size);
        addBaseModel(model, authentication);
        model.addAttribute("filter", filter);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", meetingRecordAdminViewService.getStatuses());
        model.addAttribute("meetingRecordsPage", meetingRecordAdminViewService.getMeetingRecords(filter));
        return "admin/meeting-record/meeting-records";
    }

    @GetMapping("/{recordId}")
    public String detail(
        @PathVariable Long recordId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        addBaseModel(model, authentication);
        model.addAttribute("record", meetingRecordAdminViewService.getMeetingRecord(userDetails.getUserId(), recordId));
        return "admin/meeting-record/meeting-records-detail";
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {
        addBaseModel(model, authentication);
        model.addAttribute("form", new MeetingRecordForm());
        model.addAttribute("statuses", meetingRecordAdminViewService.getStatuses());
        return "admin/meeting-record/meeting-records-form";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") MeetingRecordForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addBaseModel(model, authentication);
            model.addAttribute("statuses", meetingRecordAdminViewService.getStatuses());
            return "admin/meeting-record/meeting-records-form";
        }

        Long recordId = meetingRecordAdminViewService.createMeetingRecord(
            userDetails.getUserId(),
            form.getTitle(),
            form.getAgenda()
        );
        redirectAttributes.addFlashAttribute("message", "교학 회의록이 생성되었습니다.");
        return "redirect:/admin/meeting-records/" + recordId;
    }

    @GetMapping("/{recordId}/edit")
    public String editForm(
        @PathVariable Long recordId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication
    ) {
        MeetingRecordDetailResponse record = meetingRecordAdminViewService.getMeetingRecordForEdit(userDetails.getUserId(), recordId);
        MeetingRecordForm form = new MeetingRecordForm();
        form.setTitle(record.title());
        form.setAgenda(record.agenda());
        form.setDiscussion(record.discussion());
        form.setSuggestion(record.suggestion());
        form.setStatus(record.status());

        addBaseModel(model, authentication);
        model.addAttribute("record", record);
        model.addAttribute("form", form);
        model.addAttribute("statuses", meetingRecordAdminViewService.getStatuses());
        return "admin/meeting-record/meeting-records-edit";
    }

    @PostMapping("/{recordId}")
    public String update(
        @PathVariable Long recordId,
        @Valid @ModelAttribute("form") MeetingRecordForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addBaseModel(model, authentication);
            model.addAttribute("record", meetingRecordAdminViewService.getMeetingRecordForEdit(userDetails.getUserId(), recordId));
            model.addAttribute("statuses", meetingRecordAdminViewService.getStatuses());
            return "admin/meeting-record/meeting-records-edit";
        }

        meetingRecordAdminViewService.updateMeetingRecord(
            userDetails.getUserId(),
            recordId,
            form.getTitle(),
            form.getAgenda(),
            form.getDiscussion(),
            form.getSuggestion(),
            form.getStatus()
        );
        redirectAttributes.addFlashAttribute("message", "교학 회의록이 수정되었습니다.");
        return "redirect:/admin/meeting-records/" + recordId;
    }

    @PostMapping("/{recordId}/delete")
    public String delete(
        @PathVariable Long recordId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        meetingRecordAdminViewService.deleteMeetingRecord(userDetails.getUserId(), recordId);
        redirectAttributes.addFlashAttribute("message", "교학 회의록이 삭제되었습니다.");
        return "redirect:/admin/meeting-records";
    }

    private void addBaseModel(Model model, Authentication authentication) {
        model.addAttribute("active", "meetingRecords");
        model.addAttribute("adminName", authentication.getName());
    }
}
