package com.sep.mmms_backend.service;

import com.sep.mmms_backend.aop.interfaces.CheckCommitteeAccess;
import com.sep.mmms_backend.dto.AgendaDto;
import com.sep.mmms_backend.dto.CommitteeMembershipDto;
import com.sep.mmms_backend.dto.DecisionDto;
import com.sep.mmms_backend.dto.MinuteDataDto;
import com.sep.mmms_backend.entity.Committee;
import com.sep.mmms_backend.entity.Meeting;
import com.sep.mmms_backend.entity.Member;
import com.sep.mmms_backend.enums.MinuteLanguage;
import com.sep.mmms_backend.repository.CommitteeRepository;
import com.sep.mmms_backend.repository.MeetingRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.hibernate.Hibernate;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MeetingMinutePreparationService {
    private final TemplateEngine templateEngine;
    private final NepaliDateService nepaliDateService;
    private final MeetingRepository meetingRepository;

    @Autowired
    public MeetingMinutePreparationService(TemplateEngine templateEngine, NepaliDateService nepaliDateService, MeetingRepository meetingRepository) {
        this.templateEngine = templateEngine;
        this.nepaliDateService = nepaliDateService;
        this.meetingRepository = meetingRepository;
    }

    /** Compatibility constructor retained for existing unit tests and callers. */
    public MeetingMinutePreparationService(TemplateEngine templateEngine) {
        this(templateEngine, new NepaliDateService(), null);
    }

    /** Compatibility constructor retained for existing unit tests and callers. */
    public MeetingMinutePreparationService(
            MeetingService ignoredMeetingService,
            CommitteeRepository ignoredCommitteeRepository,
            MemberService ignoredMemberService,
            TemplateEngine templateEngine) {
        this(templateEngine, new NepaliDateService(), null);
    }


    @Transactional(readOnly = true)
    @CheckCommitteeAccess(shouldValidateMeeting = true)
    public MinuteDataDto prepareDataForMinute(Committee committee, Meeting meeting, String username) {
        // Controllers may pass an entity returned by a completed repository call.
        // Reload it in this transaction before touching lazy minute collections;
        // otherwise decisions/agendas can still be attached to a closed session.
        if (meetingRepository != null && meeting != null && meeting.getId() != null) {
            Meeting managedMeeting = meetingRepository.findById(meeting.getId()).orElse(meeting);
            initializeMinuteGraph(managedMeeting);
            meeting = managedMeeting;
            if (managedMeeting.getCommittee() != null) {
                committee = managedMeeting.getCommittee();
            }
        }

        MinuteDataDto minuteData = new MinuteDataDto();
        minuteData.setMinuteLanguage(committee.getMinuteLanguage());
        setDates(minuteData, meeting);

        minuteData.setMeetingHeldDay(getMeetingHeldDay(meeting.getHeldDate(), committee.getMinuteLanguage()));

        minuteData.setPartOfDay(getPartOfDay(meeting.getHeldTime(), committee.getMinuteLanguage()));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
        String meetingHeldTime = meeting.getHeldTime().format(timeFormatter);
        if (MinuteLanguage.NEPALI.equals(committee.getMinuteLanguage())) {
            meetingHeldTime = toNepaliDigits(meetingHeldTime);
        }

        minuteData.setMeetingHeldTime(meetingHeldTime);

        minuteData.setMeetingHeldPlace(meeting.getHeldPlace());
        minuteData.setMeetingTitle(meeting.getTitle());
        minuteData.setMeetingNumber(getMeetingNumber(committee, meeting));

        minuteData.setCommitteeDescription(committee.getDescription());

        minuteData.setCommitteeName(getCommitteeDisplayName(committee));

        minuteData.setCoordinatorFullName(getFullNameOfParticipant(committee.getCoordinator(), committee.getMinuteLanguage()));
        minuteData.setChairmanFullName(getFullNameOfParticipant(
                meeting.getChairman() != null ? meeting.getChairman() : committee.getCoordinator(),
                committee.getMinuteLanguage()));

        minuteData.setDecisions(meeting.getDecisions().stream().map(decision -> new DecisionDto(decision.getDecisionId(), decision.getDecision())).toList());

        minuteData.setAgendas(meeting.getAgendas().stream().map(agenda -> new AgendaDto(agenda.getAgendaId(), agenda.getAgenda())).toList());

        minuteData.setParticipants(getParticipants(committee, meeting));

        minuteData.setOpeningParagraph(substitutePlaceholders(committee.getMinuteOpeningTemplate(), minuteData));
        minuteData.setHeader(substitutePlaceholders(committee.getMinuteHeaderTemplate(), minuteData));

        if (meeting.getMinuteContentHtml() != null && !meeting.getMinuteContentHtml().isBlank()) {
            minuteData.setMinuteContentHtml(meeting.getMinuteContentHtml());
        } else {
            String meetingTemplate = meeting.getMinuteTemplateHtml();
            if (meetingTemplate != null) {
                if (!meetingTemplate.isBlank()) {
                    minuteData.setMinuteContentHtml(renderFullMinuteTemplate(meetingTemplate, minuteData));
                }
            } else if (committee.getMinuteTemplateHtml() != null && !committee.getMinuteTemplateHtml().isBlank()) {
                // Compatibility for meetings created before per-meeting
                // snapshots were introduced.
                minuteData.setMinuteContentHtml(renderFullMinuteTemplate(committee.getMinuteTemplateHtml(), minuteData));
            }
        }

        return minuteData;
    }

    private void initializeMinuteGraph(Meeting meeting) {
        Hibernate.initialize(meeting.getDecisions());
        Hibernate.initialize(meeting.getAgendas());
        Hibernate.initialize(meeting.getAttendees());
        Hibernate.initialize(meeting.getInvitees());
        Hibernate.initialize(meeting.getChairman());

        Committee committee = meeting.getCommittee();
        if (committee != null) {
            Hibernate.initialize(committee.getMemberships());
            Hibernate.initialize(committee.getCoordinator());
            Hibernate.initialize(committee.getSecretary());
            committee.getMemberships().forEach(membership ->
                    Hibernate.initialize(membership.getMember()));
        }
    }

    /**
     * Renders the committee-authored full template with current structured
     * meeting data. This is used after AI refinement so the template, not the
     * model, remains the source of the document layout and prose.
     */
    public String renderCommitteeTemplate(Committee committee, MinuteDataDto data) {
        if (committee == null || committee.getMinuteTemplateHtml() == null
                || committee.getMinuteTemplateHtml().isBlank()) {
            return null;
        }
        return renderFullMinuteTemplate(committee.getMinuteTemplateHtml(), data);
    }

    /**
     * Renders the template frozen for this meeting. Legacy meetings without a
     * snapshot temporarily fall back to the committee template for backwards
     * compatibility.
     */
    public String renderMeetingTemplate(Meeting meeting, MinuteDataDto data) {
        if (meeting == null) {
            return null;
        }
        String template = meeting.getMinuteTemplateHtml();
        if (template == null && meeting.getCommittee() != null) {
            template = meeting.getCommittee().getMinuteTemplateHtml();
        }
        if (template == null || template.isBlank()) {
            return null;
        }
        return renderFullMinuteTemplate(template, data);
    }

    /**
     * Builds the document that is attached to the final-minute email. A
     * meeting-specific or committee template remains the source of truth; the
     * fallback keeps email delivery working for committees without a saved
     * template.
     */
    public String renderMinuteForEmail(Committee committee, Meeting meeting, String username) {
        MinuteDataDto data = prepareDataForMinute(committee, meeting, username);
        if (data.getMinuteContentHtml() != null && !data.getMinuteContentHtml().isBlank()) {
            return data.getMinuteContentHtml();
        }
        return renderFallbackMinuteHtml(data);
    }

    private String renderFallbackMinuteHtml(MinuteDataDto data) {
        boolean nepali = MinuteLanguage.NEPALI.equals(data.getMinuteLanguage());
        String chairmanLabel = nepali ? "\u0905\u0927\u094d\u092f\u0915\u094d\u0937" : "Chairman";
        String meetingNumberLabel = nepali ? "बैठक नं." : "Meeting no.";
        String meetingLabel = nepali ? "बैठकको विषय" : "Meeting";
        String dateLabel = nepali ? "मिति" : "Date";
        String timeLabel = nepali ? "समय" : "Time";
        String placeLabel = nepali ? "स्थान" : "Place";
        String attendanceLabel = nepali ? "उपस्थिति" : "Attendance";
        String agendasLabel = nepali ? "कार्यसूची" : "Agendas";
        String decisionsLabel = nepali ? "निर्णयहरू" : "Decisions and resolutions";

        return "<div id=\"a4-box\">"
                + "<div class=\"introduction\"><p class=\"introduction-body\"><strong>"
                + meetingNumberLabel + ":</strong> " + escapeHtml(data.getMeetingNumber())
                + "<br><strong>" + meetingLabel + ":</strong> " + escapeHtml(data.getMeetingTitle())
                + "<br><strong>" + dateLabel + ":</strong> " + escapeHtml(getDateForTemplate(data))
                + "<br><strong>" + timeLabel + ":</strong> " + escapeHtml(data.getMeetingHeldTime())
                + "<br><strong>" + placeLabel + ":</strong> " + escapeHtml(data.getMeetingHeldPlace())
                + "<br><strong>" + chairmanLabel + ":</strong> " + escapeHtml(data.getChairmanFullName())
                + "</p></div>"
                + "<div class=\"memberships\"><h5 class=\"heading\">" + attendanceLabel + "</h5>"
                + renderAttendance(data) + "</div>"
                + "<div class=\"agendas\"><h5 class=\"heading\">" + agendasLabel + "</h5>"
                + renderList(data.getAgendas(), data.getMinuteLanguage()) + "</div>"
                + "<div class=\"decisions\"><h5 class=\"heading\">" + decisionsLabel + "</h5>"
                + renderList(data.getDecisions(), data.getMinuteLanguage()) + "</div>"
                + "</div>";
    }

    /**
     * AI providers occasionally omit the attendance table even though it is
     * requested in the prompt. Normalize the generated fragment before it is
     * persisted so every rendered minute contains the meeting members.
     */
    public String ensureAttendanceTable(String htmlContent, MinuteDataDto data) {
        Document document = Jsoup.parseBodyFragment(htmlContent == null ? "" : htmlContent);
        Element body = document.body();
        Element attendanceTable = findAttendanceTable(body, data);

        if (attendanceTable == null) {
            String heading = MinuteLanguage.NEPALI.equals(data.getMinuteLanguage())
                    ? "\u0909\u092a\u0938\u094d\u0925\u093f\u0924\u093f"
                    : "Attendance";
            body.appendElement("h2").text(heading);
            attendanceTable = body.appendElement("table");
            Element headerRow = attendanceTable.appendElement("thead").appendElement("tr");
            if (MinuteLanguage.NEPALI.equals(data.getMinuteLanguage())) {
                headerRow.appendElement("th").text("\u0915\u094d\u0930.\u0938\u0902.");
                headerRow.appendElement("th").text("\u0928\u093e\u092e");
                headerRow.appendElement("th").text("\u092a\u0926");
                headerRow.appendElement("th").text("\u0939\u0938\u094d\u0924\u093e\u0915\u094d\u0937\u0930");
            } else {
                headerRow.appendElement("th").text("S.N.");
                headerRow.appendElement("th").text("Name");
                headerRow.appendElement("th").text("Position");
                headerRow.appendElement("th").text("Signature");
            }
        }

        Element bodyRows = attendanceTable.selectFirst("tbody");
        if (bodyRows == null) {
            bodyRows = attendanceTable.appendElement("tbody");
        }

        for (CommitteeMembershipDto participant : data.getParticipants()) {
            String fullName = participant.getFullName() == null ? "" : participant.getFullName();
            if (fullName.isBlank() || attendanceTable.text().contains(fullName)) {
                continue;
            }

            Element row = bodyRows.appendElement("tr");
            row.appendElement("td").text(String.valueOf(bodyRows.children().size()));
            row.appendElement("td").text(fullName);
            row.appendElement("td").text(participant.getRole() == null ? "" : participant.getRole());
            row.appendElement("td");
        }

        return body.html();
    }

    private Element findAttendanceTable(Element body, MinuteDataDto data) {
        for (Element table : body.select("table")) {
            String text = table.text().toLowerCase();
            boolean hasEnglishAttendanceHeaders = text.contains("signature")
                    && (text.contains("name") || text.contains("position"));
            boolean hasNepaliAttendanceHeaders = text.contains("\u0939\u0938\u094d\u0924\u093e\u0915\u094d\u0937\u0930")
                    && (text.contains("\u0928\u093e\u092e") || text.contains("\u092a\u0926"));
            boolean hasAttendanceHeaders = hasEnglishAttendanceHeaders || hasNepaliAttendanceHeaders;
            boolean hasParticipant = data.getParticipants().stream()
                    .map(CommitteeMembershipDto::getFullName)
                    .filter(name -> name != null && !name.isBlank())
                    .anyMatch(table.text()::contains);
            if (hasAttendanceHeaders || hasParticipant) {
                return table;
            }
        }
        return null;
    }

    /**
     * Resolves the placeholders available to a committee's full HTML template.
     * Values originating from the meeting are escaped; the generated section
     * fragments are intentionally HTML because they are inserted into the
     * administrator-authored template.
     */
    private String renderFullMinuteTemplate(String template, MinuteDataDto data) {
        String rendered = template;
        rendered = replaceToken(rendered, escapeHtml(data.getCommitteeName()), "committeeName", "committee", "committe");
        rendered = replaceToken(rendered, escapeHtml(data.getCommitteeDescription()), "committeeDescription", "purpose");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingTitle()), "meetingTitle", "title");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingNumber()), "meetingNumber", "meetingNo");
        rendered = replaceToken(rendered, escapeHtml(getDateForTemplate(data)), "date", "data");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldDay()), "day");
        rendered = replaceToken(rendered, escapeHtml(data.getPartOfDay()), "partOfDay");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldTime()), "time");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldPlace()), "place", "location");
        rendered = replaceToken(rendered, escapeHtml(data.getCoordinatorFullName()), "coordinator");
        rendered = replaceToken(rendered, escapeHtml(data.getChairmanFullName()), "chairman", "chairperson");
        rendered = replaceToken(rendered, textFragment(data.getHeader()), "header");
        rendered = replaceToken(rendered, textFragment(data.getOpeningParagraph()), "openingParagraph");
        // The legacy @attendance token remains a table. Template authors can
        // explicitly choose @attendanceTable or @attendanceList.
        rendered = replaceToken(rendered, renderAttendance(data), "attendanceTable");
        rendered = replaceToken(rendered, renderAttendanceList(data), "attendanceList");
        rendered = replaceToken(rendered, renderAttendance(data), "attendance", "participants");
        // Keep a semantic wrapper around live structured sections. Committee
        // templates are arbitrary HTML, so the token can be nested inside a
        // paragraph/div rather than placed directly under an agenda/decision
        // heading. The wrapper gives the minute editor a stable way to find
        // the section again when structured values are changed.
        rendered = replaceToken(rendered,
                renderStructuredSection("agendas", renderList(data.getAgendas(), data.getMinuteLanguage())),
                "agendas");
        rendered = replaceToken(rendered,
                renderStructuredSection("decisions", renderList(data.getDecisions(), data.getMinuteLanguage())),
                "decisions");

        boolean nepali = MinuteLanguage.NEPALI.equals(data.getMinuteLanguage());

        if (!templateContainsToken(template, "decisions")) {
            rendered += "\n<h2 style=\"margin-top:2rem\">" + (nepali ? "\u0928\u093f\u0930\u094d\u0923\u092f\u0939\u0942" : "Decisions and resolutions") + "</h2>\n"
                    + renderStructuredSection("decisions", renderList(data.getDecisions(), data.getMinuteLanguage()));
        }

        return rendered;
    }

    private String renderStructuredSection(String section, String renderedList) {
        return "<div class=\"" + section + " minute-structured-section\">" + renderedList + "</div>";
    }

    private boolean templateContainsToken(String template, String... names) {
        if (template == null) return false;
        for (String name : names) {
            if (template.contains("{{" + name + "}}") || template.contains("{" + name + "}") || template.contains("@" + name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replace both the original brace syntax and the shorter @ syntax used by
     * the committee template editor. The aliases make the editor read like
     * normal prose: @committee, @date, @location, @purpose, @coordinator and @chairman.
     */
    private String replaceToken(String template, String value, String... names) {
        String safeValue = value == null ? "" : value;
        String rendered = template;
        for (String name : names) {
            rendered = rendered.replace("{{" + name + "}}", safeValue)
                    .replace("{" + name + "}", safeValue)
                    .replace("@" + name, safeValue);
        }
        return rendered;
    }

    private String renderAttendance(MinuteDataDto data) {
        boolean nepali = MinuteLanguage.NEPALI.equals(data.getMinuteLanguage());
        String serialHeader = nepali ? "\u0915\u094d\u0930.\u0938\u0902." : "S.N.";
        String nameHeader = nepali ? "\u0928\u093e\u092e" : "Name";
        String roleHeader = nepali ? "\u092a\u0926/\u092d\u0942\u092e\u093f\u0915\u093e" : "Position";
        String signatureHeader = nepali ? "\u0939\u0938\u094d\u0924\u093e\u0915\u094d\u0937\u0930" : "Signature";
        StringBuilder html = new StringBuilder(
                "<table class=\"memberships\"><thead><tr><th>"
                        + serialHeader + "</th><th>" + nameHeader + "</th><th>"
                        + roleHeader + "</th><th>" + signatureHeader + "</th></tr></thead><tbody>");
        int index = 1;
        for (CommitteeMembershipDto participant : data.getParticipants()) {
            html.append("<tr><td>").append(index++).append("</td><td>")
                    .append(escapeHtml(participant.getFullName())).append("</td><td>")
                    .append(escapeHtml(participant.getRole())).append("</td><td></td></tr>");
        }
        return html.append("</tbody></table>").toString();
    }

    private String renderAttendanceList(MinuteDataDto data) {
        StringBuilder html = new StringBuilder("<div class=\"attendance-list\">");
        int index = 1;
        boolean nepali = MinuteLanguage.NEPALI.equals(data.getMinuteLanguage());
        for (CommitteeMembershipDto participant : data.getParticipants()) {
            String num = String.valueOf(index++);
            if (nepali) {
                num = toNepaliDigits(num);
            }
            html.append("<p>").append(num).append(". ")
                    .append(escapeHtml(participant.getFullName()));
            if (participant.getRole() != null && !participant.getRole().isBlank()) {
                html.append(" — ").append(escapeHtml(participant.getRole()));
            }
            html.append("</p>");
        }
        return html.append("</div>").toString();
    }

    private String renderList(List<?> items, MinuteLanguage language) {
        StringBuilder html = new StringBuilder("<div class=\"minute-list\">");
        int index = 1;
        boolean nepali = MinuteLanguage.NEPALI.equals(language);
        for (Object item : items) {
            String value = item instanceof AgendaDto agenda ? agenda.getAgenda() : ((DecisionDto) item).getDecision();
            String num = String.valueOf(index++);
            if (nepali) {
                num = toNepaliDigits(num);
            }
            html.append("<p>").append(num).append(". ").append(escapeHtml(value)).append("</p>");
        }
        return html.append("</div>").toString();
    }

    private String textFragment(String value) {
        return value == null ? "" : "<p>" + escapeHtml(value).replace("\n", "<br>") + "</p>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Substitutes a committee's editable minute template (opening paragraph or header) with
     * this meeting's values. Returns null when the template is empty so the frontend falls
     * back to its built-in default / renders nothing.
     */
    private String substitutePlaceholders(String template, MinuteDataDto data) {
        if (template == null || template.isBlank()) {
            return null;
        }
        String rendered = template;
        rendered = replaceToken(rendered, nullSafe(data.getCommitteeName()), "committeeName", "committee", "committe");
        rendered = replaceToken(rendered, nullSafe(data.getCommitteeDescription()), "committeeDescription", "purpose");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingTitle()), "meetingTitle", "title");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingNumber()), "meetingNumber", "meetingNo");
        rendered = replaceToken(rendered, getDateForTemplate(data), "date", "data");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldDay()), "day");
        rendered = replaceToken(rendered, nullSafe(data.getPartOfDay()), "partOfDay");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldTime()), "time");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldPlace()), "place", "location");
        rendered = replaceToken(rendered, nullSafe(data.getCoordinatorFullName()), "coordinator");
        return replaceToken(rendered, nullSafe(data.getChairmanFullName()), "chairman", "chairperson");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }


    private void setDates(MinuteDataDto minuteDataDto, Meeting meeting) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        minuteDataDto.setMeetingHeldDate(meeting.getHeldDate());
        minuteDataDto.setMeetingHeldDateNepali(nepaliDateService.toNepaliDate(meeting.getHeldDate()));
    }

    private String getDateForTemplate(MinuteDataDto data) {
        if (MinuteLanguage.NEPALI.equals(data.getMinuteLanguage())
                && data.getMeetingHeldDateNepali() != null
                && !data.getMeetingHeldDateNepali().isBlank()) {
            return toNepaliDigits(data.getMeetingHeldDateNepali());
        }
        return data.getMeetingHeldDate() == null ? "" : data.getMeetingHeldDate().toString();
    }

    private String getMeetingNumber(Committee committee, Meeting meeting) {
        if (meetingRepository == null || committee == null || committee.getId() == null) {
            return formatMeetingNumber(1, committee);
        }
        List<Meeting> orderedMeetings = meetingRepository.findByCommitteeIdWithAgendas(committee.getId()).stream()
                .filter(candidate -> candidate.getId() != null)
                .sorted(java.util.Comparator.comparing(Meeting::getId))
                .toList();
        int position = 1;
        for (int index = 0; index < orderedMeetings.size(); index++) {
            if (java.util.Objects.equals(orderedMeetings.get(index).getId(), meeting.getId())) {
                position = index + 1;
                break;
            }
        }
        return formatMeetingNumber(position, committee);
    }

    private String formatMeetingNumber(int position, Committee committee) {
        String number = String.valueOf(position);
        return committee != null && MinuteLanguage.NEPALI.equals(committee.getMinuteLanguage())
                ? toNepaliDigits(number)
                : number;
    }

    private String toNepaliDigits(String value) {
        return value
                .replace('0', '\u0966')
                .replace('1', '\u0967')
                .replace('2', '\u0968')
                .replace('3', '\u0969')
                .replace('4', '\u096A')
                .replace('5', '\u096B')
                .replace('6', '\u096C')
                .replace('7', '\u096D')
                .replace('8', '\u096E')
                .replace('9', '\u096F');
    }

    private List<CommitteeMembershipDto> getDefaultParticipants(Committee committee, Meeting meeting) {
        List<CommitteeMembershipDto> memberships;

        memberships = committee.getSortedMemberships().stream().map(membership -> {
            Member member = membership.getMember();
            String fullName = getFullNameOfParticipant(member, committee.getMinuteLanguage());
            return new CommitteeMembershipDto(member.getId(), fullName, membership.getRole());
        }).collect(Collectors.toCollection(ArrayList::new));

        String coordinatorRole = committee.getMinuteLanguage() == MinuteLanguage.ENGLISH ? "Coordinator" : "संयोजक";

        memberships.addFirst(new CommitteeMembershipDto(committee.getCoordinator().getId(), getFullNameOfParticipant(committee.getCoordinator(), committee.getMinuteLanguage()), coordinatorRole ));
        Member chairman = meeting.getChairman() != null ? meeting.getChairman() : committee.getCoordinator();
        memberships.removeIf(participant -> participant.getMemberId().equals(chairman.getId()));
        memberships.addFirst(new CommitteeMembershipDto(
                chairman.getId(),
                getFullNameOfParticipant(chairman, committee.getMinuteLanguage()),
                committee.getMinuteLanguage() == MinuteLanguage.ENGLISH ? "Chairman" : "\u0905\u0927\u094d\u092f\u0915\u094d\u0937"));

        meeting.getInvitees().forEach( invitee -> {
            String fullname = getFullNameOfParticipant(invitee, committee.getMinuteLanguage());
            String role;
            if(committee.getMinuteLanguage() == MinuteLanguage.ENGLISH) {
                role = "Invitee";
            } else {
                role = "आमन्त्रित";
            }
            memberships.add(new CommitteeMembershipDto(invitee.getId(), fullname, role));
        });
        return memberships;
    }

    private List<CommitteeMembershipDto> getParticipants(Committee committee, Meeting meeting) {
        List<CommitteeMembershipDto> defaults = getDefaultParticipants(committee, meeting);
        if (meeting.getAttendees() == null || meeting.getAttendees().isEmpty()) {
            return defaults;
        }

        Map<Integer, CommitteeMembershipDto> byMemberId = defaults.stream()
                .filter(participant -> participant.getMemberId() != null)
                .collect(Collectors.toMap(CommitteeMembershipDto::getMemberId, participant -> participant,
                        (left, right) -> left));
        List<CommitteeMembershipDto> ordered = new ArrayList<>();
        Set<Integer> orderedMemberIds = new HashSet<>();
        for (Member attendee : meeting.getAttendees()) {
            CommitteeMembershipDto participant = byMemberId.get(attendee.getId());
            if (participant != null && orderedMemberIds.add(participant.getMemberId())) {
                ordered.add(participant);
            }
        }
        defaults.stream()
                .filter(participant -> participant.getMemberId() != null
                        && orderedMemberIds.add(participant.getMemberId()))
                .forEach(ordered::add);
        return ordered.isEmpty() ? defaults : ordered;
    }

    private String getFullNameOfParticipant(Member member, MinuteLanguage language) {
        String titleSource = MinuteLanguage.NEPALI.equals(language) && hasText(member.getTitleNepali())
                ? member.getTitleNepali() : member.getTitle();
        String title = localizeTitle(titleSource, language);
        String firstName = MinuteLanguage.NEPALI.equals(language) && hasText(member.getFirstNameNepali())
                ? member.getFirstNameNepali() : member.getFirstName();
        String lastName = MinuteLanguage.NEPALI.equals(language) && hasText(member.getLastNameNepali())
                ? member.getLastNameNepali() : member.getLastName();
        String fullname =  (title + " " + firstName + " " + lastName).trim();
        if(member.getPost() != null && !member.getPost().isBlank()) {
           fullname = fullname + ", " + member.getPost();
        } else if(member.getInstitution() != null && !member.getInstitution().isBlank()) {
            fullname = fullname + ", " + member.getInstitution();
        }
        return fullname;
    }

    private String getCommitteeDisplayName(Committee committee) {
        if (MinuteLanguage.NEPALI.equals(committee.getMinuteLanguage())
                && hasText(committee.getNepaliName())) {
            return committee.getNepaliName().trim();
        }
        return committee.getName();
    }

    private String localizeTitle(String title, MinuteLanguage language) {
        if (!hasText(title)) {
            return "";
        }
        String normalized = title.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[.,]", "")
                .replaceAll("\\s+", " ");
        boolean nepali = MinuteLanguage.NEPALI.equals(language);
        if (!nepali && "prof".equals(normalized)) {
            return title.trim();
        }
        return switch (normalized) {
            case "pra", "prof", "professor", "प्रा" -> nepali ? "\u092a\u094d\u0930\u093e." : "Professor";
            case "upra", "assistant professor", "asst professor", "asst. professor", "उप्रा" -> nepali ? "\u0909\u092a\u094d\u0930\u093e." : "Assistant Professor";
            case "associate professor", "assoc professor", "सहप्राध्यापक" -> nepali ? "\u0938\u0939\u092a\u094d\u0930\u093e\u0927\u094d\u092f\u093e\u092a\u0915" : "Associate Professor";
            case "da", "dr", "doctor", "डा" -> nepali ? "\u0921\u093e." : "Dr.";
            default -> title.trim();
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private String getMeetingHeldDay(LocalDate date, MinuteLanguage language) {
        if (MinuteLanguage.NEPALI.equals(language)) {
            return switch (date.getDayOfWeek()) {
                case SUNDAY -> "आइतबार";
                case MONDAY -> "सोमबार";
                case TUESDAY -> "मंगलबार";
                case WEDNESDAY -> "बुधबार";
                case THURSDAY -> "बिहीबार";
                case FRIDAY -> "शुक्रबार";
                case SATURDAY -> "शनिबार";
                default -> "";
            };
        }

        return switch (date.getDayOfWeek()) {
            case SUNDAY -> "Sunday";
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            default -> "";
        };
    }

    private String getPartOfDay(LocalTime time, MinuteLanguage language) {
        int hour = time.getHour();
        String partOfDay;

        if (hour >= 5 && hour < 12) {
            if (language.equals(MinuteLanguage.NEPALI))
                partOfDay = "बिहान";
            else
                partOfDay = "Morning";
        } else if (hour >= 12 && hour < 17) {
            if (language.equals(MinuteLanguage.NEPALI))
                partOfDay = "दिउँसो";
            else
                partOfDay = "Afternoon";
        } else if (hour >= 17 && hour < 21) {
            if (language.equals(MinuteLanguage.NEPALI))
                partOfDay = "साँझ";
            else
                partOfDay = "Evening";
        } else {
            if (language.equals(MinuteLanguage.NEPALI))
                partOfDay = "राति";
            else
                partOfDay = "Night";
        }
        return partOfDay;
    }


    //----------------------------------------------------------------------------------
    //This part is for msword creation
    //TODO: Consider moving the code below to a separate service like: MeetingMinuteWordPreparationService


    public String renderHtmlTemplate(String templateName, Map<String, Object> dataModel) {
        Context context = new Context();
        dataModel.forEach(context::setVariable);

        return templateEngine.process(templateName, context);
    }

    /*
    Possible classes that our templates can have:
    1. introduction -> signifies sections
    2. justify-text -> styling
    3. heading -> styling
    4. memberships -> signifies sections
    5. decisions -> signifies sections

    Structure of the template:

    #a4-box
        #introduction
            #introduction-body
        #memberships
            #heading
            #table
        #agendas  (only have this section if agenda isn't empty)
            #heading-agendas
            #agendas-list
        #decisions
            #heading-decisions
            #decisions-list
    */

    public byte[] createWordDocumentFromHtml(String htmlContent) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            Document html = Jsoup.parse(htmlContent);

            XWPFParagraph paragraph = null;
            XWPFRun run = null;
            Element a4_box = html.getElementById("a4-box");
            if (a4_box == null) {
                a4_box = html.body();
            }


            for (Element element : a4_box.children()) {
                if (element.className().contains("introduction")) {

                    //rest of the classes(which are used for styling)
                    List<String> stylings = Arrays.asList(element.className().split("\\s+"));

                    Elements children = element.children();
                    for (Element child : children) {
                        if (child.className().equals("introduction-body")) {
                            paragraph = document.createParagraph();
                            paragraph.setSpacingAfter(100);
                            run = paragraph.createRun();
                            run.setText(element.text());

                            if (stylings.contains("justify-text")) {
                                styleJustifyText(paragraph);
                            }
                        }
                    }
                } else if (element.className().contains("memberships")) {
                    Elements children = element.children();

                    //attendee has two children, a heading, and the table
                    for (Element child : children) {

                        if (child.className().contains("heading")) {
                            paragraph = document.createParagraph();
                            paragraph.setSpacingBefore(100);
                            paragraph.setSpacingAfter(200);
                            styleHeading(paragraph.createRun(), child);
                        }

                        if (child.nodeName().equals("table")) {
                            XWPFTable newTable = document.createTable();
                            final int PADDING_LEFT = 100;
                            final int PADDING_TOP = 100;
                            newTable.setCellMargins(PADDING_TOP, PADDING_LEFT, 0, 0);
                            newTable.setWidth(XWPFTable.DEFAULT_PERCENTAGE_WIDTH);

                            copyTable(newTable, child);
                        }
                    }
                } else if (element.className().contains("agendas")) {
                    Elements children = element.children();

                    //deicisions has two children, a heading, and a list
                    for (Element child : children) {
                        if (child.className().contains("heading")) {
                            paragraph = document.createParagraph();
                            paragraph.setSpacingBefore(200);
                            paragraph.setSpacingAfter(200);
                            run = paragraph.createRun();
                            styleHeading(run, child);
                        }

                        if (child.nodeName().equals("ol")) {
                            Elements agendas = child.children();
                            int count = 1;
                            final int DECISION_SPACING = 17;

                            // 1. Create numbering instance
                            XWPFNumbering numbering = document.createNumbering();

                            // 2. Define abstract numbering style (numbered list)
                            CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
                            abstractNum.setAbstractNumId(BigInteger.ZERO);

                            CTLvl level = abstractNum.addNewLvl();
                            level.setIlvl(BigInteger.ZERO);
                            level.addNewNumFmt().setVal(STNumberFormat.HINDI_NUMBERS);
                            level.addNewLvlText().setVal("%1.");
                            level.addNewStart().setVal(BigInteger.ONE);

                            // 3. Add the abstract numbering to document
                            XWPFAbstractNum xwpfAbstractNum = new XWPFAbstractNum(abstractNum);
                            BigInteger abstractNumID = numbering.addAbstractNum(xwpfAbstractNum);

                            // 4. Create a numbering instance for the list
                            BigInteger numID = numbering.addNum(abstractNumID);

                            // 5. Now add decisions with automatic numbering
                            for (Element agenda : agendas) {
                                paragraph = document.createParagraph();
                                paragraph.setNumID(numID);  // ← This line activates numbering!

                                // Fix line wrapping for numbered list
                                paragraph.setIndentationLeft(720);      // Indent whole paragraph
                                paragraph.setIndentationHanging(360);   // Hanging indent for number alignment

                                run = paragraph.createRun();
                                run.setText(listItemText(agenda));
                            }
                        } else if (child.className().contains("minute-list")) {
                            for (Element item : child.children()) {
                                paragraph = document.createParagraph();
                                paragraph.setIndentationLeft(720);      
                                paragraph.setIndentationHanging(360);   
                                run = paragraph.createRun();
                                run.setText(item.text().trim());
                            }
                        }
                    }
                } else if (element.className().contains("decisions")) {
                    Elements children = element.children();

                    //deicisions has two children, a heading, and a list
                    for (Element child : children) {
                        if (child.className().contains("heading")) {
                            paragraph = document.createParagraph();
                            paragraph.setSpacingBefore(200);
                            paragraph.setSpacingAfter(200);
                            run = paragraph.createRun();
                            styleHeading(run, child);
                        }

                        if (child.nodeName().equals("ol")) {
                            Elements decisions = child.children();
                            int count = 1;
                            final int DECISION_SPACING = 17;

                            // 1. Create numbering instance
                            XWPFNumbering numbering = document.createNumbering();

                            // 2. Define abstract numbering style (numbered list)
                            CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
                            abstractNum.setAbstractNumId(BigInteger.ONE);

                            CTLvl level = abstractNum.addNewLvl();
                            level.setIlvl(BigInteger.ZERO);
                            level.addNewNumFmt().setVal(STNumberFormat.HINDI_NUMBERS);
                            level.addNewLvlText().setVal("%1.");
                            level.addNewStart().setVal(BigInteger.ONE);

                            // 3. Add the abstract numbering to document
                            XWPFAbstractNum xwpfAbstractNum = new XWPFAbstractNum(abstractNum);
                            BigInteger abstractNumID = numbering.addAbstractNum(xwpfAbstractNum);

                            // 4. Create a numbering instance for the list
                            BigInteger numID = numbering.addNum(abstractNumID);

                            // 5. Now add decisions with automatic numbering
                            for (Element decision : decisions) {
                                paragraph = document.createParagraph();
                                paragraph.setNumID(numID);  // ← This line activates numbering!

                                // Fix line wrapping for numbered list
                                paragraph.setIndentationLeft(720);      // Indent whole paragraph
                                paragraph.setIndentationHanging(360);   // Hanging indent for number alignment

                                run = paragraph.createRun();
                                run.setText(listItemText(decision));
                            }
                        } else if (child.className().contains("minute-list")) {
                            for (Element item : child.children()) {
                                paragraph = document.createParagraph();
                                paragraph.setIndentationLeft(720);      
                                paragraph.setIndentationHanging(360);   
                                run = paragraph.createRun();
                                run.setText(item.text().trim());
                            }
                        }
                    }
                }
            }

            // Committee templates and AI drafts are not required to use the
            // built-in section classes above. If the structured converter did
            // not recognize any section, preserve generic headings,
            // paragraphs, lists, and tables instead of returning a blank DOCX.
            if (document.getParagraphs().isEmpty() && document.getTables().isEmpty()) {
                appendGenericHtml(document, a4_box);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            byte[] bytes = out.toByteArray();
            return bytes;
        } catch (Exception e) {
            throw e;
        }
    }

    private void appendGenericHtml(XWPFDocument document, Element root) {
        for (Element child : root.children()) {
            appendGenericElement(document, child);
        }
    }

    private void appendGenericElement(XWPFDocument document, Element element) {
        String tagName = element.tagName();

        if (tagName.equals("table")) {
            XWPFTable table = document.createTable();
            table.setWidth(XWPFTable.DEFAULT_PERCENTAGE_WIDTH);
            copyTable(table, element);
            return;
        }

        if (tagName.matches("h[1-6]")) {
            XWPFParagraph paragraph = document.createParagraph();
            styleHeading(paragraph.createRun(), element);
            return;
        }

        if (tagName.equals("ol") || tagName.equals("ul")) {
            int index = 1;
            for (Element item : element.children()) {
                if (!item.tagName().equals("li") || item.text().isBlank()) {
                    continue;
                }

                XWPFParagraph paragraph = document.createParagraph();
                String prefix = tagName.equals("ol") ? index++ + ". " : "• ";
                paragraph.createRun().setText(prefix + item.text().trim());
            }
            return;
        }

        boolean hasBlockChild = false;
        for (Element child : element.children()) {
            if (isBlockElement(child)) {
                hasBlockChild = true;
                appendGenericElement(document, child);
            }
        }

        if (!hasBlockChild && !element.text().isBlank()) {
            document.createParagraph().createRun().setText(element.text().trim());
        }
    }

    private String listItemText(Element item) {
        return item.text().trim().replaceFirst("^\\d+[.)]\\s*", "");
    }

    private boolean isBlockElement(Element element) {
        return switch (element.tagName()) {
            case "address", "article", "blockquote", "div", "footer", "h1", "h2", "h3", "h4", "h5", "h6",
                    "header", "li", "ol", "p", "section", "table", "ul" -> true;
            default -> false;
        };
    }


    public void styleJustifyText(XWPFParagraph paragraph) {
        paragraph.setAlignment(ParagraphAlignment.BOTH);
    }

    public void styleHeading(XWPFRun run, Element element) {
        run.setText(element.text());
        run.setBold(true);
        run.setUnderline(UnderlinePatterns.SINGLE);
    }


    //copies html table to msword table
    public void copyTable(XWPFTable newTable, Element oldTable) {
        //getting all the rows
        Elements oldRows = oldTable.select("tr");

        //iterate through the rows
        for (int i = 0; i < oldRows.size(); i++) {
            Element oldRow = oldRows.get(i);

            //getting the individual cells
            Elements oldCells = oldRow.select("th, td");

            //create new row(skip first because Apache POI creates one by default)
            XWPFTableRow newTableRow = (i == 0) ? newTable.getRow(0) : newTable.createRow();


            //set the min-height of the table row
            final int ROW_HEIGHT = 600;
            newTableRow.setHeight(ROW_HEIGHT);
            newTableRow.setHeightRule(TableRowHeightRule.AT_LEAST);

            //get the data from each cell and populate the XWPFTableRow
            for (int j = 0; j < oldCells.size(); j++) {
                String oldCellText = oldCells.get(j).text();
                //remove the first cell in the first row which is pre-built by the framework
                if (i == 0 && j == 0) {
                    newTableRow.removeCell(0);
                }

                XWPFTableCell cell = null;
                //only create new cells, if jth cell does not exist
                if (newTableRow.getTableCells().size() < j + 1) {
                    cell = newTableRow.createCell();
                } else {
                    cell = newTableRow.getCell(j);
                }

                //remove the pre-built paragraph
                cell.removeParagraph(0);
                XWPFParagraph para = cell.addParagraph();
                XWPFRun run = para.createRun();
                run.setText(oldCellText);

                if (j == 0) {
                    newTableRow.getCell(j).setWidth("5%");
                } else if (j == 1 || j == 2) {
                    newTableRow.getCell(j).setWidth("30%");
                } else if (j == 3) {
                    newTableRow.getCell(j).setWidth("35%");
                }
            }
        }
    }
}
