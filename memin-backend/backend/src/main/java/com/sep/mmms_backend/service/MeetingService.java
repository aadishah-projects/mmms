package com.sep.mmms_backend.service;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.dto.*;
import com.sep.mmms_backend.entity.*;
import com.sep.mmms_backend.exceptions.*;
import com.sep.mmms_backend.enums.MinuteLanguage;
import com.sep.mmms_backend.repository.MeetingRepository;
import com.sep.mmms_backend.repository.MemberRepository;
import com.sep.mmms_backend.repository.AppUserRepository;
import com.sep.mmms_backend.validators.EntityValidator;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final EntityValidator entityValidator;
    private final AppUserRepository appUserRepository;
    private final EmailService emailService;
    private final CommitteeService committeeService;
    private final MeetingMinutePreparationService meetingMinutePreparationService;

    public MeetingService(MeetingRepository meetingRepository, MemberRepository memberRepository, EntityValidator entityValidator, AppUserRepository appUserRepository, EmailService emailService, CommitteeService committeeService, MeetingMinutePreparationService meetingMinutePreparationService) {
        this.meetingRepository = meetingRepository;
        this.entityValidator = entityValidator;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
        this.emailService = emailService;
        this.committeeService = committeeService;
        this.meetingMinutePreparationService = meetingMinutePreparationService;
    }

    @Transactional
    @CheckCommitteeAccess
    public Meeting saveNewMeeting(MeetingCreationDto meetingCreationDto, Committee committee, String username) {
        entityValidator.validate(meetingCreationDto);

        Meeting meeting = new Meeting();

        meeting.setCommittee(committee);
        meeting.setTitle(meetingCreationDto.getTitle());
        meeting.setHeldDate(meetingCreationDto.getHeldDate());
        meeting.setHeldTime(meetingCreationDto.getHeldTime());
        meeting.setHeldPlace(meetingCreationDto.getHeldPlace());
        meeting.setChairman(resolveChairman(meetingCreationDto.getChairmanId(), committee, null));
        // Freeze the committee template at meeting creation. A blank snapshot
        // deliberately means "use the built-in minute view" for this meeting.
        meeting.setMinuteTemplateHtml(committee.getMinuteTemplateHtml() == null
                ? ""
                : committee.getMinuteTemplateHtml());
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
            meeting.setInvitees(orderMembersByRequestedIds(requestedInvitees, foundMembers));
        }
        initializeParticipantOrder(meeting);
        Meeting savedMeeting = meetingRepository.save(meeting);
        notifyFinalMinuteRecipients(savedMeeting, username);
        return savedMeeting;
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

        if (MinuteLanguage.NEPALI.equals(existingCommittee.getMinuteLanguage())) {
            existingCommittee.setNepaliName(minuteUpdationDto.getCommitteeName());
        } else {
            existingCommittee.setName(minuteUpdationDto.getCommitteeName());
        }
        existingCommittee.setDescription(minuteUpdationDto.getCommitteeDescription());

        existingMeeting.setHeldDate(minuteUpdationDto.getMeetingHeldDate());
        existingMeeting.setHeldTime(minuteUpdationDto.getMeetingHeldTime());
        existingMeeting.setHeldPlace(minuteUpdationDto.getMeetingHeldPlace());
        if (minuteUpdationDto.getHtmlContent() != null) {
            existingMeeting.setMinuteContentHtml(minuteUpdationDto.getHtmlContent().isBlank()
                    ? null
                    : minuteUpdationDto.getHtmlContent());
        }


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

    /**
     * Replaces only the structured agenda and decision records returned by the
     * AI assistant. The assistant never owns the meeting's HTML document.
     */
    @Transactional
    public void replaceAgendaAndDecisionItems(
            int meetingId,
            List<String> agendas,
            List<String> decisions,
            String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);

        meeting.getAgendas().clear();
        if (agendas != null) {
            agendas.stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(value -> {
                        Agenda agenda = new Agenda();
                        agenda.setAgenda(value);
                        meeting.addAgenda(agenda);
                    });
        }

        meeting.getDecisions().clear();
        if (decisions != null) {
            decisions.stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(value -> {
                        Decision decision = new Decision();
                        decision.setDecision(value);
                        meeting.addDecision(decision);
                    });
        }

        meetingRepository.save(meeting);
    }

    @Transactional
    public void updateMinuteContent(Integer meetingId, String htmlContent, String username) {
        Meeting meeting = findMeetingById(meetingId);
        committeeService.getCommitteeIfAccessible(meeting.getCommittee().getId(), username);
        meeting.setMinuteContentHtml(htmlContent);
        meetingRepository.save(meeting);
    }

    /** Persist the order in which participants are rendered in the minute. */
    @Transactional
    public Meeting updateParticipantOrder(int meetingId, List<Integer> requestedParticipantIds, String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);
        List<Member> defaultParticipants = getDefaultParticipantOrder(meeting);
        Map<Integer, Member> allowed = defaultParticipants.stream()
                .filter(member -> member.getId() != null)
                .collect(Collectors.toMap(Member::getId, member -> member, (left, right) -> left,
                        LinkedHashMap::new));

        List<Member> ordered = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        if (requestedParticipantIds != null) {
            for (Integer participantId : requestedParticipantIds) {
                if (participantId != null && allowed.containsKey(participantId) && seen.add(participantId)) {
                    ordered.add(allowed.get(participantId));
                }
            }
        }
        defaultParticipants.stream()
                .filter(member -> member.getId() != null && seen.add(member.getId()))
                .forEach(ordered::add);

        meeting.getAttendees().clear();
        meeting.getAttendees().addAll(ordered);
        refreshSavedAttendanceTable(meeting, ordered);
        return meetingRepository.save(meeting);
    }

    private void refreshSavedAttendanceTable(Meeting meeting, List<Member> orderedParticipants) {
        if (meeting.getMinuteContentHtml() == null || meeting.getMinuteContentHtml().isBlank()) {
            return;
        }
        Document document = Jsoup.parseBodyFragment(meeting.getMinuteContentHtml());
        Element attendanceTable = document.select("table.memberships").first();
        if (attendanceTable == null) {
            attendanceTable = document.select("table").stream()
                    .filter(table -> {
                        String text = table.text().toLowerCase();
                        return text.contains("signature") && text.contains("name");
                    })
                    .findFirst()
                    .orElse(null);
        }
        if (attendanceTable == null) {
            return;
        }

        Element body = attendanceTable.selectFirst("tbody");
        if (body == null) {
            body = attendanceTable.appendElement("tbody");
        }
        Map<String, String> signaturesByName = new HashMap<>();
        for (Element row : body.children()) {
            if (row.childrenSize() >= 4) {
                String participantName = row.child(1).text();
                if (!participantName.isBlank()) {
                    signaturesByName.put(participantName, row.child(3).html());
                }
            }
        }
        body.empty();
        for (int index = 0; index < orderedParticipants.size(); index++) {
            Member participant = orderedParticipants.get(index);
            String participantName = participantDisplayName(participant, meeting.getCommittee().getMinuteLanguage());
            Element row = body.appendElement("tr");
            row.appendElement("td").text(String.valueOf(index + 1));
            row.appendElement("td").text(participantName);
            row.appendElement("td").text(participantRole(meeting.getCommittee(), participant));
            row.appendElement("td").html(signaturesByName.getOrDefault(participantName, ""));
        }
        meeting.setMinuteContentHtml(document.body().html());
    }

    private String participantDisplayName(Member member, MinuteLanguage language) {
        boolean nepali = MinuteLanguage.NEPALI.equals(language);
        String title = nepali && member.getTitleNepali() != null && !member.getTitleNepali().isBlank()
                ? member.getTitleNepali() : member.getTitle();
        String firstName = nepali && member.getFirstNameNepali() != null && !member.getFirstNameNepali().isBlank()
                ? member.getFirstNameNepali() : member.getFirstName();
        String lastName = nepali && member.getLastNameNepali() != null && !member.getLastNameNepali().isBlank()
                ? member.getLastNameNepali() : member.getLastName();
        String name = String.join(" ",
                title == null ? "" : title,
                firstName == null ? "" : firstName,
                lastName == null ? "" : lastName).trim();
        if (member.getPost() != null && !member.getPost().isBlank()) {
            return name + ", " + member.getPost();
        }
        if (member.getInstitution() != null && !member.getInstitution().isBlank()) {
            return name + ", " + member.getInstitution();
        }
        return name;
    }

    private String participantRole(Committee committee, Member participant) {
        if (committee.getCoordinator() != null && participant.getId().equals(committee.getCoordinator().getId())) {
            return committee.getMinuteLanguage() != null && committee.getMinuteLanguage().name().equals("NEPALI")
                    ? "\u0938\u0902\u092f\u094b\u091c\u0915" : "Coordinator";
        }
        for (CommitteeMembership membership : committee.getMemberships()) {
            if (membership.getMember().getId().equals(participant.getId())) {
                return membership.getRole();
            }
        }
        return committee.getMinuteLanguage() != null && committee.getMinuteLanguage().name().equals("NEPALI")
                ? "\u0906\u092e\u0928\u094d\u0924\u094d\u0930\u093f\u0924" : "Invitee";
    }


    @CheckCommitteeAccess
    public List<MeetingSummaryDto> getMeetingOfCommittee(Committee committee, String username) {
        // Reload the meetings with their agendas eagerly fetched so MeetingSummaryDto
        // can include agenda items (committee.getMeetings() leaves agendas lazy).
        List<Meeting> meetings = meetingRepository.findByCommitteeIdWithAgendas(committee.getId());
        return meetings.stream().map(MeetingSummaryDto::new).toList();
    }


    public Meeting findMeetingById(int meetingId) {
        return meetingRepository.findById(meetingId).orElseThrow(() -> new MeetingDoesNotExistException(ExceptionMessages.MEETING_DOES_NOT_EXIST, meetingId));
    }

    public Optional<Meeting> findMeetingByIdNoException(int meetingId) {
        return meetingRepository.findById(meetingId);
    }

    @Transactional
    public MeetingDetailsForEditDto getMeetingDetails(Integer meetingId, String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);
        List<Member> possibleInvitees = memberRepository.getPossibleInviteesForMeeting(meetingId, meeting.getCommittee().getId(), username);
        List<MemberSearchResultDto> possibleInviteesFormatted = possibleInvitees.stream().map(MemberSearchResultDto::new).toList();
        int meetingNumber = meetingRepository.findByCommitteeIdWithAgendas(meeting.getCommittee().getId()).stream()
                .filter(candidate -> candidate.getId() != null)
                .sorted(java.util.Comparator.comparing(Meeting::getId))
                .map(Meeting::getId)
                .toList()
                .indexOf(meeting.getId()) + 1;
        return new MeetingDetailsForEditDto(meeting, possibleInviteesFormatted,
                meetingNumber > 0 ? meetingNumber : 1);
    }

    private Meeting getMeetingIfAccessible(Integer memberId, String username) {
        Meeting meeting = meetingRepository.findById(memberId)
                .orElseThrow(() -> new MeetingDoesNotExistException());
        // Access is committee-scoped. Restricting the lookup to meeting.createdBy
        // blocked a secretary from working on meetings created by another user.
        committeeService.getCommitteeIfAccessible(meeting.getCommittee().getId(), username);
        return meeting;
    }

    private void initializeParticipantOrder(Meeting meeting) {
        meeting.getAttendees().clear();
        meeting.getAttendees().addAll(getDefaultParticipantOrder(meeting));
    }

    private List<Member> orderMembersByRequestedIds(List<Integer> requestedIds, Collection<Member> members) {
        Map<Integer, Member> membersById = members.stream()
                .filter(member -> member.getId() != null)
                .collect(Collectors.toMap(Member::getId, member -> member, (left, right) -> left));
        return requestedIds.stream()
                .map(membersById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void reorderInvitees(Meeting meeting, List<Integer> requestedInviteeIds) {
        List<Member> orderedInvitees = orderMembersByRequestedIds(
                requestedInviteeIds,
                meeting.getInvitees());
        meeting.getInvitees().clear();
        meeting.getInvitees().addAll(orderedInvitees);
    }

    private void syncParticipantOrder(Meeting meeting, List<Integer> requestedInviteeIds) {
        List<Member> allowed = getDefaultParticipantOrder(meeting);
        Set<Integer> allowedIds = allowed.stream()
                .map(Member::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> requestedInviteeIdSet = new HashSet<>(requestedInviteeIds);
        List<Member> orderedInvitees = orderMembersByRequestedIds(requestedInviteeIds, meeting.getInvitees());
        List<Member> currentAttendees = new ArrayList<>(meeting.getAttendees());
        if (currentAttendees.isEmpty()) {
            currentAttendees.addAll(allowed);
        }
        List<Member> ordered = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int nextRequestedInvitee = 0;

        for (Member attendee : currentAttendees) {
            if (attendee.getId() != null && requestedInviteeIdSet.contains(attendee.getId())) {
                if (nextRequestedInvitee < orderedInvitees.size()) {
                    Member requestedInvitee = orderedInvitees.get(nextRequestedInvitee++);
                    if (seen.add(requestedInvitee.getId())) {
                        ordered.add(requestedInvitee);
                    }
                }
            } else if (attendee.getId() != null && allowedIds.contains(attendee.getId()) && seen.add(attendee.getId())) {
                ordered.add(attendee);
            }
        }
        while (nextRequestedInvitee < orderedInvitees.size()) {
            Member requestedInvitee = orderedInvitees.get(nextRequestedInvitee++);
            if (seen.add(requestedInvitee.getId())) {
                ordered.add(requestedInvitee);
            }
        }
        allowed.stream()
                .filter(member -> member.getId() != null && seen.add(member.getId()))
                .forEach(ordered::add);
        meeting.getAttendees().clear();
        meeting.getAttendees().addAll(ordered);
    }

    private List<Member> getDefaultParticipantOrder(Meeting meeting) {
        Committee committee = meeting.getCommittee();
        List<Member> participants = new ArrayList<>();
        addParticipantIfMissing(participants, getChairman(meeting));
        addParticipantIfMissing(participants, committee.getCoordinator());
        committee.getSortedMemberships().forEach(membership -> addParticipantIfMissing(participants, membership.getMember()));
        meeting.getInvitees().forEach(invitee -> addParticipantIfMissing(participants, invitee));
        return participants;
    }

    private Member getChairman(Meeting meeting) {
        return meeting.getChairman() != null ? meeting.getChairman() : meeting.getCommittee().getCoordinator();
    }

    private Member resolveChairman(Integer chairmanId, Committee committee, Member currentChairman) {
        if (chairmanId == null) {
            return currentChairman != null ? currentChairman : committee.getCoordinator();
        }

        if (committee.getCoordinator().getId().equals(chairmanId)) {
            return committee.getCoordinator();
        }
        return committee.getSortedMemberships().stream()
                .map(CommitteeMembership::getMember)
                .filter(member -> member.getId().equals(chairmanId))
                .findFirst()
                .orElseThrow(() -> new IllegalOperationException("The chairman must be a member of the committee"));
    }

    private void addParticipantIfMissing(List<Member> participants, Member candidate) {
        if (candidate != null && participants.stream().noneMatch(existing ->
                existing.getId() != null && existing.getId().equals(candidate.getId()))) {
            participants.add(candidate);
        }
    }


    @Transactional
    public Meeting updateExistingMeeting(MeetingCreationDto meetingCreationDto, Integer meetingId, String username) {
        entityValidator.validate(meetingCreationDto);
        Meeting existingMeeting = getMeetingIfAccessible(meetingId, username);

        //reassign the updated values
        existingMeeting.setTitle(meetingCreationDto.getTitle());
        existingMeeting.setHeldPlace(meetingCreationDto.getHeldPlace());
        existingMeeting.setHeldTime(meetingCreationDto.getHeldTime());
        existingMeeting.setHeldDate(meetingCreationDto.getHeldDate());
        existingMeeting.setChairman(resolveChairman(
                meetingCreationDto.getChairmanId(),
                existingMeeting.getCommittee(),
                existingMeeting.getChairman()));

        List<Agenda> existingAgendas = existingMeeting.getAgendas();

        //remove agendas that are NOT in the new list
        existingAgendas.removeIf(existing -> meetingCreationDto.getAgendas().stream().noneMatch(newAgenda -> java.util.Objects.equals(newAgenda.getAgendaId(), existing.getAgendaId())));

        //add new ones or update existing ones
        for (AgendaDto newAgendaDto : meetingCreationDto.getAgendas()) {

            //check if the agenda is already in the current list
            Agenda existingAgenda = existingAgendas.stream().filter(agenda -> java.util.Objects.equals(agenda.getAgendaId(), newAgendaDto.getAgendaId())).findFirst().orElse(null);

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

        Set<Decision> existingDecisions = existingMeeting.getDecisions();

        //remove decisions that are NOT in the new list
        existingDecisions.removeIf(existing -> meetingCreationDto.getDecisions().stream().noneMatch(newDecision -> java.util.Objects.equals(newDecision.getDecisionId(), existing.getDecisionId())));

        //add new ones or update existing ones
        for (DecisionDto newDecisionDto : meetingCreationDto.getDecisions()) {
            //check if the decision is already in the current list
            Decision existingDecision = existingDecisions.stream().filter(decision -> java.util.Objects.equals(decision.getDecisionId(), newDecisionDto.getDecisionId())).findFirst().orElse(null);

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
        List<Integer> requestedInviteeIds = meetingCreationDto.getInviteeIds().stream().toList();
        reorderInvitees(existingMeeting, requestedInviteeIds);
        syncParticipantOrder(existingMeeting, requestedInviteeIds);
        // Keep a previously edited minute as the source of truth. When the
        // meeting edit form changes the invitee order, update only its saved
        // attendance rows instead of rebuilding the document from the frozen
        // committee template.
        refreshSavedAttendanceTable(existingMeeting, existingMeeting.getAttendees());
        return meetingRepository.save(existingMeeting);
    }

    @Transactional
    public void deleteMeeting(Integer meetingId, String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);

        // Clear join-table associations and orphaned child records before
        // deleting the meeting so foreign-key constraints remain valid.
        meeting.getAttendees().clear();
        meeting.getInvitees().clear();
        meeting.getDecisions().clear();
        meeting.getAgendas().clear();
        meetingRepository.delete(meeting);
    }

    @Transactional
    public int sendMeetingInvites(Integer meetingId, String username) {
        Meeting meeting = getMeetingIfAccessible(meetingId, username);
        return notifyFinalMinuteRecipients(meeting, username);
    }

    private int notifyFinalMinuteRecipients(Meeting meeting, String senderUsername) {
        byte[] attachment;
        try {
            String htmlContent = meetingMinutePreparationService.renderMinuteForEmail(
                    meeting.getCommittee(), meeting, senderUsername);
            attachment = meetingMinutePreparationService.createWordDocumentFromHtml(htmlContent);
        } catch (Exception exception) {
            log.warn("Could not prepare the final minute attachment for meeting {}. The meeting record was still saved. Reason: {}",
                    meeting.getId(), exception.getMessage(), exception);
            return 0;
        }

        AppUser sender = appUserServiceForEmail(senderUsername);
        String senderName = sender.getFirstName() + " " + sender.getLastName();
        int sentCount = 0;

        for (Member invitee : meeting.getInvitees()) {
            String email = invitee.getEmail();
            if ((email == null || email.isBlank()) && invitee.getId() != null) {
                email = appUserRepository.findFirstByLinkedMemberId(invitee.getId())
                        .map(AppUser::getEmail)
                        .orElse(null);
            }
            if (email != null && !email.isBlank()
                    && emailService.sendMeetingMinutesEmail(email, meeting, senderName, attachment)) {
                sentCount++;
            }
        }
        return sentCount;
    }

    private AppUser appUserServiceForEmail(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UserDoesNotExistException(ExceptionMessages.USER_DOES_NOT_EXIST));
    }
}
