package com.sep.mmms_backend.global_constants;

//did not make these as enums because Validation annotations require values that are compile time constants

public class ValidationErrorMessages {
    public static final String INVALID_USERNAME_FORMAT = "invalid username format :: username must only contain 'a-z' 'A-Z' '0-9' '_' ";

    public static final String USERNAME_NOT_AVAILABLE = "the selected username is already in user";

    public static final String FIELD_CANNOT_BE_EMPTY = "This field cannot be empty";

    public static final String VALID_EMAIL_REQUIRED = "This field requires a valid email address";

    public static final String PASSWORD_CONFIRMPASSWORD_MISMATCH = "Password and confirm password fields are not the same";

    public static final String CHOOSE_STRONGER_PASSWORD = "Please choose a stronger password";
}
