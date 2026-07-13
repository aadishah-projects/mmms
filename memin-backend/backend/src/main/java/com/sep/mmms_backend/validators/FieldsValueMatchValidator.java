package com.sep.mmms_backend.validators;

import com.sep.mmms_backend.validators.annotations.FieldsValueMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class FieldsValueMatchValidator implements ConstraintValidator<FieldsValueMatch, Object> {
    public String field;
    public String fieldMatch;
    public String message;

    @Override
    public void initialize(FieldsValueMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String fieldValue =(String) new BeanWrapperImpl(value).getPropertyValue(field);
        String fieldMatchValue =(String) new BeanWrapperImpl(value).getPropertyValue(fieldMatch);

        if (fieldValue != null && fieldValue.equals(fieldMatchValue)) {
            return true;
        } else {
            context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
            return false;
        }
    }
}
