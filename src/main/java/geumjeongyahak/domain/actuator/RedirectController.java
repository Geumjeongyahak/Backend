package geumjeongyahak.domain.actuator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@Tag(name = "Redirect", description = "Redirect API")
public class RedirectController {

    @Operation(summary = "Redirect metrics to actuator metrics")
    @GetMapping("/metrics")
    public String redirectMetrics() {
        return "redirect:/actuator/metrics";
    }

    @Operation(summary = "루트 주소에서 관리자 로그인 주소로 리디렉션 합니다.")
    @GetMapping("/")
    public String redirectAdminLogin() {
        return "redirect:/admin/auth/login";
    }
}
