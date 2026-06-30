package geumjeongyahak.common.security.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.department.service.DepartmentPermissionProxyService;
import geumjeongyahak.domain.users.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserCredentialRepository userCredentialRepository;
    private final DepartmentPermissionProxyService departmentPermissionProxyService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserCredential credential = userCredentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        return toUserDetails(credential);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        UserCredential credential = userCredentialRepository.findByUserIdAndProvider(userId, ProviderType.LOCAL)
            .or(() -> userCredentialRepository.findAllByUserId(userId).stream().findFirst())
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        return toUserDetails(credential);
    }

    private UserDetails toUserDetails(UserCredential credential) {
        User user = credential.getUser();
        if (user.isDeleted()) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + user.getId());
        }
        Collection<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(user.getRole().getAuthority());
        authorities.addAll(user.getPermissions()
            .stream()
            .map(permission -> new SimpleGrantedAuthority(permission.toAuthorityCode()))
            .collect(Collectors.toSet()));

        authorities.addAll(departmentPermissionProxyService.getEffectivePermissions(user)
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.toAuthorityCode()))
                .collect(Collectors.toSet()));

        return new CustomUserDetails(
            user.getId(),
            credential.getId(),
            credential.getCredentialEmail(),
            credential.getPasswordHash(),
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            authorities
        );
    }
}
