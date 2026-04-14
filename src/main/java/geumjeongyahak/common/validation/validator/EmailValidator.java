package geumjeongyahak.common.validation.validator;

import java.util.regex.Pattern;

import geumjeongyahak.common.validation.annotation.ValidEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_PATTERN);

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            // @NotNull @NotBlank should accept/reject null/empty independently. 
            // Usually validators return true for null to allow @NotNull to handle it, 
            // but for specific format @ValidEmail, we might want to be strict or loose.
            // Standard practice: Let @NotNull or @NotBlank handle existence. Return true if null.
            return true;
        }
        return PATTERN.matcher(email).matches();
    }
}
