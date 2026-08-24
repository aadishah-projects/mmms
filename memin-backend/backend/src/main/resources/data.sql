INSERT INTO app_users (username, password, email, firstname, lastname, role, linked_member_id)
VALUES ('username', '{noop}password', 'username@gmail.com', 'admin', 'admin', 'DEPARTMENT_HEAD', 1),
       ('not_username', '{noop}password', 'username@gmail.com', 'admin', 'admin', 'DEPARTMENT_MEMBER', 2);

INSERT INTO members (member_title, member_post,
                     member_created_by, member_created_date, member_modified_by, member_modified_date, member_uuid,
                     member_first_name, member_last_name,
                     member_first_name_nepali, member_last_name_nepali,
                     member_title_nepali)
VALUES ('Prof.', 'Campus Chief', 'username', '2025-07-04', 'username', '2025-07-04', RANDOM_UUID(), 'Hari', 'Bahadur', 'हरि', 'बहादुर', 'प्रा.'),
       ('Dr.', 'IOM Delegate', 'username', '2025-07-05', 'username', '2025-07-05', RANDOM_UUID(), 'Gita', 'Oli', 'गिता', 'ओली', 'डा.'),
       ('Dr.', 'IMO Chief', 'username', '2025-07-06', 'username', '2025-07-06', RANDOM_UUID(), 'Bikash', 'Lama', 'विकाश', 'लामा', 'डा.'),
       ('Prof.', 'MSc Environmental Science', 'username', '2025-07-07', 'username', '2025-07-07', RANDOM_UUID(), 'Sunita',
        'Maharjan', 'सुनिता', 'महार्जन', 'प्रा.'),
       ('Dr.', 'PhD Civil Engineering', 'username', '2025-07-12', 'username', '2025-07-12', RANDOM_UUID(), 'Kamal', 'Pandey', 'कमल', 'पाण्डे', 'डा.'),
       ('Prof.', 'MBBS', 'username', '2025-07-13', 'username', '2025-07-13', RANDOM_UUID(), 'Deepa', 'Gurung', 'दीपा', 'गुरुङ', 'प्रा.'),
       ('Prof.', 'MSc Computer Science', 'username', '2025-07-14', 'username', '2025-07-14', RANDOM_UUID(), 'Navin', 'Tamang', 'नविन', 'तामाङ', 'प्रा.'),
       ('Dr.', 'MA Sociology', 'username', '2025-07-15', 'username', '2025-07-15', RANDOM_UUID(), 'Anita', 'Shrestha', 'अनिता', 'श्रेष्ठ', 'डा.'),
       ('Dr.', 'BSc CSIT', 'username', '2025-07-16', 'username', '2025-07-16', RANDOM_UUID(), 'Ramesh', 'Karki', 'रमेश', 'कार्की', 'डा.'),
       ('Dr.', 'PhD Biotechnology', 'username', '2025-07-17', 'username', '2025-07-17', RANDOM_UUID(), 'Sita', 'Basnet', 'सीता', 'बस्नेत', 'डा.'),
       ('Dr.', 'M.Ed', 'username', '2025-07-18', 'username', '2025-07-18', RANDOM_UUID(), 'Prakash', 'Rana', 'प्रकाश', 'राणा', 'डा.'),
       ('Dr.', 'MA Economics', 'username', '2025-07-19', 'username', '2025-07-19', RANDOM_UUID(), 'Mina', 'Thapa', 'मिना', 'थापा', 'डा.'),
       ('Prof.', 'MSc IT', 'username', '2025-07-20', 'username', '2025-07-20', RANDOM_UUID(), 'Dipesh', 'K.C', 'दिपेश', 'के.सी', 'प्रा.'),
       ('Prof.', 'PhD Management', 'username', '2025-07-21', 'username', '2025-07-21', RANDOM_UUID(), 'Sarita', 'Dhakal', 'सरिता', 'ढकाल', 'प्रा.'),
       ('Prof.', 'EEC Chief', 'username', '2025-07-22', 'username', '2025-07-22', RANDOM_UUID(), 'Vijay', 'Gurung', 'विजय', 'गुरुङ', 'प्रा.'),
       ('Prof.', 'MSc CSIT', 'username', '2025-07-23', 'username', '2025-07-23', RANDOM_UUID(), 'Rojina', 'Maharjan', 'रोजिना', 'महार्जन', 'प्रा.'),
       ('Dr.', 'PhD Information Systems', 'username', '2025-07-24', 'username', '2025-07-24', RANDOM_UUID(), 'Suman', 'Bista', 'सुमन', 'बिष्ट', 'डा.');


-- Insert Committees
INSERT INTO committees (committee_name,
                        committee_name_nepali,
                        committee_description,
                        committee_created_by,
                        committee_created_date,
                        committee_modified_by,
                        committee_modified_date,
                        committee_uuid,
                        committee_status,
                        committee_max_no_of_meetings,
                        committee_minute_language,
                        committee_coordinator_id)
VALUES ('Academic Committee', 'शैक्षिक समिति', 'Oversee academic policies and curriculum development', 'username', CURRENT_DATE, 'username',
        CURRENT_DATE, RANDOM_UUID(), 'ACTIVE', 10, 'NEPALI', 1),
       ('Events Committee', 'कार्यक्रम समिति', 'Plan and organize all institutional events and seminars', 'username', CURRENT_DATE,
        'username',
        CURRENT_DATE, RANDOM_UUID(), 'ACTIVE', 10, 'ENGLISH', 2),
       ('Research and Development Committee', 'अनुसन्धान तथा विकास समिति', 'Promote research and innovation', 'username', CURRENT_DATE, 'username',
        CURRENT_DATE,
        RANDOM_UUID(), 'ACTIVE', 10, 'NEPALI', 3),
       ('Disciplinary Committee', 'अनुशासन समिति', 'Handle student and staff disciplinary issues', 'username', CURRENT_DATE, 'username',
        CURRENT_DATE,
        RANDOM_UUID(), 'ACTIVE', 10, 'NEPALI', 4),
       ('Student Welfare Committee', 'विद्यार्थी कल्याण समिति', 'Addresse student concerns and well-being', 'username', CURRENT_DATE, 'username',
        CURRENT_DATE,
        RANDOM_UUID(), 'INACTIVE', 10, 'NEPALI', 5),
       ('IT and Infrastructure Committee', 'सूचना प्रविधि तथा पूर्वाधार समिति', 'Manage IT resources and campus infrastructure', 'username', CURRENT_DATE,
        'username',
        CURRENT_DATE, RANDOM_UUID(), 'INACTIVE', 10, 'NEPALI', 6),
       ('इ.अ.स. BE/BArch केन्द्रीकृत भर्ना २०८२ अनुगमन',
        'त्रि.वि. इ.अ.स. अन्तर्गत आंगिक क्याम्पस तथा सम्बन्धन प्राप्त कलेजहरुमा शैक्षिक वर्ष २०८२/०८३ मा संचालन हुने स्नातक (BE/BArch) तहका विभिन्न कार्यक्रमहरुमा केन्द्रीकृत भर्ना अनुगमन',
        NULL,
        'username', CURRENT_DATE, 'username', CURRENT_DATE, RANDOM_UUID(), 'ACTIVE', 1, 'NEPALI', 7);

-- Insert Meetings
INSERT INTO meetings (committee_id, meeting_title,
                      meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
VALUES
    -- meetings for committee 1
    (1, 'Syllabus Update Discussion', '2026-02-22', 'Pulchowk Campus', '14:30:00', 'username', 'username', '2025-07-13',
     '2026-04-13', RANDOM_UUID()),
    (1, 'Annual Seminar Planning', '2026-02-22', 'Auditorium', '11:00:00', 'username', 'username', '2025-07-14',
     '2026-03-14', RANDOM_UUID()),
    (1, 'Research Grant Proposals Review', '2026-03-25', 'Innovation Hub', '13:00:00', 'username', 'username',
     '2026-02-21', '2025-07-21', RANDOM_UUID()),
    (1, 'Review of Recent Incidents', '2026-04-28', 'Admin Office 1', '10:00:00', 'username', 'username', '2025-07-22',
     '2026-01-22', RANDOM_UUID()),

    -- meetings for committee 2
    (2, 'Canteen and Hostel Feedback Session', '2025-07-29', 'Student Lounge', '15:00:00', 'username', 'username',
     '2026-02-23', '2025-07-23', RANDOM_UUID()),
    (2, 'Campus Wi-Fi Upgrade Plan', '2025-08-01', 'IT Department', '11:00:00', 'username', 'username', '2025-07-25',
     '2026-01-25', RANDOM_UUID());

-- Insert Decisions
INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
VALUES
    -- decisions for meeting 1
    (1, 'हालको डाटा स्ट्रक्चर पाठ्यक्रमलाई नयाँ सामग्रीहरूसँग अद्यावधिक गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (1, 'आधुनिक इन्जिनियरिङ प्रवृत्तिहरूमा अतिथि व्याख्यान शृङ्खला आयोजना गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username', '2025-07-04' ),
    (1, 'अन्तिम वर्षको परियोजनाका लागि अन्तर-विभागीय प्रस्ताव स्वीकृत गरिएको छ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (1, 'सेमिनारको शीर्षक "प्रविधिमा नवीनता" रहनेछ।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04' ),

    -- decisions for meeting 2
    (2, 'क्याम्पसभरि Wi-Fi 6 मा स्तरोन्नति गर्ने प्रस्ताव स्वीकृत गरिएको छ।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04'  ),
    (2, 'दुई हप्ताभित्र तीन फरक विक्रेताबाट दरभाउपत्रहरू सङ्कलन गरिनेछ।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04' ),
    (2, 'पुस्तकालयका कम्प्युटरहरू नयाँ SSD र थप RAM सहित स्तरोन्नति गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (2, 'सञ्जाल सुरक्षालाई सुदृढ गर्न नयाँ फायरवाल कार्यान्वयन गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),

    -- decisions for meeting 3
    (3, 'मुख्य वक्ताहरूको छनोट अर्को सातासम्ममा अन्तिम गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (3, 'कार्यक्रमको बजेट रू. ३,००,००० मा अन्तिम गरिएको छ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (3, 'लजिस्टिक व्यवस्थापनका लागि विद्यार्थी स्वयंसेवक समिति गठन गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),

    -- decisions for meeting 4
    (4, 'क्वान्टम कम्प्युटिङ अनुसन्धानका लागि भौतिकशास्त्र विभागको अनुदान प्रस्ताव स्वीकृत गरिएको छ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (4, 'विद्यार्थी परियोजनाका लागि बौद्धिक सम्पत्ति अधिकारसम्बन्धी नयाँ नीति प्रारूप तयार गरिनेछ।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04'  ),
    (4, 'नयाँ 3D प्रिन्टिङ प्रयोगशालाका लागि बजेट सैद्धान्तिक रूपमा स्वीकृत गरिएको छ; अन्तिम दरभाउ आवश्यक।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04' ),
    (4, 'कृत्रिम बुद्धिमत्ता अनुसन्धानमा काठमाडौँ विश्वविद्यालयसँग सहकार्य सुरु गरिनेछ।', RANDOM_UUID(),'username','2025-07-04', 'username','2025-07-04' ),

    -- decisions for meeting 5
    (5,
     'The student involved in fraudulent work will be given a formal warning and will be required to resubmit the work.',
     RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'The examination code of conduct will be updated and sent to all students.', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'A workshop on academic integrity will be made mandatory for all first-year students.', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'The decision regarding the hostel rule violation has been postponed until further evidence is available.',
     RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),

    -- decisions for meeting 6
    (6, 'हालको क्यान्टिन सेवाप्रदायकसँगको सम्झौता नकारात्मक प्रतिक्रियाका आधारमा पुनरावलोकन गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (6, 'छात्रावास ब्लक B मा नयाँ पानी शुद्धीकरण प्रणाली जडान गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (6, 'परीक्षा अवधिमा मानसिक स्वास्थ्य परामर्श सेवा सप्ताहन्तमा पनि विस्तार गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (6, 'नयाँ खेलकुद सुविधाको माग मूल्याङ्कन गर्न सर्वेक्षण गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' );

-- Insert Committee Memberships
INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
VALUES
    -- Committee 1 (8 members)
    (1, 2, 'सदस्य', RANDOM_UUID(), 1),
    (1, 3, 'सदस्य', RANDOM_UUID(), 2),
    (1, 4, 'सदस्य', RANDOM_UUID(), 3),
    (1, 5, 'सदस्य', RANDOM_UUID(), 4),
    (1, 6, 'सदस्य', RANDOM_UUID(), 5),

    -- Committee 2 (5 members)
    (2, 9, 'Member', RANDOM_UUID(), 1),
    (2, 10, 'Member', RANDOM_UUID(), 2),
    (2, 11, 'Member-Secreatary', RANDOM_UUID(), 3),
    (2, 12, 'Member', RANDOM_UUID(), 4),
    (2, 13, 'Member', RANDOM_UUID(), 5),

    -- Committee 3 (3 members)
    (3, 14, 'सदस्य', RANDOM_UUID(), 1),
    (3, 15, 'सदस्य', RANDOM_UUID(), 2),
    (3, 16, 'सदस्य', RANDOM_UUID(), 3),

    -- Committee 4 (1 member)
    (4, 17, 'सदस्य', RANDOM_UUID(), 1),
    (4, 7, 'सदस्य', RANDOM_UUID(), 2),
    (4, 8, 'सदस्य', RANDOM_UUID(), 3);



INSERT INTO meeting_attendees (member_id, meeting_id)
VALUES
    -- Meeting 1 attendees. meeting 1 belongs to committee 1, so only members belonging to committee 1 should be present

    -- Furthermore while populatig the meeting_attendees, make sure that the meeting coordinator is part of the meeting_attendee
    (1, 1),
    (2, 1),
    (3, 1),
    (4, 1),

    -- Meeting 2 attendees
    (3, 2),
    (1, 2),
    (4, 2),
    (5, 2),
    (7, 2),

    -- Meeting 3 attendees
    (5, 3),
    (8, 3),
    (1, 3),
    (2, 3),

    -- Meeting 4 attendees
    (1, 4),
    (2, 4),
    (7, 4),
    (8, 4),

    -- Meeting 5 attendees
    (9, 5),
    (10, 5),
    (11, 5),
    (12, 5),

    -- Meeting 6 attendees
    (10, 6),
    (12, 6),
    (9, 6),
    (11, 6);


INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
VALUES
    --agenda for meeting 1
    (1, 'क्याम्पसभरि Wi-Fi 6 मा स्तरोन्नति गर्ने प्रस्ताव स्वीकृत गरिएको छ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (1, 'दुई हप्ताभित्र तीन फरक विक्रेताबाट दरभाउपत्रहरू सङ्कलन गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (1, 'पुस्तकालयका कम्प्युटरहरू नयाँ SSD र थप RAM सहित स्तरोन्नति गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (1, 'सञ्जाल सुरक्षालाई सुदृढ गर्न नयाँ फायरवाल कार्यान्वयन गरिनेछ।', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),

    --agenda for meeting 5
    (5, 'Discussion and decision on issue regarding the student involved in fraudulent work.', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'Review and update of the examination code of conduct for all students.', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'Making the academic integrity workshop mandatory for all first-year students.', RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' ),
    (5, 'Postponement of the decision on issue concerning hostel rule violation until further evidence is obtained.',
     RANDOM_UUID(), 'username','2025-07-04', 'username','2025-07-04' );


INSERT INTO meeting_invitees(member_id, meeting_id) VALUES
    (15, 1),
    (16, 1);
