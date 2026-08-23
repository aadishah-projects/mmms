package com.sep.mmms_backend.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class NepaliLocalTimeDeserializer extends JsonDeserializer<LocalTime> {
    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String timeStr = p.getValueAsString();
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }

        // Convert Nepali digits to English digits
        timeStr = timeStr.replace('\u0966', '0')
                         .replace('\u0967', '1')
                         .replace('\u0968', '2')
                         .replace('\u0969', '3')
                         .replace('\u096A', '4')
                         .replace('\u096B', '5')
                         .replace('\u096C', '6')
                         .replace('\u096D', '7')
                         .replace('\u096E', '8')
                         .replace('\u096F', '9');

        try {
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            if (timeStr.length() == 5) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
            throw e;
        }
    }
}
