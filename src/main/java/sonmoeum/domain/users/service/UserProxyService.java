package sonmoeum.domain.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserProxyService {
    private final UserRepository userRepository;

    boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    User getReferenceById(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
