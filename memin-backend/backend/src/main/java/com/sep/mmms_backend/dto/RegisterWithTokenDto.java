package com.sep.mmms_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterWithTokenDto {
    private String token;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String confirmPassword;
}
