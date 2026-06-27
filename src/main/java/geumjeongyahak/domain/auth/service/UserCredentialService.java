package geumjeongyahak.domain.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.exception.CredentialNotFoundException;
import geumjeongyahak.domain.auth.exception.DuplicateCredentialException;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserCredential getById(Long credentialId) {
        return userCredentialRepository.findById(credentialId)
            .orElseThrow(CredentialNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<UserCredential> getAllCredentialsByUserId(Long userId) {
        return userCredentialRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UserCredential getCredentialByUserIdAndProvider(Long userId, ProviderType provider) {
        return userCredentialRepository.findByUserIdAndProvider(userId, provider)
            .orElseThrow(CredentialNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public UserCredential getCredentialByCredentialEmailAndProvider(String email, ProviderType provider) {
        return userCredentialRepository.findByCredentialEmailAndProvider(email, provider)
            .orElseThrow(CredentialNotFoundException::new);
    }
    
    @Transactional(readOnly = true)
    public boolean hasCredentialForProvider(Long userId, ProviderType provider) {
        return userCredentialRepository.existsByUserIdAndProvider(userId, provider);
    }

    @Transactional(readOnly = true)
    public boolean existsByCredentialEmail(String email) {
        return userCredentialRepository.existsByCredentialEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByCredentialEmailAndProvider(String email, ProviderType provider) {
        return userCredentialRepository.existsByCredentialEmailAndProvider(email, provider);
    }

    @Transactional(readOnly = true)
    public boolean existsByProviderUserIdAndProvider(String providerUserId, ProviderType provider) {
        return userCredentialRepository.existsByProviderUserIdAndProvider(providerUserId, provider);
    }

    @Transactional(readOnly = true)
    public UserCredential getCredentialByProviderUserIdAndProvider(String providerUserId, ProviderType provider) {
        return userCredentialRepository.findByProviderUserIdAndProvider(providerUserId, provider)
            .orElseThrow(CredentialNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredential> findOptionalByCredentialEmailAndProvider(String email, ProviderType provider) {
        return userCredentialRepository.findByCredentialEmailAndProvider(email, provider);
    }

    @Transactional
    public UserCredential createLocalCredential(
        User user,
        String email,
        String password
    ) {
        return createLocalCredential(user, email, password, true);
    }

    @Transactional
    public UserCredential createLocalCredential(
        User user,
        String email,
        String password,
        boolean emailVerified
    ) {
        log.info("로컬 로그인 자격 증명 생성 요청 - userId: {}, email: {}", user.getId(), user.getEmail());

        if (hasCredentialForProvider(user.getId(), ProviderType.LOCAL)) {
            log.info("로컬 로그인 자격 증명 생성 실패 - 이미 로컬 계정으로 가입된 사용자: userId: {}, email: {}", user.getId(), user.getEmail());
            throw new DuplicateCredentialException(
                "이미 가입된 계정입니다."
            );
        }

        if (existsByCredentialEmailAndProvider(email, ProviderType.LOCAL)) {
            log.info("로컬 로그인 자격 증명 생성 실패 - 이미 가입된 로컬 이메일: email: {}", email);
            throw new DuplicateCredentialException();
        }

        UserCredential credential = UserCredential.local(
            user,
            email,
            passwordEncoder.encode(password),
            emailVerified
        );

        log.debug("로컬 로그인 자격 증명 생성 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
        return userCredentialRepository.save(credential);
    }

    @Transactional
    public UserCredential createGoogleCredential(
        User user,
        String providerUserId,
        String credentialEmail,
        boolean emailVerified
    ) {
        log.info("Google 로그인 자격 증명 생성 요청 - userId: {}, email: {}", user.getId(), user.getEmail());

        if (hasCredentialForProvider(user.getId(), ProviderType.GOOGLE)) {
            log.info("Google 로그인 자격 증명 생성 실패 - 이미 Google 계정으로 가입된 사용자: userId: {}, email: {}", user.getId(), user.getEmail());
            throw new DuplicateCredentialException(
                "이미 Google 계정으로 가입된 사용자입니다."
            );
        }

        if (existsByCredentialEmailAndProvider(credentialEmail, ProviderType.GOOGLE)) {
            log.info("Google 로그인 자격 증명 생성 실패 - 이미 가입된 Google 이메일: email: {}", credentialEmail);
            throw new DuplicateCredentialException();
        }

        if (existsByProviderUserIdAndProvider(providerUserId, ProviderType.GOOGLE)) {
            log.info("Google 로그인 자격 증명 생성 실패 - 이미 가입된 Google providerUserId: {}", providerUserId);
            throw new DuplicateCredentialException();
        }
        
        UserCredential credential = UserCredential.google(
            user,
            providerUserId,
            credentialEmail,
            emailVerified
        );
        log.debug("Google 로그인 자격 증명 생성 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
        return userCredentialRepository.save(credential);
    }

    @Transactional
    public void updateLocalPassword(User user, String newPasswordHash) {
        log.info("로컬 로그인 비밀번호 업데이트 요청 - userId: {}, email: {}", user.getId(), user.getEmail());
        UserCredential credential = userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL)
            .orElseThrow(() -> {
                log.info("로컬 로그인 비밀번호 업데이트 실패 - 로컬 계정이 존재하지 않음: userId: {}, email: {}", user.getId(), user.getEmail());
                throw new CredentialNotFoundException();
            });
        credential.changePassword(newPasswordHash);
        userCredentialRepository.save(credential);
        log.debug("로컬 로그인 비밀번호 업데이트 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
    }

    @Transactional
    public void updateLocalCredentialEmail(User user, String newEmail) {
        log.info("로컬 로그인 이메일 업데이트 요청 - userId: {}, email: {}", user.getId(), user.getEmail());
        UserCredential credential = userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL)
            .orElseThrow(CredentialNotFoundException::new);
        if (newEmail.equals(credential.getCredentialEmail())) {
            return;
        }
        if (existsByCredentialEmailAndProvider(newEmail, ProviderType.LOCAL)) {
            throw new DuplicateCredentialException("이미 가입된 계정입니다.");
        }
        credential.changeCredentialEmail(newEmail);
        userCredentialRepository.save(credential);
    }

    @Transactional
    public void clearPasswordResetTokensByUserId(Long userId) {
        List<UserCredential> credentials = userCredentialRepository.findAllByUserId(userId);
        credentials.forEach(UserCredential::clearPasswordResetToken);
        userCredentialRepository.saveAll(credentials);
        log.debug("사용자 비밀번호 재설정 토큰 정리 완료 - userId: {}", userId);
    }

    @Transactional
    public void deleteAllCredentials(User user) {
        log.info("사용자 자격 증명 삭제 요청 - userId: {}, email: {}", user.getId(), user.getEmail());
        userCredentialRepository.deleteAllByUserId(user.getId());
        log.debug("사용자 자격 증명 삭제 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
    }
}
