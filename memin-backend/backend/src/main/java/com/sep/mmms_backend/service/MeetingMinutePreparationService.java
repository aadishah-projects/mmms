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
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MeetingMinutePreparationService {
    private final TemplateEngine templateEngine;

    public MeetingMinutePreparationService(MeetingService meetingService, CommitteeRepository committeeRepository, MemberService memberService, TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }


    @Transactional(readOnly = true)
    @CheckCommitteeAccess(shouldValidateMeeting = true)
    public MinuteDataDto prepareDataForMinute(Committee committee, Meeting meeting, String username) {
        MinuteDataDto minuteData = new MinuteDataDto();
        setDates(minuteData, meeting);

        minuteData.setMinuteLanguage(committee.getMinuteLanguage());

        minuteData.setMeetingHeldDay(getMeetingHeldDay(meeting.getHeldDate(), committee.getMinuteLanguage()));

        minuteData.setPartOfDay(getPartOfDay(meeting.getHeldTime(), committee.getMinuteLanguage()));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
        String meetingHeldTime = meeting.getHeldTime().format(timeFormatter);

        minuteData.setMeetingHeldTime(meetingHeldTime);

        minuteData.setMeetingHeldPlace(meeting.getHeldPlace());
        minuteData.setMeetingTitle(meeting.getTitle());

        minuteData.setCommitteeDescription(committee.getDescription());

        minuteData.setCommitteeName(meeting.getCommittee().getName());

        minuteData.setCoordinatorFullName(getCoordinatorFullName(committee));

        minuteData.setDecisions(meeting.getDecisions().stream().map(decision -> new DecisionDto(decision.getDecisionId(), decision.getDecision())).toList());

        minuteData.setAgendas(meeting.getAgendas().stream().map(agenda -> new AgendaDto(agenda.getAgendaId(), agenda.getAgenda())).toList());

        minuteData.setParticipants(getParticipants(committee, meeting));

        minuteData.setOpeningParagraph(substitutePlaceholders(committee.getMinuteOpeningTemplate(), minuteData));
        minuteData.setHeader(substitutePlaceholders(committee.getMinuteHeaderTemplate(), minuteData));

        if (meeting.getMinuteContentHtml() != null && !meeting.getMinuteContentHtml().isBlank()) {
            minuteData.setMinuteContentHtml(meeting.getMinuteContentHtml());
        } else if (committee.getMinuteTemplateHtml() != null && !committee.getMinuteTemplateHtml().isBlank()) {
            minuteData.setMinuteContentHtml(renderFullMinuteTemplate(committee.getMinuteTemplateHtml(), minuteData));
        }

        return minuteData;
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
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldDate() == null ? "" : data.getMeetingHeldDate().toString()), "date", "data");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldDay()), "day");
        rendered = replaceToken(rendered, escapeHtml(data.getPartOfDay()), "partOfDay");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldTime()), "time");
        rendered = replaceToken(rendered, escapeHtml(data.getMeetingHeldPlace()), "place", "location");
        rendered = replaceToken(rendered, escapeHtml(data.getCoordinatorFullName()), "coordinator");
        rendered = replaceToken(rendered, textFragment(data.getHeader()), "header");
        rendered = replaceToken(rendered, textFragment(data.getOpeningParagraph()), "openingParagraph");
        rendered = replaceToken(rendered, renderAttendance(data), "attendance", "participants");
        rendered = replaceToken(rendered, renderList(data.getAgendas(), true), "agendas");
        rendered = replaceToken(rendered, renderList(data.getDecisions(), false), "decisions");
        return rendered;
    }

    /**
     * Replace both the original brace syntax and the shorter @ syntax used by
     * the committee template editor. The aliases make the editor read like
     * normal prose: @committee, @date, @location, @purpose and @coordinator.
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

    private String renderList(List<?> items, boolean agendas) {
        StringBuilder html = new StringBuilder("<ol>");
        int index = 1;
        for (Object item : items) {
            String value = item instanceof AgendaDto agenda ? agenda.getAgenda() : ((DecisionDto) item).getDecision();
            html.append("<li>").append(escapeHtml(value)).append("</li>");
            index++;
        }
        return html.append("</ol>").toString();
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
        rendered = replaceToken(rendered, data.getMeetingHeldDate() != null ? data.getMeetingHeldDate().toString() : "", "date", "data");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldDay()), "day");
        rendered = replaceToken(rendered, nullSafe(data.getPartOfDay()), "partOfDay");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldTime()), "time");
        rendered = replaceToken(rendered, nullSafe(data.getMeetingHeldPlace()), "place", "location");
        return replaceToken(rendered, nullSafe(data.getCoordinatorFullName()), "coordinator");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }


    private void setDates(MinuteDataDto minuteDataDto, Meeting meeting) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        minuteDataDto.setMeetingHeldDate(meeting.getHeldDate());
    }

    private List<CommitteeMembershipDto> getParticipants(Committee committee, Meeting meeting) {
        List<CommitteeMembershipDto> memberships;

        memberships = committee.getSortedMemberships().stream().map(membership -> {
            Member member = membership.getMember();
            String fullName = getFullNameOfParticipant(member);
            return new CommitteeMembershipDto(fullName, membership.getRole());
        }).collect(Collectors.toCollection(ArrayList::new));

        String coordinatorRole = committee.getMinuteLanguage() == MinuteLanguage.ENGLISH ? "Coordinator" : "संयोजक";

        memberships.addFirst(new CommitteeMembershipDto(getFullNameOfParticipant(committee.getCoordinator()),coordinatorRole ));

        meeting.getInvitees().forEach( invitee -> {
            String fullname = getFullNameOfParticipant(invitee);
            String role;
            if(committee.getMinuteLanguage() == MinuteLanguage.ENGLISH) {
                role = "Invitee";
            } else {
                role = "आमन्त्रित";
            }
            memberships.add(new CommitteeMembershipDto(fullname, role));
        });
        return memberships;
    }

    private String getFullNameOfParticipant(Member member) {
        String fullname =  member.getTitle() + " " + member.getFirstName() + " " + member.getLastName();
        if(member.getPost() != null && !member.getPost().isBlank()) {
           fullname = fullname + ", " + member.getPost();
        } else if(member.getInstitution() != null && !member.getInstitution().isBlank()) {
            fullname = fullname + ", " + member.getInstitution();
        }
        return fullname;
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


    private String getCoordinatorFullName(Committee committee) {
        return committee.getCoordinator().getTitle() + " " + committee.getCoordinator().getFirstName() + " " + committee.getCoordinator().getLastName();
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
        System.out.println(htmlContent);
        try (XWPFDocument document = new XWPFDocument()) {
            Document html = Jsoup.parse(htmlContent);

            XWPFParagraph paragraph = null;
            XWPFRun run = null;
            Element a4_box = html.getElementById("a4-box");
            if (a4_box == null) {
                throw new Exception();
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
                                run.setText(agenda.text().substring(3));
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
                                run.setText(decision.text().substring(3));
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
