package com.sep.mmms_backend.databuilder;


import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/*
    A meeting is associated with a committee and hence must reference a 'SAVED' committee. It populates the join column as it owns the relationship

    A meeting is associated with a coordinator and hence must reference a 'SAVED' coordinator. It populates the join column as it owns the relationship

    A meeting is associated with at least one attendee, and hence must reference a 'SAVED' attendees. It populates join tbale as it owns the relationship

    A meeting is associated with decisions. However, they can reference an unsaved 'DECISION' because meeting has a CascadeType.PERSIST policy for decision.

        The decision must also reference the meeting as decision is the owner of the relationship and requires a meeting reference into order to populate join column
 */

public class MeetingBuilder {
    private String title = "meetingTitle";
    private LocalDate heldDate = LocalDate.now();
    private LocalTime heldTime = LocalTime.of(10, 0);
    private String heldPlace = "meetingHeldPlace";

    private Committee committee = null;


    private List<Member> attendees = new LinkedList<>();
    private List<Decision> decisions = new ArrayList<>();

    /**
     * 'saved committee' , 'at least one saved Atteendee' and 'unsaved decisions' is mandatory for save operation in this entity
     */
    public static MeetingBuilder builder() {
        return new MeetingBuilder();
    }


    public MeetingBuilder withHeldDate(LocalDate heldDate) {
        this.heldDate = heldDate;
        return this;
    }

    public MeetingBuilder withHeldTime(LocalTime heldTime) {
        this.heldTime = heldTime;
        return this;
    }

    public MeetingBuilder withHeldPlace(String heldPlace) {
        this.heldPlace = heldPlace;
        return this;
    }

    public MeetingBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public MeetingBuilder withCommittee(Committee committee) {
        this.committee = committee;
        return this;
    }


    public MeetingBuilder withAttendees(List<Member> attendees) {
        this.attendees = attendees;
        return this;
    }

    public MeetingBuilder withDecisions(List<Decision> decisions) {
        this.decisions = decisions;
        return this;
    }

    public Meeting build() {
        Meeting meeting = new Meeting();
        meeting.setTitle(this.title);
        meeting.setHeldDate(this.heldDate);
        meeting.setHeldTime(this.heldTime);
        meeting.setHeldPlace(this.heldPlace);

        meeting.setCommittee(this.committee);
        this.committee.getMeetings().add(meeting);


        meeting.setAttendees(this.attendees);

        meeting.setDecisions(this.decisions);

        //this step is crucial because decision is the owner of the relationship
        for (Decision decision : this.decisions) {
            decision.setMeeting(meeting);
        }


        return meeting;
    }
}
