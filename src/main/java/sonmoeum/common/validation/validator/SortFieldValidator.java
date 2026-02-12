package sonmoeum.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import sonmoeum.common.validation.annotation.ValidSortField;

import java.util.*;

public class SortFieldValidator implements ConstraintValidator<ValidSortField, String> {
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";

    private Set<String> allowedFields;

    @Override
    public void initialize(ValidSortField constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        allowedFields = new HashSet<>(Arrays.asList(constraintAnnotation.fields()));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        Set<String> seenFields = new HashSet<>();

        String[] sorts = value.split(";");
        for (String sort : sorts) {
            if (sort.isEmpty()) {
                continue;
            }
            String[] parts = sort.split(",");
            if (parts.length != 2) {
                return false;
            }
            String field = parts[0].trim();
            String direction = parts[1].trim().toUpperCase();
            if (!allowedFields.contains(field) || seenFields.contains(field) ||
                (!direction.equals(ASC) && !direction.equals(DESC))) {
                return false;
            }
            seenFields.add(field);
        }
        return true;
    }
}
