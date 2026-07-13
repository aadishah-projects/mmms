package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.enums.CommitteeStatus;
import com.sep.mmms_backend.enums.MinuteLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CommitteeCreationDto {
    @NotBlank(message = "committee name is required")
    private String name;

    @NotBlank(message = "committee description/aim is required")
    private String description;

    private CommitteeStatus status = CommitteeStatus.ACTIVE;

    Integer maximumNumberOfMeetings;

    //TODO implement a non blank validator for this
//    @NotBlank(message="minute language is required")
    private MinuteLanguage minuteLanguage;

    //member id and role
    //this annotation causes the json's 'key' to be used as the 'key' of the map
    List<MemberIdWithRoleDto> members = new ArrayList<>();

    @NotNull(message = "committee coordinator is missing")
    Integer coordinatorId;
}
