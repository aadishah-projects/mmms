package com.sep.mmms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiConnectionTestResultDto {
    private boolean success;
    private String message;
}
