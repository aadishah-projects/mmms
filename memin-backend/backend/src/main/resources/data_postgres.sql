-- ============================================================
-- App Users
-- ============================================================
INSERT INTO app_users (username, password, email, firstname, lastname, role)
VALUES ('admin', '{noop}admin123', 'admin@pcampus.edu.np', 'Admin', 'User', 'DEPARTMENT_HEAD')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_users (username, password, email, firstname, lastname, role)
VALUES ('secretary', '{noop}secretary123', 'secretary@pcampus.edu.np', 'Secretary', 'User', 'SECRETARY')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_users (username, password, email, firstname, lastname, role)
VALUES ('member', '{noop}member123', 'member@pcampus.edu.np', 'Member', 'User', 'DEPARTMENT_MEMBER')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_users (username, password, email, firstname, lastname, role)
VALUES ('guest', '{noop}guest123', 'guest@pcampus.edu.np', 'Guest', 'User', 'GUEST')
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_users (username, password, email, firstname, lastname, role)
VALUES ('username', '{noop}password', 'username@gmail.com', 'Admin', 'Admin', 'DEPARTMENT_MEMBER')
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- Members
-- ============================================================
INSERT INTO members (member_uuid, member_first_name, member_last_name, member_first_name_nepali, member_last_name_nepali,
                     member_post, member_title, member_title_nepali, member_institution,
                     member_created_by, member_created_date, member_modified_by, member_modified_date)
VALUES
('uuid-mem-01', 'Hari', 'Bahadur', 'हरि', 'बहादुर', 'Campus Chief', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-02', 'Gita', 'Oli', 'गिता', 'ओली', 'IOM Delegate', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-03', 'Bikash', 'Lama', 'विकाश', 'लामा', 'IMO Chief', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-04', 'Sunita', 'Maharjan', 'सुनिता', 'महार्जन', 'MSc Environmental Science', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-05', 'Kamal', 'Pandey', 'कमल', 'पाण्डे', 'PhD Civil Engineering', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-06', 'Deepa', 'Gurung', 'दीपा', 'गुरुङ', 'MBBS', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-07', 'Navin', 'Tamang', 'नविन', 'तामाङ', 'MSc Computer Science', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-08', 'Anita', 'Shrestha', 'अनिता', 'श्रेष्ठ', 'MA Sociology', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-09', 'Ramesh', 'Karki', 'रमेश', 'कार्की', 'BSc CSIT', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-10', 'Sita', 'Basnet', 'सीता', 'बस्नेत', 'PhD Biotechnology', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-11', 'Prakash', 'Rana', 'प्रकाश', 'राणा', 'M.Ed', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-12', 'Mina', 'Thapa', 'मिना', 'थापा', 'MA Economics', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-13', 'Dipesh', 'K.C', 'दिपेश', 'के.सी', 'MSc IT', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-14', 'Sarita', 'Dhakal', 'सरिता', 'ढकाल', 'PhD Management', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-15', 'Vijay', 'Gurung', 'विजय', 'गुरुङ', 'EEC Chief', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-16', 'Rojina', 'Maharjan', 'रोजिना', 'महार्जन', 'MSc CSIT', 'Prof.', 'प्रा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE),
('uuid-mem-17', 'Suman', 'Bista', 'सुमन', 'बिष्ट', 'PhD Information Systems', 'Dr.', 'डा.', NULL, 'admin', CURRENT_DATE, 'admin', CURRENT_DATE)
ON CONFLICT (member_uuid) DO NOTHING;

-- ============================================================
-- Committees
-- ============================================================
INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-02'), 'Oversee academic policies and curriculum development',
       'uuid-comm-01', 'Academic Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'ACTIVE', 'NEPALI', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-03'), 'Plan and organize all institutional events and seminars',
       'uuid-comm-02', 'Events Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'ACTIVE', 'ENGLISH', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-04'), 'Promote research and innovation',
       'uuid-comm-03', 'Research and Development Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'ACTIVE', 'NEPALI', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-02'), 'Handle student and staff disciplinary issues',
       'uuid-comm-04', 'Disciplinary Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'ACTIVE', 'NEPALI', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-03'), 'Addresses student concerns and well-being',
       'uuid-comm-05', 'Student Welfare Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'INACTIVE', 'NEPALI', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-04'), 'Manage IT resources and campus infrastructure',
       'uuid-comm-06', 'IT and Infrastructure Committee',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'INACTIVE', 'NEPALI', 10
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

INSERT INTO committees (committee_coordinator_id, committee_secretary_id, committee_description, committee_uuid, committee_name,
                        committee_created_by, committee_created_date, committee_modified_by, committee_modified_date,
                        committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-02'), 'त्रि.वि. इ.अ.स. अन्तर्गत आंगिक क्याम्पस तथा सम्बन्धन प्राप्त कलेजहरुमा शैक्षिक वर्ष २०८२/०८३ मा संचालन हुने स्नातक (BE/BArch) तहका विभिन्न कार्यक्रमहरुमा केन्द्रीकृत भर्ना अनुगमन',
       'uuid-comm-07', 'इ.अ.स. BE/BArch केन्द्रीकृत भर्ना २०८२ अनुगमन',
       'admin', CURRENT_DATE, 'admin', CURRENT_DATE, 'ACTIVE', 'NEPALI', 1
FROM members WHERE member_uuid = 'uuid-mem-01'
ON CONFLICT (committee_uuid) DO NOTHING;

-- ============================================================
-- Committee Memberships
-- (coordinator is member 1 = uuid-mem-01, already implicit as coordinator)
-- ============================================================
INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 1
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-01' AND m.member_uuid = 'uuid-mem-02'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 2
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-01' AND m.member_uuid = 'uuid-mem-03'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 3
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-01' AND m.member_uuid = 'uuid-mem-04'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 4
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-01' AND m.member_uuid = 'uuid-mem-05'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 5
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-01' AND m.member_uuid = 'uuid-mem-06'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'Member', gen_random_uuid()::text, 1
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-02' AND m.member_uuid = 'uuid-mem-09'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'Member', gen_random_uuid()::text, 2
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-02' AND m.member_uuid = 'uuid-mem-10'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'Member-Secretary', gen_random_uuid()::text, 3
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-02' AND m.member_uuid = 'uuid-mem-11'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'Member', gen_random_uuid()::text, 4
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-02' AND m.member_uuid = 'uuid-mem-12'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'Member', gen_random_uuid()::text, 5
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-02' AND m.member_uuid = 'uuid-mem-13'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 1
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-03' AND m.member_uuid = 'uuid-mem-14'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 2
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-03' AND m.member_uuid = 'uuid-mem-15'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 3
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-03' AND m.member_uuid = 'uuid-mem-16'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 1
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-04' AND m.member_uuid = 'uuid-mem-17'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 2
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-04' AND m.member_uuid = 'uuid-mem-07'
ON CONFLICT (committee_id, member_id) DO NOTHING;

INSERT INTO committee_memberships (committee_id, member_id, role, uuid, display_order)
SELECT c.committee_id, m.member_id, 'सदस्य', gen_random_uuid()::text, 3
FROM committees c, members m WHERE c.committee_uuid = 'uuid-comm-04' AND m.member_uuid = 'uuid-mem-08'
ON CONFLICT (committee_id, member_id) DO NOTHING;

-- ============================================================
-- Meetings
-- ============================================================
INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Syllabus Update Discussion', '2026-02-22', 'Pulchowk Campus', '14:30:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-01'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Annual Seminar Planning', '2026-02-22', 'Auditorium', '11:00:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-02'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Research Grant Proposals Review', '2026-03-25', 'Innovation Hub', '13:00:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-03'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Review of Recent Incidents', '2026-04-28', 'Admin Office 1', '10:00:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-04'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Canteen and Hostel Feedback Session', '2025-07-29', 'Student Lounge', '15:00:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-05'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-02'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO meetings (committee_id, meeting_title, meeting_held_date, meeting_held_place, meeting_held_time,
                      created_by, updated_by, created_date, updated_date, uuid)
SELECT c.committee_id, 'Campus Wi-Fi Upgrade Plan', '2025-08-01', 'IT Department', '11:00:00',
       'admin', 'admin', CURRENT_DATE, CURRENT_DATE, 'uuid-meet-06'
FROM committees c WHERE c.committee_uuid = 'uuid-comm-02'
ON CONFLICT (uuid) DO NOTHING;

-- ============================================================
-- Agendas
-- ============================================================
INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'क्याम्पसभरि Wi-Fi 6 मा स्तरोन्नति गर्ने प्रस्ताव स्वीकृत गरिएको छ।',
       'uuid-ag-01', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'दुई हप्ताभित्र तीन फरक विक्रेताबाट दरभाउपत्रहरू सङ्कलन गरिनेछ।',
       'uuid-ag-02', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'पुस्तकालयका कम्प्युटरहरू नयाँ SSD र थप RAM सहित स्तरोन्नति गरिनेछ।',
       'uuid-ag-03', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'सञ्जाल सुरक्षालाई सुदृढ गर्न नयाँ फायरवाल कार्यान्वयन गरिनेछ।',
       'uuid-ag-04', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'Discussion and decision on issue regarding the student involved in fraudulent work.',
       'uuid-ag-05', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'Review and update of the examination code of conduct for all students.',
       'uuid-ag-06', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'Making the academic integrity workshop mandatory for all first-year students.',
       'uuid-ag-07', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05'
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO agendas (meeting_id, agenda, uuid, agenda_created_by, agenda_created_date, agenda_modified_by, agenda_modified_date)
SELECT m.meeting_id, 'Postponement of the decision on hostel rule violation until further evidence is obtained.',
       'uuid-ag-08', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05'
ON CONFLICT (uuid) DO NOTHING;

-- ============================================================
-- Decisions
-- ============================================================
INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'हालको डाटा स्ट्रक्चर पाठ्यक्रमलाई नयाँ सामग्रीहरूसँग अद्यावधिक गरिनेछ।',
       'uuid-dec-01', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'आधुनिक इन्जिनियरिङ प्रवृत्तिहरूमा अतिथि व्याख्यान शृङ्खला आयोजना गरिनेछ।',
       'uuid-dec-02', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'अन्तिम वर्षको परियोजनाका लागि अन्तर-विभागीय प्रस्ताव स्वीकृत गरिएको छ।',
       'uuid-dec-03', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-01' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'सेमिनारको शीर्षक "प्रविधिमा नवीनता" रहनेछ।',
       'uuid-dec-04', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-02' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'क्याम्पसभरि Wi-Fi 6 मा स्तरोन्नति गर्ने प्रस्ताव स्वीकृत गरिएको छ।',
       'uuid-dec-05', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-02' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'दुई हप्ताभित्र तीन फरक विक्रेताबाट दरभाउपत्रहरू सङ्कलन गरिनेछ।',
       'uuid-dec-06', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-02' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'पुस्तकालयका कम्प्युटरहरू नयाँ SSD र थप RAM सहित स्तरोन्नति गरिनेछ।',
       'uuid-dec-07', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-02' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'सञ्जाल सुरक्षालाई सुदृढ गर्न नयाँ फायरवाल कार्यान्वयन गरिनेछ।',
       'uuid-dec-08', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-02' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'मुख्य वक्ताहरूको छनोट अर्को सातासम्ममा अन्तिम गरिनेछ।',
       'uuid-dec-09', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-03' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'कार्यक्रमको बजेट रू. ३,००,००० मा अन्तिम गरिएको छ।',
       'uuid-dec-10', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-03' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'लजिस्टिक व्यवस्थापनका लागि विद्यार्थी स्वयंसेवक समिति गठन गरिनेछ।',
       'uuid-dec-11', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-03' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'क्वान्टम कम्प्युटिङ अनुसन्धानका लागि भौतिकशास्त्र विभागको अनुदान प्रस्ताव स्वीकृत गरिएको छ।',
       'uuid-dec-12', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-04' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'विद्यार्थी परियोजनाका लागि बौद्धिक सम्पत्ति अधिकारसम्बन्धी नयाँ नीति प्रारूप तयार गरिनेछ।',
       'uuid-dec-13', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-04' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'नयाँ 3D प्रिन्टिङ प्रयोगशालाका लागि बजेट सैद्धान्तिक रूपमा स्वीकृत गरिएको छ; अन्तिम दरभाउ आवश्यक।',
       'uuid-dec-14', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-04' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'कृत्रिम बुद्धिमत्ता अनुसन्धानमा काठमाडौँ विश्वविद्यालयसँग सहकार्य सुरु गरिनेछ।',
       'uuid-dec-15', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-04' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'The student involved in fraudulent work will be given a formal warning and will be required to resubmit the work.',
       'uuid-dec-16', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'The examination code of conduct will be updated and sent to all students.',
       'uuid-dec-17', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'A workshop on academic integrity will be made mandatory for all first-year students.',
       'uuid-dec-18', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'The decision regarding the hostel rule violation has been postponed until further evidence is available.',
       'uuid-dec-19', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-05' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'हालको क्यान्टिन सेवाप्रदायकसँगको सम्झौता नकारात्मक प्रतिक्रियाका आधारमा पुनरावलोकन गरिनेछ।',
       'uuid-dec-20', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-06' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'छात्रावास ब्लक B मा नयाँ पानी शुद्धीकरण प्रणाली जडान गरिनेछ।',
       'uuid-dec-21', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-06' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'परीक्षा अवधिमा मानसिक स्वास्थ्य परामर्श सेवा सप्ताहन्तमा पनि विस्तार गरिनेछ।',
       'uuid-dec-22', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-06' ON CONFLICT (uuid) DO NOTHING;

INSERT INTO decisions (meeting_id, decision, uuid, decision_created_by, decision_created_date, decision_modified_by, decision_modified_date)
SELECT m.meeting_id, 'नयाँ खेलकुद सुविधाको माग मूल्याङ्कन गर्न सर्वेक्षण गरिनेछ।',
       'uuid-dec-23', 'admin', CURRENT_DATE, 'admin', CURRENT_DATE
FROM meetings m WHERE m.uuid = 'uuid-meet-06' ON CONFLICT (uuid) DO NOTHING;

-- ============================================================
-- Meeting Attendees
-- ============================================================
INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-01' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-02' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-03' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-04' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-01' AND mt.uuid = 'uuid-meet-02' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-03' AND mt.uuid = 'uuid-meet-02' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-04' AND mt.uuid = 'uuid-meet-02' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-05' AND mt.uuid = 'uuid-meet-02' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-07' AND mt.uuid = 'uuid-meet-02' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-01' AND mt.uuid = 'uuid-meet-03' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-02' AND mt.uuid = 'uuid-meet-03' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-05' AND mt.uuid = 'uuid-meet-03' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-08' AND mt.uuid = 'uuid-meet-03' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-01' AND mt.uuid = 'uuid-meet-04' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-02' AND mt.uuid = 'uuid-meet-04' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-07' AND mt.uuid = 'uuid-meet-04' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-08' AND mt.uuid = 'uuid-meet-04' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-09' AND mt.uuid = 'uuid-meet-05' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-10' AND mt.uuid = 'uuid-meet-05' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-11' AND mt.uuid = 'uuid-meet-05' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-12' AND mt.uuid = 'uuid-meet-05' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-09' AND mt.uuid = 'uuid-meet-06' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-10' AND mt.uuid = 'uuid-meet-06' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-11' AND mt.uuid = 'uuid-meet-06' ON CONFLICT DO NOTHING;

INSERT INTO meeting_attendees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-12' AND mt.uuid = 'uuid-meet-06' ON CONFLICT DO NOTHING;

-- ============================================================
-- Meeting Invitees
-- ============================================================
INSERT INTO meeting_invitees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-15' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

INSERT INTO meeting_invitees (member_id, meeting_id)
SELECT m.member_id, mt.meeting_id FROM members m, meetings mt
WHERE m.member_uuid = 'uuid-mem-16' AND mt.uuid = 'uuid-meet-01' ON CONFLICT DO NOTHING;

-- ============================================================
-- Update Users with Linked Member IDs
-- ============================================================
UPDATE app_users SET linked_member_id = (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-02') WHERE username = 'secretary';
UPDATE app_users SET linked_member_id = (SELECT member_id FROM members WHERE member_uuid = 'uuid-mem-03') WHERE username = 'username';
