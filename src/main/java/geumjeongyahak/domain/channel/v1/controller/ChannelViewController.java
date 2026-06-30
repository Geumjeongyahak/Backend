package geumjeongyahak.domain.channel.v1.controller;

import geumjeongyahak.domain.channel.service.ChannelAdminViewService;
import geumjeongyahak.domain.channel.service.ChannelAdminViewService.ChannelFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/channel/channels")
@RequiredArgsConstructor
public class ChannelViewController {

    private final ChannelAdminViewService channelAdminViewService;

    @GetMapping
    public String channels(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String channelType,
        @RequestParam(required = false) String bindingType,
        @RequestParam(required = false) String accessLevel,
        @RequestParam(required = false) Boolean allowGuestRead,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isDefault,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        ChannelFilter filter = new ChannelFilter(keyword, channelType, bindingType, accessLevel, allowGuestRead, isActive, isDefault, page, size, sort);
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("channelsPage", channelAdminViewService.getChannels(filter));
        model.addAttribute("channelTypes", channelAdminViewService.getChannelTypes());
        model.addAttribute("bindingTypes", channelAdminViewService.getBindingTypes());
        model.addAttribute("accessLevels", channelAdminViewService.getAccessLevels());
        return "admin/channel/channels";
    }

    @GetMapping("/new")
    public String newChannel(Model model, Authentication authentication) {
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("accessLevels", channelAdminViewService.getAccessLevels());
        return "admin/channel/channels-form";
    }

    @PostMapping
    public String createChannel(
        @RequestParam String name,
        @RequestParam(required = false) String description,
        @RequestParam String accessLevel,
        @RequestParam(required = false) Boolean allowGuestRead,
        @RequestParam(required = false) Boolean isDefault,
        @RequestParam(required = false) Boolean isActive,
        RedirectAttributes redirectAttributes
    ) {
        Long channelId = channelAdminViewService.createChannel(name, description, accessLevel, allowGuestRead, isDefault, isActive);
        redirectAttributes.addFlashAttribute("message", "채널을 생성했습니다.");
        return "redirect:/admin/channel/channels/" + channelId;
    }

    @GetMapping("/{channelId}")
    public String channelDetail(
        @PathVariable Long channelId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("channel", channelAdminViewService.getChannel(channelId));
        return "admin/channel/channels-detail";
    }

    @PostMapping("/{channelId}/delete")
    public String deleteChannel(
        @PathVariable Long channelId,
        RedirectAttributes redirectAttributes
    ) {
        channelAdminViewService.deleteChannel(channelId);
        redirectAttributes.addFlashAttribute("message", "채널을 삭제했습니다.");
        return "redirect:/admin/channel/channels";
    }
}
