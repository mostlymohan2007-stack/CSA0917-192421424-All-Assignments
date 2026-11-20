-- ============================================================
-- Task 6 - Campus Event Registration: MySQL schema
-- Run:  mysql -u <user> -p < schema.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_events;

DROP TABLE IF EXISTS registration;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS event;

CREATE TABLE student (
    student_id VARCHAR(10) NOT NULL,
    name       VARCHAR(60) NOT NULL,
    email      VARCHAR(80) NOT NULL,
    department VARCHAR(30) NOT NULL,
    PRIMARY KEY (student_id)
);

CREATE TABLE event (
    event_id   INT          NOT NULL AUTO_INCREMENT,
    title      VARCHAR(80)  NOT NULL,
    venue      VARCHAR(60)  NOT NULL,
    event_date DATE         NOT NULL,
    capacity   INT          NOT NULL,
    PRIMARY KEY (event_id),
    UNIQUE KEY uq_event_title (title),
    CONSTRAINT chk_capacity CHECK (capacity > 0)
);

CREATE TABLE registration (
    reg_id        BIGINT      NOT NULL AUTO_INCREMENT,
    student_id    VARCHAR(10) NOT NULL,
    event_id      INT         NOT NULL,
    registered_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reg_id),
    -- RULE: a student cannot register twice for the same event (enforced by the database itself)
    UNIQUE KEY uq_student_event (student_id, event_id),
    KEY idx_event_reg (event_id, reg_id),
    CONSTRAINT fk_reg_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_reg_event   FOREIGN KEY (event_id)   REFERENCES event (event_id)
);

-- Sample events. Events 1-10 have a large capacity; event 11 is tiny so that the "event full" rule can be tested.
INSERT INTO event (title, venue, event_date, capacity) VALUES
    ('Tech Fest Inauguration',   'Main Auditorium', '2026-12-05', 20000),
    ('Hackathon 2026',           'CSE Lab Block',   '2026-12-06', 20000),
    ('Robotics Workshop',        'Mechanical Hall', '2026-12-07', 20000),
    ('Cultural Night',           'Open Air Theatre','2026-12-08', 20000),
    ('Career Guidance Seminar',  'Seminar Hall 1',  '2026-12-09', 20000),
    ('Inter-College Quiz',       'Seminar Hall 2',  '2026-12-10', 20000),
    ('Photography Walk',         'Campus Gate',     '2026-12-11', 20000),
    ('Blood Donation Camp',      'Health Centre',   '2026-12-12', 20000),
    ('Sports Day',               'Main Ground',     '2026-12-13', 20000),
    ('Alumni Meet',              'Convention Hall', '2026-12-14', 20000),
    ('Small Workshop (3 seats)', 'Room 101',        '2026-12-15', 3);
