-- Clean users table to reset data
DELETE FROM users;
DELETE FROM student;
DELETE FROM teacher;
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE student_id_seq RESTART WITH 1;
ALTER SEQUENCE teacher_id_seq RESTART WITH 1;
