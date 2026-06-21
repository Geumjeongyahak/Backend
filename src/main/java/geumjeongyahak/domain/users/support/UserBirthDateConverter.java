package geumjeongyahak.domain.users.support;

import geumjeongyahak.domain.users.exception.InvalidResidentRegistrationNumberPrefixException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class UserBirthDateConverter {

    private static final DateTimeFormatter PREFIX_FORMATTER =
        DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER =
        DateTimeFormatter.ofPattern("MMdd")
            .withResolverStyle(ResolverStyle.STRICT);
    private UserBirthDateConverter() {
    }

    public static String toResidentRegistrationNumberPrefix(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return birthDate.format(PREFIX_FORMATTER);
    }

    public static LocalDate toBirthDate(String residentRegistrationNumberPrefix) {
        return toBirthDate(residentRegistrationNumberPrefix, LocalDate.now());
    }

    static LocalDate toBirthDate(
        String residentRegistrationNumberPrefix,
        LocalDate referenceDate
    ) {
        if (residentRegistrationNumberPrefix == null) {
            return null;
        }
        validatePrefixFormat(residentRegistrationNumberPrefix);

        int shortYear = Integer.parseInt(residentRegistrationNumberPrefix.substring(0, 2));
        int month = Integer.parseInt(residentRegistrationNumberPrefix.substring(2, 4));
        int day = Integer.parseInt(residentRegistrationNumberPrefix.substring(4, 6));
        int currentCentury = referenceDate.getYear() / 100 * 100;

        LocalDate currentCenturyDate = createDate(currentCentury + shortYear, month, day);
        LocalDate birthDate = currentCenturyDate.isAfter(referenceDate)
            ? createDate(currentCentury + shortYear - 100, month, day)
            : currentCenturyDate;

        return birthDate;
    }

    private static void validatePrefixFormat(String residentRegistrationNumberPrefix) {
        if (!residentRegistrationNumberPrefix.matches("\\d{6}")) {
            throw new InvalidResidentRegistrationNumberPrefixException();
        }

        try {
            MONTH_DAY_FORMATTER.parse(residentRegistrationNumberPrefix.substring(2));
        } catch (DateTimeException exception) {
            throw new InvalidResidentRegistrationNumberPrefixException();
        }
    }

    private static LocalDate createDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException exception) {
            throw new InvalidResidentRegistrationNumberPrefixException();
        }
    }

}
