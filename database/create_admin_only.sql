-- Simple script to create admin user only
-- Delete dependent records first
DELETE FROM teachers WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');
DELETE FROM students WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');

-- Then delete the admin user
DELETE FROM users WHERE username = 'admin';

-- Insert new admin user
INSERT INTO users (username, password, role, last_login, enabled, created_at)
VALUES ('admin', 'admin123', 'ADMIN', NULL, true, NOW());

-- Verify admin was created
SELECT * FROM users WHERE username = 'admin';
