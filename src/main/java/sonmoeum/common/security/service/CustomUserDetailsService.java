package sonmoeum.common.security.service;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Combine Role + Permissions into authorities
        Collection<GrantedAuthority> authorities = Stream.concat(
            Stream.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            user.getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.name()))
        ).collect(Collectors.toList());

        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            authorities
        );
    }
}
