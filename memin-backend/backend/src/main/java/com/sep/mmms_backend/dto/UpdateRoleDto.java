package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.AppRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleDto {
    private AppRole role;
}
