package com.sep.mmms_backend.dto;

import com.sep.mmms_backend.entity.MinuteTemplate;
import lombok.Getter;

@Getter
public class MinuteTemplateSummaryDto {
    private final Integer templateId;
    private final String name;
    private final String minuteTemplateHtml;
    private final String minuteLanguage;
    private final boolean active;

    public MinuteTemplateSummaryDto(MinuteTemplate template, boolean active) {
        this.templateId = template.getId();
        this.name = template.getName();
        this.minuteTemplateHtml = template.getMinuteTemplateHtml();
        this.minuteLanguage = template.getMinuteLanguage() == null
                ? null
                : template.getMinuteLanguage().name();
        this.active = active;
    }
}
