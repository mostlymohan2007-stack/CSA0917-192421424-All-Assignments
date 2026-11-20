-- ============================================================
-- Task 6 - OPTIONAL load-test data: 10,000 students x 10 events = 100,000 registrations
-- Run after schema.sql:  mysql -u <user> -p campus_events < seed_100000.sql
-- ============================================================
USE campus_events;
SET SESSION cte_max_recursion_depth = 20000;

INSERT INTO student (student_id, name, email, department)
WITH RECURSIVE n (i) AS (
    SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 10000
)
SELECT CONCAT('S', LPAD(i, 6, '0')),
       CONCAT('Student ', i),
       CONCAT('student', i, '@campus.edu'),
       ELT(1 + (i MOD 4), 'CSE', 'ECE', 'MECH', 'CIVIL')
FROM n;

INSERT INTO registration (student_id, event_id)
SELECT s.student_id, e.event_id
FROM student s
JOIN event e ON e.event_id <= 10
ORDER BY e.event_id, s.student_id;

SELECT COUNT(*) AS registrations FROM registration;
