package com.sep.mmms_backend.service;

import com.sep.mmms_backend.entity.Committee;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpeningParagraphService {

    String[] templateNames = {"opening_paragraph_nepali"};
    TemplateEngine engine;
    OpeningParagraphService(TemplateEngine engine) {
        this.engine = engine;
    }


    public String[] getOpeningParagraphs(Committee committee) {
        String[] openingParagraphs = new String[templateNames.length];
        openingParagraphs[0] = getOpeningParagraph("opening_paragraph_nepali", committee);

        return openingParagraphs;
    }

    private String getOpeningParagraph(String templateName, Committee committee) {
        Map<String, Object> templateData = new HashMap<>();

        templateData.put("meetingHeldDate", LocalDate.now().toString());

        templateData.put("meetingHeldDateEnglish", "2020-02-05");

        templateData.put("meetingHeldDay", "Sunday");
        templateData.put("partOfDay", "Evening");

        templateData.put("meetingHeldTime","8:00");

        templateData.put("meetingHeldPlace", "Pulchowk");

        templateData.put("committeeDescription", committee.getDescription());

        templateData.put("committeeName", committee.getName());

        templateData.put("coordinatorFullName", "Coordinator Name");

        Context context = new Context();
        context.setVariables(templateData);

        return engine.process(templateName,context);
    }
}
