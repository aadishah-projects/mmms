package com.sep.mmms_backend.service;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.*;
import com.sep.mmms_backend.exceptions.*;
import com.sep.mmms_backend.repository.MeetingRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.validators.EntityValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final EntityValidator entityValidator;

    public MeetingService(MeetingRepository meetingRepository, MemberRepository memberRepository, EntityValidator entityValidator) {
        this.meetingRepository = meetingRepository;
        this.entityValidator = entityValidator;
        this.memberRepository = memberRepository;
    }

    //TODO: Create Tests
    @CheckCommitteeAccess
    public Meeting saveNewMeeting(MeetingCreationDto meetingCreationDto, Committee committee, String username) {
        entityValidator.validate(meetingCreationDto);

        Meeting meeting = new Meeting();

        meeting.setCommittee(committee);
        meeting.setTitle(meetingCreationDto.getTitle());
        meeting.setHeldDate(meetingCreationDto.getHeldDate());
        meeting.setHeldTime(meetingCreationDto.getHeldTime());
        meeting.setHeldPlace(meetingCreationDto.getHeldPlace());
        meetingCreationDto.getDecisions().forEach(decisionDto -> {
            //check if decision string is blank, if yes, don't save it
            if (decisionDto.getDecision() != null && !decisionDto.getDecision().isBlank()) {
                Decision decision = new Decision();
                decision.setDecision(decisionDto.getDecision());
                meeting.addDecision(decision);
            }
        });

        meetingCreationDto.getAgendas().forEach(agendaDto -> {
            if (agendaDto.getAgenda() != null && !agendaDto.getAgenda().isBlank()) {
                Agenda agenda = new Agenda();
                agenda.setAgenda(agendaDto.getAgenda());
                meeting.addAgenda(agenda);
            }
        });

        //populating the invittees
        //TODO: Fix (this route does not check whether the requested Invittee is already part of the commitee, it relies on the frontend to do so)
        //If the invittee is already part of the committee, it will be rendered twice in the minute
        List<Integer> requestedInvitees = meetingCreationDto.getInviteeIds().stream().toList();
        if (!requestedInvitees.isEmpty()) {
            List<Member> foundMembers = memberRepository.findAccessibleMembersByIds(requestedInvitees, username);
            memberRepository.validateWhetherAllMembersAreFound(requestedInvitees, foundMembers);
            meeting.setInvitees(foundMembers);
        }
        return meetingRepository.save(meeting);
    }


    @Transactional
    public void updateExistingMeetingMinute(MinuteUpdationDto minuteUpdationDto, int meetingId, int committeeId, String username) {
        entityValidator.validate(minuteUpdationDto);
        Meeting existingMeeting = meetingRepository.findMeetingById(meetingId);
        if (!existingMeeting.getCreatedBy().equals(username)) {
            //TODO: throw exception
        }

        if (existingMeeting.getCommittee().getId() != committeeId) {
            //TODO: throw exception
        }

        Committee existingCommittee = existingMeeting.getCommittee();

        if (!existingCommittee.getCreatedBy().equals(username)) {
            //TODO: throw exception
        }

        existingCommittee.setName(minuteUpdationDto.getCommitteeName());
        existingCommittee.setDescription(minuteUpdationDto.getCommitteeDescription());

        existingMeeting.setHeldDate(minuteUpdationDto.getMeetingHeldDate());
        existingMeeting.setHeldTime(minuteUpdationDto.getMeetingHeldTime());
        existingMeeting.setHeldPlace(minuteUpdationDto.getMeetingHeldPlace());


        //save the committee

        //remove decisions that are NOT in the new list
        existingMeeting.getDecisions().removeIf(existingDecision -> minuteUpdationDto.getDecisions().stream().noneMatch(newDecision -> newDecision.getDecisionId() == existingDecision.getDecisionId()
                )
        );

        //add new ones OR Update existing ones

        for (DecisionDto newDecision : minuteUpdationDto.getDecisions()) {

            //check if the decision already exists
            Decision existingDecision = existingMeeting.getDecisions().stream().filter(existingDecision1 -> existingDecision1.getDecisionId() == newDecision.getDecisionId()).findFirst().orElse(null);

            if (existingDecision == null) {
                if (!newDecision.getDecision().isBlank()) {
                    //its a new decision -> add it
                    Decision newDecisionObj = new Decision();
                    newDecisionObj.setDecision(newDecision.getDecision());
                    existingMeeting.addDecision(newDecisionObj);
                }
            } else {
                //its an existing decision -> update it
                existingDecision.setDecision(newDecision.getDecision());
            }
        }


        //remove agendas that are NOT in the new list
        existingMeeting.getAgendas().removeIf(existingAgenda -> minuteUpdationDto.getAgendas().stream().noneMatch(newagenda -> newagenda.getAgendaId() == existingAgenda.getAgendaId()
                )
        );

        //add new ones OR Update existing ones

        for (AgendaDto newAgenda : minuteUpdationDto.getAgendas()) {
            //check if the agenda already exists
            Agenda existingagenda = existingMeeting.getAgendas().stream().filter(existingagenda1 -> existingagenda1.getAgendaId() == newAgenda.getAgendaId()).findFirst().orElse(null);

            if (existingagenda == null) {
                //it is a new agenda -> add it
                if (!newAgenda.getAgenda().isBlank()) {
                    Agenda newagendaObj = new Agenda();
                    newagendaObj.setAgenda(newAgenda.getAgenda());
                    existingMeeting.addAgenda(newagendaObj);
                }
            } else {
                //it is an existing agenda -> update it
                existingagenda.setAgenda(newAgenda.getAgenda());
            }
        }
        meetingRepository.save(existingMeeting);
    }


    @CheckCommitteeAccess
    public List<MeetingSummaryDto> getMeetingOfCommittee(Committee committee, String username) {
        List<Meeting> meetings = committee.getMeetings();
        return meetings.stream().map(MeetingSummaryDto::new).toList();
    }


    public Meeting findMeetingById(int meetingId) {
        return meetingRepository.findById(meetingId).orElseThrow(() -> new MeetingDoesNotExistException(ExceptionMessages.MEETING_DOES_NOT_EXIST, meetingId));
    }

    public Optional<Meeting> findMeetingByIdNoException(int meetingId) {
        return meetingRepository.findById(meetingId);
    }

    public MeetingDetailsForEditDto getMeetingDetails(Integer meetingId, String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);
        List<Member> possibleInvitees = memberRepository.getPossibleInviteesForMeeting(meetingId, meeting.getCommittee().getId(), username);
        List<MemberSearchResultDto> possibleInviteesFormatted = possibleInvitees.stream().map(MemberSearchResultDto::new).toList();
        return new MeetingDetailsForEditDto(meeting, possibleInviteesFormatted);
    }

    private Meeting getMeetingIfAccessible(Integer memberId, String username) {
        Optional<Meeting> optionalMeeting = meetingRepository.getMeetingIfAccessible(memberId, username);
        if (optionalMeeting.isEmpty()) {
            throw new MeetingDoesNotExistException();
        }
        return optionalMeeting.get();
    }


    public Meeting updateExistingMeeting(MeetingCreationDto meetingCreationDto, Integer meetingId, String username) {
        entityValidator.validate(meetingCreationDto);
        Meeting existingMeeting = getMeetingIfAccessible(meetingId, username);

        //reassign the updated values
        existingMeeting.setTitle(meetingCreationDto.getTitle());
        existingMeeting.setHeldPlace(meetingCreationDto.getHeldPlace());
        existingMeeting.setHeldTime(meetingCreationDto.getHeldTime());
        existingMeeting.setHeldDate(meetingCreationDto.getHeldDate());

        List<Agenda> existingAgendas = existingMeeting.getAgendas();

        //remove agendas that are NOT in the new list
        existingAgendas.removeIf(existing -> meetingCreationDto.getAgendas().stream().noneMatch(newAgenda -> newAgenda.getAgendaId() == existing.getAgendaId()));

        //add new ones or update existing ones
        for (AgendaDto newAgendaDto : meetingCreationDto.getAgendas()) {

            //check if the agenda is already in the current list
            Agenda existingAgenda = existingAgendas.stream().filter(agenda -> agenda.getAgendaId() == newAgendaDto.getAgendaId()).findFirst().orElse(null);

            if(existingAgenda == null) {
                //CASE: It's a new agenda -> Add It
                Agenda newAgenda = new Agenda();
                newAgenda.setAgenda(newAgendaDto.getAgenda());
                newAgenda.setMeeting(existingMeeting);
                existingMeeting.getAgendas().add(newAgenda);
            } else {
                //CASE: Agenda exists -> Update Agenda data
                existingAgenda.setAgenda(newAgendaDto.getAgenda());
            }
        }

        List<Decision> existingDecisions = existingMeeting.getDecisions();

        //remove decisions that are NOT in the new list
        existingDecisions.removeIf(existing -> meetingCreationDto.getDecisions().stream().noneMatch(newDecision -> newDecision.getDecisionId() == existing.getDecisionId()));

        //add new ones or update existing ones
        for (DecisionDto newDecisionDto : meetingCreationDto.getDecisions()) {
            //check if the decision is already in the current list
            Decision existingDecision = existingDecisions.stream().filter(decision -> decision.getDecisionId() == newDecisionDto.getDecisionId()).findFirst().orElse(null);

            if(existingDecision == null) {
                //CASE: It's a new decision -> Add It
                Decision newDecision = new Decision();
                newDecision.setDecision(newDecisionDto.getDecision());
                newDecision.setMeeting(existingMeeting);
                existingMeeting.getDecisions().add(newDecision);
            } else {
                //CASE: Decision exists -> Update Decision data
                existingDecision.setDecision(newDecisionDto.getDecision());
            }
        }

        //remove invitees that are not in the new list
        List<Member> existingInvitees = existingMeeting.getInvitees();

        //take existing invitee, compare with all new ids, if returns false, remove
        existingInvitees.removeIf(existing -> meetingCreationDto.getInviteeIds().stream().noneMatch(newInviteeId -> newInviteeId == existing.getId()));

        //add new Invitees
        for(Integer newInviteeId: meetingCreationDto.getInviteeIds()) {

            boolean noneMatch = (existingMeeting.getInvitees().stream().noneMatch(invitee -> invitee.getId() == newInviteeId));

            if(noneMatch) {
                //fetch the new invitee member and add
                Optional<Member> member = memberRepository.findAccessibleMember(newInviteeId, username);
                if(member.isEmpty()) {
                    throw new MemberDoesNotExistException(ExceptionMessages.MEMBER_DOES_NOT_EXIST, newInviteeId);
                }
                existingMeeting.getInvitees().add(member.get());
            }
        }
        return meetingRepository.save(existingMeeting);
    }
}
