package geumjeongyahak.domain.users.v1.controller;

import geumjeongyahak.domain.base.service.PermissionRegistryViewService;
import geumjeongyahak.domain.users.service.UserAdminViewService;
import geumjeongyahak.domain.users.service.UserAdminViewService.UserFilter;
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
@RequestMapping("/admin/user/users")
@RequiredArgsConstructor
public class UserViewController {

    private final UserAdminViewService userAdminViewService;
    private final PermissionRegistryViewService permissionRegistryViewService;

    @GetMapping
    public String users(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String permissionCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        UserFilter filter = new UserFilter(keyword, role, departmentId, permissionCode, page, size, sort);
        model.addAttribute("active", "users");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("usersPage", userAdminViewService.getUsers(filter));
        model.addAttribute("departments", userAdminViewService.getDepartmentOptions());
        model.addAttribute("permissionOptions", permissionRegistryViewService.getGlobalPermissions());
        return "admin/user/users";
    }

    @GetMapping("/new")
    public String newUser(Model model, Authentication authentication) {
        model.addAttribute("active", "users");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("departments", userAdminViewService.getDepartmentOptions());
        return "admin/user/users-form";
    }

    @PostMapping
    public String createUser(
        @RequestParam String email,
        @RequestParam String nickname,
        @RequestParam String name,
        @RequestParam String password,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam String role,
        @RequestParam(required = false) Long departmentId,
        RedirectAttributes redirectAttributes
    ) {
        Long userId = userAdminViewService.createUser(email, nickname, name, password, phoneNumber, role, departmentId);
        redirectAttributes.addFlashAttribute("message", "사용자를 등록했습니다.");
        return "redirect:/admin/user/users/" + userId;
    }

    @GetMapping("/{userId}")
    public String userDetail(
        @PathVariable Long userId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "users");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("user", userAdminViewService.getUser(userId));
        model.addAttribute("departments", userAdminViewService.getDepartmentOptions());
        model.addAttribute("permissionOptions", permissionRegistryViewService.getGlobalPermissions());
        return "admin/user/users-detail";
    }

    @GetMapping("/{userId}/edit")
    public String editUser(
        @PathVariable Long userId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "users");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("user", userAdminViewService.getUser(userId));
        model.addAttribute("departments", userAdminViewService.getDepartmentOptions());
        return "admin/user/users-edit";
    }

    @PostMapping("/{userId}")
    public String updateUser(
        @PathVariable Long userId,
        @RequestParam String email,
        @RequestParam String nickname,
        @RequestParam String name,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam String role,
        @RequestParam(required = false) Long departmentId,
        RedirectAttributes redirectAttributes
    ) {
        userAdminViewService.updateUser(userId, email, nickname, name, phoneNumber, role, departmentId);
        redirectAttributes.addFlashAttribute("message", "사용자 정보를 수정했습니다.");
        return "redirect:/admin/user/users/" + userId;
    }

    @PostMapping("/{userId}/role")
    public String updateRole(
        @PathVariable Long userId,
        @RequestParam String role,
        RedirectAttributes redirectAttributes
    ) {
        userAdminViewService.updateRole(userId, role);
        redirectAttributes.addFlashAttribute("message", "사용자 역할을 변경했습니다.");
        return "redirect:/admin/user/users/" + userId;
    }

    @PostMapping("/{userId}/department")
    public String updateDepartment(
        @PathVariable Long userId,
        @RequestParam Long departmentId,
        RedirectAttributes redirectAttributes
    ) {
        userAdminViewService.updateDepartment(userId, departmentId);
        redirectAttributes.addFlashAttribute("message", "사용자 소속 부서를 변경했습니다.");
        return "redirect:/admin/user/users/" + userId;
    }

    @PostMapping("/{userId}/permissions")
    public String addPermission(
        @PathVariable Long userId,
        @RequestParam String permissionCode,
        RedirectAttributes redirectAttributes
    ) {
        userAdminViewService.addPermission(userId, permissionCode);
        redirectAttributes.addFlashAttribute("message", "사용자 직접 권한을 추가했습니다.");
        return "redirect:/admin/user/users/" + userId;
    }
}
