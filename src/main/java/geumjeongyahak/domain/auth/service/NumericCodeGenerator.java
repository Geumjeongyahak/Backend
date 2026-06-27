package geumjeongyahak.domain.auth.service;

import java.security.SecureRandom;
import java.util.function.Supplier;

class NumericCodeGenerator implements Supplier<String> {

    private static final int CODE_BOUND = 1_000_000;
    private static final int CODE_DIGITS = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String get() {
        return String.format("%0" + CODE_DIGITS + "d", secureRandom.nextInt(CODE_BOUND));
    }
}
