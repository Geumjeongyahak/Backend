package sonmoeum.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import sonmoeum.common.validation.annotation.ValidSortField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortFieldValidator implements ConstraintValidator<ValidSortField, List<String>> {
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private Map<String, Boolean> fieldCheck;

    @Override
    public void initialize(ValidSortField constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.fieldCheck = new HashMap<>();
        for (String field : constraintAnnotation.fields()) {
            fieldCheck.put(field, true);
        }
    }

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (value.size() % 2 == 1) {
            return false;
        }

        for (int i=0; i < value.size(); i+=2) {
            String field = value.get(i);
            String direction = value.get(i+1).toUpperCase();

            // 필드명이 유효한지, 중복된 필드명이 아닌지 확인
            if (!fieldCheck.containsKey(field) || !fieldCheck.get(field)) {
                return false;
            } else {
                fieldCheck.put(field, false);
            }
            // 정렬 방향이 유효한지 확인
            if (!direction.equals(ASC) && !direction.equals(DESC)) {
                return false;
            }
        }
        return true;
    }
}
