package geumjeongyahak.domain.auth.service;

import java.util.List;

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
    public List<UserCredential> getAllCredentialsByUserId(Long userId) {
        return userCredentialRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasCredentialForProvider(Long userId, ProviderType provider) {
        return userCredentialRepository.existsByUserIdAndProvider(userId, provider);
    }

    @Transactional
    public UserCredential createLocalCredential(
        User user,
        String email,
        String password
    ) {
        log.info("로컬 로그인 자격 증명 생성 요청 - userId: {}, nickname: {}", user.getId(), user.getNickname());

        if (hasCredentialForProvider(user.getId(), ProviderType.LOCAL)) {
            log.info("로컬 로그인 자격 증명 생성 실패 - 이미 로컬 계정으로 가입된 사용자: userId: {}, nickname: {}", user.getId(), user.getNickname());
            throw new DuplicateCredentialException(
                "이미 가입된 계정입니다."
            );
        }
        if (userCredentialRepository.existsByCredentialEmailAndProvider(email, ProviderType.LOCAL)) {
            log.info("로컬 로그인 자격 증명 생성 실패 - 이미 가입된 로컬 계정: email: {}", email);
            throw new DuplicateCredentialException(
                "이미 가입된 계정입니다."
            );
        }

        UserCredential credential = UserCredential.local(
            user,
            email,
            passwordEncoder.encode(password),
            true
        );
        
        log.debug("로컬 로그인 자격 증명 생성 완료 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        return userCredentialRepository.save(credential);
    }

    @Transactional
    public UserCredential createGoogleCredential(
        User user,
        String providerUserId,
        String credentialEmail,
        boolean emailVerified
    ) {
        log.info("Google 로그인 자격 증명 생성 요청 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        if (hasCredentialForProvider(user.getId(), ProviderType.GOOGLE)) {
            log.info("Google 로그인 자격 증명 생성 실패 - 이미 Google 계정으로 가입된 사용자: userId: {}, nickname: {}", user.getId(), user.getNickname());
            throw new DuplicateCredentialException(
                "이미 Google 계정으로 가입된 사용자입니다."
            );
        }        

        if (userCredentialRepository.existsByCredentialEmailAndProvider(credentialEmail, ProviderType.GOOGLE)) {
            log.info("Google 로그인 자격 증명 생성 실패 - 이미 가입된 Google 계정: email: {}", credentialEmail);
            throw new DuplicateCredentialException(
                "이미 가입된 Google 계정입니다."
            );
        }
        
        UserCredential credential = UserCredential.google(
            user,
            providerUserId,
            credentialEmail,
            emailVerified
        );
        log.debug("Google 로그인 자격 증명 생성 완료 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        return userCredentialRepository.save(credential);
    }

    @Transactional
    public void updateLocalPassword(User user, String newPasswordHash) {
        log.info("로컬 로그인 비밀번호 업데이트 요청 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        UserCredential credential = userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL)
            .orElseThrow(() -> {
                log.info("로컬 로그인 비밀번호 업데이트 실패 - 로컬 계정이 존재하지 않음: userId: {}, nickname: {}", user.getId(), user.getNickname());
                throw new CredentialNotFoundException();
            });
        credential.setPasswordHash(newPasswordHash);
        userCredentialRepository.save(credential);
        log.debug("로컬 로그인 비밀번호 업데이트 완료 - userId: {}, nickname: {}", user.getId(), user.getNickname());
    }

    @Transactional
    public void updateLocalCredentialEmail(User user, String newEmail) {
        log.info("로컬 로그인 이메일 업데이트 요청 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        UserCredential credential = userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL)
            .orElseThrow(CredentialNotFoundException::new);

        if (newEmail.equals(credential.getCredentialEmail())) {
            return;
        }

        if (userCredentialRepository.existsByCredentialEmailAndProvider(newEmail, ProviderType.LOCAL)) {
            throw new DuplicateCredentialException("이미 가입된 계정입니다.");
        }

        credential.setCredentialEmail(newEmail);
        credential.setEmailVerified(false);
        userCredentialRepository.save(credential);
    }

    @Transactional
    public void deleteAllCredentials(User user) {
        log.info("사용자 자격 증명 삭제 요청 - userId: {}, nickname: {}", user.getId(), user.getNickname());
        userCredentialRepository.deleteAllByUserId(user.getId());
        log.debug("사용자 자격 증명 삭제 완료 - userId: {}, nickname: {}", user.getId(), user.getNickname());
    }
}
