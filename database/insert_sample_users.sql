-- Simple script to insert sample data
-- Run each section separately in Supabase SQL Editor

-- Step 1: Insert users
DELETE FROM users WHERE username IN ('admin', 'teacher1', 'student1');

INSERT INTO users (username, password, role, last_login, enabled, created_at)
VALUES ('admin', 'admin123', 'ADMIN', NULL, true, NOW());

INSERT INTO users (username, password, role, last_login, enabled, created_at)
VALUES ('teacher1', 'teacher123', 'TEACHER', NULL, true, NOW());

INSERT INTO users (username, password, role, last_login, enabled, created_at)
VALUES ('student1', 'student123', 'STUDENT', NULL, true, NOW());

-- Step 2: Insert sections
DELETE FROM sections WHERE name IN ('BSIT-3A', 'BSCS-2B');

INSERT INTO sections (name, course, year_level, created_at)
VALUES ('BSIT-3A', 'BS Information Technology', '3rd Year', NOW());

INSERT INTO sections (name, course, year_level, created_at)
VALUES ('BSCS-2B', 'BS Computer Science', '2nd Year', NOW());

-- Step 3: Get user IDs and insert teacher
-- First, check the user IDs
SELECT id, username FROM users WHERE username = 'teacher1';

-- Then insert teacher (replace 1 with actual teacher user_id from above)
DELETE FROM teachers WHERE employee_id = 'EMP001';
INSERT INTO teachers (employee_id, full_name, department, contact_number, email, user_id, created_at)
VALUES ('EMP001', 'Dr. Maria Santos', 'Computer Science', '09171234567', 'maria.santos@school.edu', 1, NOW());

-- Step 4: Get section ID and insert student
-- First, check the section ID
SELECT id, name FROM sections WHERE name = 'BSIT-3A';

-- Get student user ID
SELECT id, username FROM users WHERE username = 'student1';

-- Then insert student (replace 1 with actual section_id and 3 with actual student user_id)
DELETE FROM students WHERE student_number = '2021-0001';
INSERT INTO students (student_number, full_name, course, year_level, section_id, contact_number, email, status, user_id, created_at)
VALUES ('2021-0001', 'Juan Dela Cruz', 'BS Information Technology', '3rd Year', 1, '09181234567', 'juan.delacruz@student.edu', 'ACTIVE', 3, NOW());

-- Verify all data
SELECT * FROM users;
SELECT * FROM sections;
SELECT * FROM teachers;
SELECT * FROM students;
