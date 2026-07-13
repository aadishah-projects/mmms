package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Decision;

public class DecisionWithMeetingId {
    public int meetingId;
    public String decision;

    DecisionWithMeetingId(Decision decision) {
        this.meetingId = decision.getMeeting().getId();
        this.decision = decision.getDecision();
    }
}
