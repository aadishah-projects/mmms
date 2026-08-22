package com.sep.mmms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailSettingsDto {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private boolean hasPassword;
    private Boolean auth;
    private Boolean starttls;
    private String fromAddress;
    private String frontendUrl;
}
