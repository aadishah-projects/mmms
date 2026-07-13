package com.sep.mmms_backend.dto;

public class CommitteeIdAndNameDto {
    public final int committeeId;
    public final String committeeName;

    public CommitteeIdAndNameDto(int committeeId, String committeeName) {
        this.committeeId = committeeId;
        this.committeeName = committeeName;
    }
}
