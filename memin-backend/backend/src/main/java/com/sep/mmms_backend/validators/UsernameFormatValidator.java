package com.sep.mmms_backend.validators;


import com.sep.mmms_backend.entity.AppUser;
import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
import com.sep.mmms_backend.service.AppUserService;
import com.sep.mmms_backend.validators.annotations.UsernameFormat;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UsernameFormatValidator implements ConstraintValidator<UsernameFormat, String> {
    @Autowired
    AppUserService appUserService;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        if(username == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9_]");

        Matcher matcher = pattern.matcher(username);

        if (matcher.find()) {
            constraintValidatorContext.buildConstraintViolationWithTemplate(ValidationErrorMessages.INVALID_USERNAME_FORMAT).addConstraintViolation();
            return false;
        }
        return true;
    }



    @Override
    public void initialize(UsernameFormat constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
