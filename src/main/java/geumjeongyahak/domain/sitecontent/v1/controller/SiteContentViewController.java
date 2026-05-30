package geumjeongyahak.domain.sitecontent.v1.controller;

import geumjeongyahak.domain.sitecontent.enums.SiteContentGroup;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.sitecontent.service.SiteContentService;
import geumjeongyahak.domain.sitecontent.service.SiteHistoryService;
import geumjeongyahak.domain.sitecontent.v1.dto.request.SiteHistoryPhotoRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/site-content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SiteContentViewController {

    private final SiteContentService siteContentService;
    private final SiteHistoryService siteHistoryService;
    private final ImageUploadService imageUploadService;

    @GetMapping("/contents")
    public String contents(Model model, Authentication authentication) {
        addCommon(model, authentication, "site-content");
        model.addAttribute("contents", siteContentService.getAdminContents());
        return "admin/site-content/contents";
    }

    @GetMapping("/contents/new")
    public String newContent(Model model, Authentication authentication) {
        addCommon(model, authentication, "site-content");
        addContentOptions(model);
        return "admin/site-content/content-form";
    }

    @PostMapping("/contents")
    public String createContent(
        @RequestParam SiteContentType contentType,
        @RequestParam(required = false) Long refId,
        @RequestParam String title,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) SiteContentGroup group,
        @RequestParam(required = false) Integer sortOrder,
        @RequestParam(required = false) String itemsText,
        RedirectAttributes redirectAttributes
    ) {
        Long contentId = siteContentService.createAdminContent(
            contentType, refId, title, emptyToNull(name), group, sortOrder, parseLines(itemsText));
        redirectAttributes.addFlashAttribute("message", "사이트 콘텐츠를 등록했습니다.");
        return "redirect:/admin/site-content/contents/" + contentId + "/edit";
    }

    @GetMapping("/contents/{contentId}/edit")
    public String editContent(@PathVariable Long contentId, Model model, Authentication authentication) {
        addCommon(model, authentication, "site-content");
        addContentOptions(model);
        model.addAttribute("content", siteContentService.getAdminContent(contentId));
        return "admin/site-content/content-edit";
    }

    @PostMapping("/contents/{contentId}")
    public String updateContent(
        @PathVariable Long contentId,
        @RequestParam SiteContentType contentType,
        @RequestParam(required = false) Long refId,
        @RequestParam String title,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) SiteContentGroup group,
        @RequestParam(required = false) Integer sortOrder,
        @RequestParam(required = false) String itemsText,
        RedirectAttributes redirectAttributes
    ) {
        siteContentService.updateAdminContent(
            contentId, contentType, refId, title, emptyToNull(name), group, sortOrder, parseLines(itemsText));
        redirectAttributes.addFlashAttribute("message", "사이트 콘텐츠를 수정했습니다.");
        return "redirect:/admin/site-content/contents";
    }

    @PostMapping("/contents/{contentId}/delete")
    public String deleteContent(@PathVariable Long contentId, RedirectAttributes redirectAttributes) {
        siteContentService.deleteAdminContent(contentId);
        redirectAttributes.addFlashAttribute("message", "사이트 콘텐츠를 삭제했습니다.");
        return "redirect:/admin/site-content/contents";
    }

    @GetMapping("/history")
    public String histories(Model model, Authentication authentication) {
        addCommon(model, authentication, "site-history");
        model.addAttribute("histories", siteHistoryService.getAdminHistories());
        return "admin/site-content/history";
    }

    @GetMapping("/history/new")
    public String newHistory(Model model, Authentication authentication) {
        addCommon(model, authentication, "site-history");
        return "admin/site-content/history-form";
    }

    @PostMapping("/history")
    public String createHistory(
        @RequestParam String title,
        @RequestParam(required = false) String detail,
        @RequestParam(required = false) String linkLabel,
        @RequestParam(required = false) String linkHref,
        @RequestParam(required = false) Integer sortOrder,
        @RequestParam(required = false) String photosText,
        @RequestParam(required = false) MultipartFile photoFile,
        @RequestParam(required = false) String photoAlt,
        RedirectAttributes redirectAttributes
    ) {
        siteHistoryService.createAdminHistory(
            title,
            emptyToNull(detail),
            emptyToNull(linkLabel),
            emptyToNull(linkHref),
            sortOrder,
            parsePhotos(photosText, photoFile, photoAlt)
        );
        redirectAttributes.addFlashAttribute("message", "연혁을 등록했습니다.");
        return "redirect:/admin/site-content/history";
    }

    @GetMapping("/history/{historyId}/edit")
    public String editHistory(@PathVariable Long historyId, Model model, Authentication authentication) {
        addCommon(model, authentication, "site-history");
        model.addAttribute("history", siteHistoryService.getAdminHistory(historyId));
        return "admin/site-content/history-edit";
    }

    @PostMapping("/history/{historyId}")
    public String updateHistory(
        @PathVariable Long historyId,
        @RequestParam String title,
        @RequestParam(required = false) String detail,
        @RequestParam(required = false) String linkLabel,
        @RequestParam(required = false) String linkHref,
        @RequestParam(required = false) Integer sortOrder,
        @RequestParam(required = false) String photosText,
        @RequestParam(required = false) MultipartFile photoFile,
        @RequestParam(required = false) String photoAlt,
        RedirectAttributes redirectAttributes
    ) {
        siteHistoryService.updateAdminHistory(
            historyId,
            title,
            emptyToNull(detail),
            emptyToNull(linkLabel),
            emptyToNull(linkHref),
            sortOrder,
            parsePhotos(photosText, photoFile, photoAlt)
        );
        redirectAttributes.addFlashAttribute("message", "연혁을 수정했습니다.");
        return "redirect:/admin/site-content/history";
    }

    @PostMapping("/history/{historyId}/delete")
    public String deleteHistory(@PathVariable Long historyId, RedirectAttributes redirectAttributes) {
        siteHistoryService.deleteHistory(historyId);
        redirectAttributes.addFlashAttribute("message", "연혁을 삭제했습니다.");
        return "redirect:/admin/site-content/history";
    }

    private void addCommon(Model model, Authentication authentication, String active) {
        model.addAttribute("active", active);
        model.addAttribute("adminName", authentication.getName());
    }

    private void addContentOptions(Model model) {
        model.addAttribute("contentTypes", SiteContentType.values());
        model.addAttribute("groups", SiteContentGroup.values());
    }

    private List<String> parseLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
    }

    private List<SiteHistoryPhotoRequest> parsePhotos(String text, MultipartFile photoFile, String photoAlt) {
        List<SiteHistoryPhotoRequest> photos = text == null || text.isBlank()
            ? new java.util.ArrayList<>()
            : Arrays.stream(text.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .map(line -> {
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 3 && isUuid(parts[0].trim())) {
                    return new SiteHistoryPhotoRequest(null, UUID.fromString(parts[0].trim()), parts[1].trim(), emptyToNull(parts[2].trim()));
                }
                String alt = parts.length > 1 ? emptyToNull(parts[1].trim()) : null;
                return new SiteHistoryPhotoRequest(null, null, parts[0].trim(), alt);
            })
            .toList();
        photos = new java.util.ArrayList<>(photos);
        if (photoFile != null && !photoFile.isEmpty()) {
            FileUploadResponse uploaded = imageUploadService.uploadSiteContentImage(photoFile);
            photos.add(new SiteHistoryPhotoRequest(null, uploaded.fileId(), uploaded.url(), emptyToNull(photoAlt)));
        }
        return photos;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
