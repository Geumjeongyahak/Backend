package geumjeongyahak.unit.validation;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.common.validation.annotation.ValidUserBirthDate;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserBirthDateValidatorTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validBirthDate_passesValidation() {
        var violations = validator.validate(new BirthDateHolder(LocalDate.now().minusYears(20)));

        assertThat(violations).isEmpty();
    }

    @Test
    void nullBirthDate_passesValidationForOptionalRequestFields() {
        var violations = validator.validate(new BirthDateHolder(null));

        assertThat(violations).isEmpty();
    }

    @Test
    void futureBirthDate_failsValidationWithSpecificMessage() {
        var violations = validator.validate(new BirthDateHolder(LocalDate.now().plusDays(1)));

        assertThat(violations)
            .singleElement()
            .extracting(violation -> violation.getMessage())
            .isEqualTo("생년월일은 미래일 수 없습니다.");
    }

    @Test
    void ageOneHundredOrOlder_failsValidationWithSpecificMessage() {
        var violations = validator.validate(new BirthDateHolder(LocalDate.now().minusYears(100)));

        assertThat(violations)
            .singleElement()
            .extracting(violation -> violation.getMessage())
            .isEqualTo("생년월일은 만 100세 미만이어야 합니다.");
    }

    private record BirthDateHolder(
        @ValidUserBirthDate
        LocalDate birthDate
    ) {
    }
}
