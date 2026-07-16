-- Create dedicated admin for the Attendance admin dashboard.
-- Plaintext password is auto-rehashed to BCrypt on app startup.

INSERT INTO public.users (username, password, role, email, full_name, enabled, created_at)
SELECT 'dashadmin', 'DashAdmin@2026', 'ADMIN', 'edulibrary67+dashadmin@gmail.com', 'Dashboard Admin', true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.users WHERE username = 'dashadmin'
);

SELECT id, username, role, email, full_name, enabled, created_at
FROM public.users
WHERE username = 'dashadmin';
