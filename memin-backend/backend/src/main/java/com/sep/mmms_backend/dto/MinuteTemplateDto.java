package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.enums.MinuteLanguage;

/**
 * The data needed by the dedicated committee minute-template editor.
 */
public class MinuteTemplateDto {
    public Integer committeeId;
    public String committeeName;
    public String committeeDescription;
    public MinuteLanguage minuteLanguage;
    public String minuteTemplateHtml;

    // Kept for compatibility with templates created before the rich editor.
    public String minuteOpeningTemplate;
    public String minuteHeaderTemplate;

    public MinuteTemplateDto(Committee committee) {
        this.committeeId = committee.getId();
        this.committeeName = committee.getName();
        this.committeeDescription = committee.getDescription();
        this.minuteLanguage = committee.getMinuteLanguage();
        this.minuteTemplateHtml = committee.getMinuteTemplateHtml();
        this.minuteOpeningTemplate = committee.getMinuteOpeningTemplate();
        this.minuteHeaderTemplate = committee.getMinuteHeaderTemplate();
    }
}
