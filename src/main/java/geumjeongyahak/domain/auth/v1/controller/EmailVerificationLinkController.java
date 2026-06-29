package geumjeongyahak.domain.auth.v1.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class EmailVerificationLinkController {

    @GetMapping("/auth/email-verification")
    public void redirectToEmailVerificationApi(
            @RequestParam String email,
            @RequestParam("code") String verificationCode,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(UriComponentsBuilder
            .fromPath("/api/v1/auth/email-verification/confirm")
            .queryParam("email", email)
            .queryParam("code", verificationCode)
            .toUriString());
    }
}
