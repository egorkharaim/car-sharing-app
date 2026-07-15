package mate.academy.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String field;
    private String fieldMatch;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field firstField = value.getClass().getDeclaredField(field);
            Field secondField = value.getClass().getDeclaredField(fieldMatch);
            firstField.setAccessible(true);
            secondField.setAccessible(true);
            Object firstValue = firstField.get(value);
            Object secondValue = secondField.get(value);
            return firstValue != null && firstValue.equals(secondValue);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
