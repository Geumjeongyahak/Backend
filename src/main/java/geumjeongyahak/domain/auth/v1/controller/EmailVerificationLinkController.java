package geumjeongyahak.domain.auth.v1.controller;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.mail.MailProperties;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class EmailVerificationLinkController {
    private final MailProperties mailProperties;
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/auth/email-verification")
    public void redirectToEmailVerificationApi(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            @RequestParam(value = "code", required = false) String verificationCode,
            HttpServletResponse response
    ) throws IOException {
        String status = "success";
        String errorCode = null;
        try {
            if (token != null && !token.isBlank()) {
                emailVerificationService.confirmByToken(token);
            } else if (email != null && !email.isBlank() && verificationCode != null && !verificationCode.isBlank()) {
                emailVerificationService.confirm(email, verificationCode);
            } else {
                status = "invalid";
                errorCode = CommonErrorCode.MISSING_REQUIRED_FIELD.getCode();
            }
        } catch (BusinessException exception) {
            status = AuthErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED.getCode().equals(exception.getCode())
                ? "expired"
                : "invalid";
            errorCode = exception.getCode();
        }

        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(mailProperties.frontendBaseUrl())
            .replacePath("/auth/email-verification")
            .queryParam("status", status);
        if (errorCode != null) {
            builder.queryParam("errorCode", errorCode);
        }
        response.sendRedirect(builder.toUriString());
    }
}
