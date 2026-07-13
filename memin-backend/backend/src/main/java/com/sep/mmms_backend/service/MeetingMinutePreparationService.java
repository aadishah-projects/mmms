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
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.springframework.stereotype.Service;
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

        minuteData.setCommitteeDescription(committee.getDescription());

        minuteData.setCommitteeName(meeting.getCommittee().getName());

        minuteData.setCoordinatorFullName(getCoordinatorFullName(committee));

        minuteData.setDecisions(meeting.getDecisions().stream().map(decision -> new DecisionDto(decision.getDecisionId(), decision.getDecision())).toList());

        minuteData.setAgendas(meeting.getAgendas().stream().map(agenda -> new AgendaDto(agenda.getAgendaId(), agenda.getAgenda())).toList());

        minuteData.setParticipants(getParticipants(committee, meeting));

        return minuteData;
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            byte[] bytes = out.toByteArray();
            return bytes;
        } catch (Exception e) {
            throw e;
        }
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
