-- Check teacher and user relationship
SELECT t.id, t.employee_id, t.full_name, t.user_id, u.username, u.id as user_real_id
FROM teachers t
LEFT JOIN users u ON t.user_id = u.id;
