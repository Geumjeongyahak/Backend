package sonmoeum.common.controller;

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
}
