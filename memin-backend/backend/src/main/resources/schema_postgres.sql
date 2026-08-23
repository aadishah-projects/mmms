CREATE TABLE IF NOT EXISTS app_users
(
    uid       SERIAL PRIMARY KEY,
    firstname VARCHAR(50),
    lastname  VARCHAR(50),
    username  VARCHAR(50) UNIQUE,
    email     VARCHAR(100),
    password VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS members (
    member_id SERIAL PRIMARY KEY,
    member_uuid VARCHAR(36) NOT NULL UNIQUE,

    member_first_name VARCHAR(255) NOT NULL,
    member_last_name VARCHAR(255) NOT NULL,
    member_first_name_nepali VARCHAR(255),
    member_last_name_nepali VARCHAR(255),

    member_post VARCHAR(255),
    member_title VARCHAR(255) NOT NULL,
    member_title_nepali VARCHAR(255),
    member_institution VARCHAR(255),
    member_email VARCHAR(255),

    member_created_by VARCHAR(255) NOT NULL,
    member_created_date DATE NOT NULL,
    member_modified_by VARCHAR(255) NOT NULL,
    member_modified_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS committees (
    committee_id SERIAL PRIMARY KEY,
    committee_coordinator_id INT NOT NULL,
    committee_secretary_id INT,
    committee_description TEXT,
    committee_uuid VARCHAR(36) NOT NULL UNIQUE,
    committee_name VARCHAR(255) NOT NULL,
    committee_created_by VARCHAR(255) NOT NULL,
    committee_created_date DATE NOT NULL,
    committee_modified_by VARCHAR(255) NOT NULL,
    committee_modified_date DATE NOT NULL,
    committee_status VARCHAR(255) NOT NULL,
    committee_minute_language VARCHAR(255) NOT NULL,
    committee_minute_opening_template TEXT,
    committee_minute_header_template TEXT,
    committee_minute_template_html TEXT,
    active_minute_template_id INT,
    committee_max_no_of_meetings INT,
    FOREIGN KEY (committee_coordinator_id) REFERENCES members(member_id),
    FOREIGN KEY (committee_secretary_id) REFERENCES members(member_id)
);

CREATE TABLE IF NOT EXISTS committee_memberships (
    committee_id INT NOT NULL,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    member_id INT NOT NULL,
    role VARCHAR(255) NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (committee_id, member_id),
    FOREIGN KEY (committee_id) REFERENCES committees(committee_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

CREATE TABLE IF NOT EXISTS meetings (
    meeting_id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,

    meeting_title VARCHAR(255) NOT NULL,
    meeting_held_date DATE NOT NULL,
    meeting_held_time TIME NOT NULL,
    meeting_held_place VARCHAR(255) NOT NULL,
    meeting_minute_content_html TEXT,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_date DATE NOT NULL,
    updated_date DATE NOT NULL,

    committee_id INT NOT NULL,
    meeting_chairman_id INT,
    FOREIGN KEY (committee_id) REFERENCES committees(committee_id),
    FOREIGN KEY (meeting_chairman_id) REFERENCES members(member_id)
);

CREATE TABLE IF NOT EXISTS meeting_attendees (
    member_id INT NOT NULL,
    meeting_id INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,

    PRIMARY KEY (member_id, meeting_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id)
);

CREATE TABLE IF NOT EXISTS meeting_invitees (
    member_id INT NOT NULL,
    meeting_id INT NOT NULL,

    PRIMARY KEY (member_id, meeting_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id)
);

CREATE TABLE IF NOT EXISTS decisions (
    decision_id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,

    meeting_id INT NOT NULL,
    decision TEXT,

    decision_created_by VARCHAR(255) NOT NULL,
    decision_created_date DATE NOT NULL,
    decision_modified_by VARCHAR(255) NOT NULL,
    decision_modified_date DATE NOT NULL,

    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id)
);

CREATE TABLE IF NOT EXISTS agendas (
    agenda_id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,

    meeting_id INT NOT NULL,
    agenda TEXT,

    agenda_created_by VARCHAR(255) NOT NULL,
    agenda_created_date DATE NOT NULL,
    agenda_modified_by VARCHAR(255) NOT NULL,
    agenda_modified_date DATE NOT NULL,

    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id)
);

CREATE TABLE IF NOT EXISTS invite_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    invited_by VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    committee_id INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (committee_id) REFERENCES committees(committee_id)
);

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'DEPARTMENT_MEMBER';
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS linked_member_id INT;
ALTER TABLE members ADD COLUMN IF NOT EXISTS member_email VARCHAR(255);
ALTER TABLE committees ADD COLUMN IF NOT EXISTS committee_secretary_id INT REFERENCES members(member_id);

CREATE TABLE IF NOT EXISTS system_settings (
    id INT PRIMARY KEY,
    ai_provider_type VARCHAR(50),
    ai_base_url VARCHAR(500),
    ai_api_key VARCHAR(500),
    ai_model VARCHAR(100),
    ai_max_tokens INT,
    mail_host VARCHAR(255),
    mail_port INT,
    mail_username VARCHAR(255),
    mail_password VARCHAR(255),
    mail_auth BOOLEAN,
    mail_starttls BOOLEAN,
    mail_from VARCHAR(255),
    frontend_url VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS minute_templates (
    template_id SERIAL PRIMARY KEY,
    committee_id INT NOT NULL,
    template_name VARCHAR(160) NOT NULL,
    template_html TEXT NOT NULL,
    template_language VARCHAR(30),
    created_by VARCHAR(255),
    UNIQUE (committee_id, template_name),
    FOREIGN KEY (committee_id) REFERENCES committees(committee_id)
);
