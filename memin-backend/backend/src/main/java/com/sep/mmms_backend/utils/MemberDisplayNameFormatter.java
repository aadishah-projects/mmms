package com.sep.mmms_backend.utils;

import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.MinuteLanguage;

/** Formats a member using the language selected for the committee minute. */
public final class MemberDisplayNameFormatter {

    private MemberDisplayNameFormatter() {
    }

    public static String format(Member member, MinuteLanguage language) {
        if (member == null) {
            return "";
        }

        boolean nepali = MinuteLanguage.NEPALI.equals(language);
        String title = nepali ? firstNonBlank(member.getTitleNepali(), member.getTitle())
                : firstNonBlank(member.getTitle(), member.getTitleNepali());
        String firstName = nepali ? firstNonBlank(member.getFirstNameNepali(), member.getFirstName())
                : firstNonBlank(member.getFirstName(), member.getFirstNameNepali());
        String lastName = nepali ? firstNonBlank(member.getLastNameNepali(), member.getLastName())
                : firstNonBlank(member.getLastName(), member.getLastNameNepali());

        return String.join(" ", new String[]{title, firstName, lastName}).trim().replaceAll("\\s+", " ");
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim()
                : fallback == null ? "" : fallback.trim();
    }
}
