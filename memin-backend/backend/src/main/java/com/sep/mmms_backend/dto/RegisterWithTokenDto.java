package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.global_constants.ValidationErrorMessages;
import com.sep.mmms_backend.validators.annotations.FieldsValueMatch;
import com.sep.mmms_backend.validators.annotations.UsernameFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FieldsValueMatch(
        field = "password",
        fieldMatch = "confirmPassword",
        message = ValidationErrorMessages.PASSWORD_CONFIRMPASSWORD_MISMATCH
)
public class RegisterWithTokenDto {
    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String token;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String firstName;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String lastName;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String firstNameNepali;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String lastNameNepali;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String title;

    private String post;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    @UsernameFormat
    private String username;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    @Size(min = 5, message = ValidationErrorMessages.CHOOSE_STRONGER_PASSWORD)
    private String password;

    @NotBlank(message = ValidationErrorMessages.FIELD_CANNOT_BE_EMPTY)
    private String confirmPassword;
}
