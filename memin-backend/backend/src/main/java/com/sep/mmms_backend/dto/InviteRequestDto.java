package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Pattern(regexp = ".*@pcampus\\.edu\\.np$", message = "Email must be from the @pcampus.edu.np domain")
    private String email;
    private AppRole role;
    private Integer committeeId;
}
