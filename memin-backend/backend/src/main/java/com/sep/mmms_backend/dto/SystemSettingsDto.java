package com.sep.mmms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SystemSettingsDto {
    private AiSettingsDto ai;
    private EmailSettingsDto email;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
