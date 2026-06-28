package geumjeongyahak.unit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.security.service.CustomUserDetailsService;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.department.service.DepartmentPermissionProxyService;
import geumjeongyahak.domain.users.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private DepartmentPermissionProxyService departmentPermissionProxyService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUserId_allowsGoogleOnlyUser() {
        Long userId = 1L;
        Long credentialId = 10L;
        User user = User.builder()
            .name("Google 사용자")
            .email("google@test.com")
            .role(RoleType.GUEST)
            .build();
        ReflectionTestUtils.setField(user, "id", userId);
        UserCredential credential = UserCredential.google(
            user,
            "google-sub",
            "google@test.com",
            true
        );
        ReflectionTestUtils.setField(credential, "id", credentialId);

        given(userCredentialRepository.findByUserIdAndProvider(userId, ProviderType.LOCAL))
            .willReturn(Optional.empty());
        given(userCredentialRepository.findAllByUserId(userId)).willReturn(List.of(credential));
        given(departmentPermissionProxyService.getEffectivePermissions(user)).willReturn(List.of());

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUserId(userId);

        assertThat(userDetails.getUserId()).isEqualTo(userId);
        assertThat(userDetails.getCredentialId()).isEqualTo(credentialId);
        assertThat(userDetails.getUsername()).isEqualTo("google@test.com");
    }
}
