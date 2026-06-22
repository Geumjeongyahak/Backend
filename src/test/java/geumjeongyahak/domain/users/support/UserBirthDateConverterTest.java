package geumjeongyahak.domain.users.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geumjeongyahak.domain.users.exception.InvalidResidentRegistrationNumberPrefixException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserBirthDateConverterTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 6, 21);

    @Test
    void toResidentRegistrationNumberPrefix_convertsBirthDateToSixDigits() {
        String prefix = UserBirthDateConverter.toResidentRegistrationNumberPrefix(
            LocalDate.of(2000, 1, 1)
        );

        assertThat(prefix).isEqualTo("000101");
    }

    @Test
    void toBirthDate_restoresDateFromCurrentCenturyWhenNotInFuture() {
        LocalDate birthDate = UserBirthDateConverter.toBirthDate("000101", REFERENCE_DATE);

        assertThat(birthDate).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void toBirthDate_restoresDateFromPreviousCenturyWhenCurrentCenturyDateIsInFuture() {
        LocalDate birthDate = UserBirthDateConverter.toBirthDate("900101", REFERENCE_DATE);

        assertThat(birthDate).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void toBirthDate_usesPreviousCenturyForTomorrowShortDate() {
        LocalDate birthDate = UserBirthDateConverter.toBirthDate("260622", REFERENCE_DATE);

        assertThat(birthDate).isEqualTo(LocalDate.of(1926, 6, 22));
    }

    @Test
    void converter_returnsNullWhenSourceIsNull() {
        assertThat(UserBirthDateConverter.toBirthDate(null, REFERENCE_DATE)).isNull();
        assertThat(UserBirthDateConverter.toResidentRegistrationNumberPrefix(null))
            .isNull();
    }

    @Test
    void toBirthDate_rejectsNonNumericOrWrongLengthPrefix() {
        assertThatThrownBy(() -> UserBirthDateConverter.toBirthDate("00A101", REFERENCE_DATE))
            .isInstanceOf(InvalidResidentRegistrationNumberPrefixException.class)
            .hasMessage("주민등록번호 앞자리의 형식 또는 날짜가 올바르지 않습니다.");

        assertThatThrownBy(() -> UserBirthDateConverter.toBirthDate("00101", REFERENCE_DATE))
            .isInstanceOf(InvalidResidentRegistrationNumberPrefixException.class)
            .hasMessage("주민등록번호 앞자리의 형식 또는 날짜가 올바르지 않습니다.");
    }

    @Test
    void toBirthDate_rejectsInvalidCalendarDate() {
        assertThatThrownBy(() -> UserBirthDateConverter.toBirthDate("250229", REFERENCE_DATE))
            .isInstanceOf(InvalidResidentRegistrationNumberPrefixException.class)
            .hasMessage("주민등록번호 앞자리의 형식 또는 날짜가 올바르지 않습니다.");
    }
}
