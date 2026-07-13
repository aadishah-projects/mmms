package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Agenda;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Decision;
import com.sep.mmms_backend.entity.Member;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GlobalSearchResultDto {
    List<CommitteeIdAndNameDto> committees = new ArrayList<>();
    List<MemberSearchResultDto> members = new ArrayList<>();
    List<DecisionWithMeetingId> decisions = new ArrayList<>();
    List<AgendaWithMeetingId> agendas = new ArrayList<>();

    public GlobalSearchResultDto(List<Committee> committees, List<Member> members, List<Decision> decisions, List<Agenda> agendas) {
        committees.forEach(committee -> this.committees.add(new CommitteeIdAndNameDto(committee.getId(), committee.getName())));

        members.forEach(member -> this.members.add(new MemberSearchResultDto(member)));

        decisions.forEach(decision -> this.decisions.add(new DecisionWithMeetingId(decision)));

        agendas.forEach(agenda -> this.agendas.add(new AgendaWithMeetingId(agenda)));
    }
}
