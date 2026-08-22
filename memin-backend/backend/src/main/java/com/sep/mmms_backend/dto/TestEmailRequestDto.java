package com.sep.mmms_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestEmailRequestDto {
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Must be a valid email address")
    private String toEmail;
}
