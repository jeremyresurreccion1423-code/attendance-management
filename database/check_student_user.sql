-- Check student and user relationship
SELECT s.id, s.student_number, s.full_name, s.user_id, u.username, u.id as user_real_id
FROM students s
LEFT JOIN users u ON s.user_id = u.id;
