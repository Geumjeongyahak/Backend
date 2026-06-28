package geumjeongyahak.domain.department.v1.controller;

import geumjeongyahak.domain.base.service.PermissionRegistryViewService;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import geumjeongyahak.domain.department.service.DepartmentAdminViewService;
import geumjeongyahak.domain.department.service.DepartmentAdminViewService.DepartmentFilter;
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
@RequestMapping("/admin/department/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    private final DepartmentAdminViewService departmentAdminViewService;
    private final PermissionRegistryViewService permissionRegistryViewService;

    @GetMapping
    public String departments(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String permissionCode,
        @RequestParam(required = false) Long minMemberCount,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        DepartmentFilter filter = new DepartmentFilter(keyword, permissionCode, minMemberCount, sort);
        model.addAttribute("active", "departments");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentAdminViewService.getDepartments(filter));
        model.addAttribute("permissionOptions", permissionRegistryViewService.getAssignablePermissions());
        return "admin/department/departments";
    }

    @GetMapping("/new")
    public String newDepartment(Model model, Authentication authentication) {
        model.addAttribute("active", "departments");
        model.addAttribute("adminName", authentication.getName());
        return "admin/department/departments-form";
    }

    @PostMapping
    public String createDepartment(
        @RequestParam String name,
        @RequestParam String description,
        RedirectAttributes redirectAttributes
    ) {
        Long departmentId = departmentAdminViewService.createDepartment(name, description);
        redirectAttributes.addFlashAttribute("message", "부서를 등록했습니다.");
        return "redirect:/admin/department/departments/" + departmentId;
    }

    @GetMapping("/{departmentId}")
    public String departmentDetail(
        @PathVariable Long departmentId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "departments");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("department", departmentAdminViewService.getDepartment(departmentId));
        model.addAttribute("permissionOptions", permissionRegistryViewService.getAssignablePermissions());
        model.addAttribute("permissionScopeOptions", permissionRegistryViewService.getAssignableScopes());
        return "admin/department/departments-detail";
    }

    @GetMapping("/{departmentId}/edit")
    public String editDepartment(
        @PathVariable Long departmentId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "departments");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("department", departmentAdminViewService.getDepartment(departmentId));
        return "admin/department/departments-edit";
    }

    @PostMapping("/{departmentId}")
    public String updateDepartment(
        @PathVariable Long departmentId,
        @RequestParam String name,
        @RequestParam String description,
        RedirectAttributes redirectAttributes
    ) {
        departmentAdminViewService.updateDepartment(departmentId, name, description);
        redirectAttributes.addFlashAttribute("message", "부서 정보를 수정했습니다.");
        return "redirect:/admin/department/departments/" + departmentId;
    }

    @PostMapping("/{departmentId}/permissions")
    public String addPermission(
        @PathVariable Long departmentId,
        @RequestParam(required = false) DepartmentRoleType roleType,
        @RequestParam String permissionKey,
        @RequestParam String scopeTarget,
        RedirectAttributes redirectAttributes
    ) {
        try {
            String permissionCode = permissionRegistryViewService.buildPermissionCode(permissionKey, scopeTarget);
            departmentAdminViewService.addPermission(departmentId, roleType, permissionCode);
            redirectAttributes.addFlashAttribute("message", "부서 권한을 추가했습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/department/departments/" + departmentId;
    }
}
