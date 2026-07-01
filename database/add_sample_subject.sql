-- Add sample subject
-- First, get the teacher ID
SELECT id, full_name FROM teachers LIMIT 1;

-- Get section ID
SELECT id, name FROM sections LIMIT 1;

-- Insert sample subject (replace 1 with actual teacher_id and section_id from above)
DELETE FROM subjects WHERE subject_code = 'CS301';
INSERT INTO subjects (subject_code, subject_name, description, teacher_id, section_id, created_at)
VALUES ('CS301', 'Database Systems', 'Introduction to database design and SQL', 1, 1, NOW());

-- Verify subject was created
SELECT * FROM subjects;
