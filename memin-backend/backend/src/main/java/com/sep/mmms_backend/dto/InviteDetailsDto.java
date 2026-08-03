package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.InviteToken;
import com.sep.mmms_backend.enums.AppRole;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InviteDetailsDto {
    private final String email;
    private final AppRole role;
    private final String committeeName;
    private final LocalDateTime expiresAt;

    public InviteDetailsDto(InviteToken inviteToken) {
        this.email = inviteToken.getEmail();
        this.role = inviteToken.getRole();
        this.committeeName = inviteToken.getCommittee() == null
                ? null
                : inviteToken.getCommittee().getName();
        this.expiresAt = inviteToken.getExpiresAt();
    }
}
