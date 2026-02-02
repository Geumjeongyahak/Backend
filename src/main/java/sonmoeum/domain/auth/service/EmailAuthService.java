package sonmoeum.domain.auth.service;

import java.util.List;

import sonmoeum.api.v1.auth.dto.request.EmailLoginRequest;
import sonmoeum.api.v1.auth.dto.request.EmailSignupRequest;
import sonmoeum.api.v1.users.dto.request.CreateEmailUserRequest;
import sonmoeum.api.v1.users.dto.response.UserResponse;
import sonmoeum.domain.users.service.UserCrudService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailAuthService {
    private final UserCrudService userCrudService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse signUp(EmailSignupRequest request) {
        return userCrudService.createUser(new CreateEmailUserRequest(
            request.name(),
            request.email(),
            request.password(),
            request.phoneNumber(),
            request.role(),
            List.of()
        ));
    }

    public void login(EmailLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UsernamePasswordAuthenticationToken token = 
            new UsernamePasswordAuthenticationToken(request.email(), request.password());
            
        Authentication authentication = authenticationManager.authenticate(token);
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
