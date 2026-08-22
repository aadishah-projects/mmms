package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Authority is required")
    private AppRole role;
    private Integer committeeId;
}
