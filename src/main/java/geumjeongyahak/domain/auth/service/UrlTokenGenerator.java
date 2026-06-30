package geumjeongyahak.domain.auth.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;

class UrlTokenGenerator implements Supplier<String> {
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String get() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
