package com.sep.mmms_backend.validators;

import com.sep.mmms_backend.exceptions.ExceptionMessages;
import com.sep.mmms_backend.exceptions.ValidationFailureException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

@Component
public class EntityValidator {
    private final Validator validator;
    EntityValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(Object object){
        BindingResult bindingResult = new BeanPropertyBindingResult(object, "object");
         validator.validate(object, bindingResult);
        if(bindingResult.hasErrors()) {
            throw new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, bindingResult);
        }
    }
}
