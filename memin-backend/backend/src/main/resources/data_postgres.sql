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

-- Seed Members (Coordinators)
INSERT INTO members (member_uuid, member_first_name, member_last_name, member_post, member_title, member_institution, member_created_by, member_created_date, member_modified_by, member_modified_date)
VALUES 
('uuid-jyoti-tandukar', 'Jyoti', 'Tandukar', 'Chairman', 'Dr.', 'DOECE, Pulchowk Campus', 'system', CURRENT_DATE, 'system', CURRENT_DATE),
('uuid-ram-krishna', 'Ram Krishna', 'Maharjan', 'HOD', 'Prof. Dr.', 'DOECE, Pulchowk Campus', 'system', CURRENT_DATE, 'system', CURRENT_DATE)
ON CONFLICT (member_uuid) DO NOTHING;

-- Seed Committees
INSERT INTO committees (committee_coordinator_id, committee_description, committee_uuid, committee_name, committee_created_by, committee_created_date, committee_modified_by, committee_modified_date, committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, 'Primary hub for external engagements, research, and consultancy services.', 'uuid-rtcu-committee', 'Research, Training, and Consultancy Unit (RTCU)', 'system', CURRENT_DATE, 'system', CURRENT_DATE, 'ACTIVE', 'ENGLISH', 50
FROM members WHERE member_uuid = 'uuid-jyoti-tandukar'
ON CONFLICT (committee_uuid) DO UPDATE SET committee_minute_language = 'ENGLISH';

INSERT INTO committees (committee_coordinator_id, committee_description, committee_uuid, committee_name, committee_created_by, committee_created_date, committee_modified_by, committee_modified_date, committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, 'Coordinates the M.Sc. Information and Communication Engineering program.', 'uuid-msice-committee', 'M.Sc. ICE Coordination Committee', 'system', CURRENT_DATE, 'system', CURRENT_DATE, 'ACTIVE', 'ENGLISH', 20
FROM members WHERE member_uuid = 'uuid-ram-krishna'
ON CONFLICT (committee_uuid) DO UPDATE SET committee_minute_language = 'ENGLISH';

INSERT INTO committees (committee_coordinator_id, committee_description, committee_uuid, committee_name, committee_created_by, committee_created_date, committee_modified_by, committee_modified_date, committee_status, committee_minute_language, committee_max_no_of_meetings)
SELECT member_id, 'Oversees the student admission processes for the department.', 'uuid-admission-committee', 'DOECE Admission Committee', 'system', CURRENT_DATE, 'system', CURRENT_DATE, 'ACTIVE', 'ENGLISH', 15
FROM members WHERE member_uuid = 'uuid-jyoti-tandukar'
ON CONFLICT (committee_uuid) DO UPDATE SET committee_minute_language = 'ENGLISH';
