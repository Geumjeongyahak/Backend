package sonmoeum.common.security.service;

import java.util.Collection;
import java.util.stream.Collectors;

import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(userName)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userName));

        // Combine Role + Permissions into authorities
        Collection<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> role.getRoleType().getAuthority())
                .collect(Collectors.toSet());

        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            authorities
        );
    }
}
