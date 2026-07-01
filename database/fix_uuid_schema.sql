-- Fix Supabase schema: UUID columns -> BIGINT (matches Java entities)
-- Run in Supabase SQL Editor if admin pages show 500 errors.
-- Preserves existing users (login accounts).

DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS face_recognition_log CASCADE;
DROP TABLE IF EXISTS attendance_qr CASCADE;
DROP TABLE IF EXISTS attendance CASCADE;
DROP TABLE IF EXISTS marks CASCADE;
DROP TABLE IF EXISTS timetables CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS subjects CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS teachers CASCADE;
DROP TABLE IF EXISTS sections CASCADE;

-- Sections
CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    course VARCHAR(100),
    year_level VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    student_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    course VARCHAR(100),
    year_level VARCHAR(20),
    section_id BIGINT,
    contact_number VARCHAR(20),
    email VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL
);

-- Teachers
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    contact_number VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Subjects
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    subject_code VARCHAR(20) NOT NULL UNIQUE,
    subject_name VARCHAR(150) NOT NULL,
    teacher_id BIGINT,
    section_id BIGINT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL
);

-- Enrollments
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Attendance
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    time_in TIME,
    time_out TIME,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    method VARCHAR(30) DEFAULT 'MANUAL' CHECK (method IN ('MANUAL', 'QR', 'FACE_RECOGNITION')),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    remarks VARCHAR(255),
    recorded_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Attendance QR
CREATE TABLE attendance_qr (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    qr_code VARCHAR(255) NOT NULL UNIQUE,
    session_date DATE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Face recognition logs
CREATE TABLE face_recognition_log (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT,
    result VARCHAR(20) NOT NULL CHECK (result IN ('MATCH', 'NO_MATCH', 'PENDING')),
    confidence_score DOUBLE PRECISION,
    image_path VARCHAR(255),
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL
);

-- Marks
CREATE TABLE marks (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    quiz_score DOUBLE PRECISION DEFAULT 0,
    exam_score DOUBLE PRECISION DEFAULT 0,
    assignment_score DOUBLE PRECISION DEFAULT 0,
    final_grade DOUBLE PRECISION,
    remarks VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Timetables
CREATE TABLE timetables (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50),
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
);

-- Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    read_flag BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Reports
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    generated_by BIGINT,
    file_path VARCHAR(255),
    parameters TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Audit logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Sample data (uses existing user accounts)
INSERT INTO sections (name, course, year_level) VALUES
    ('BSIT-3A', 'BS Information Technology', '3rd Year'),
    ('BSCS-2B', 'BS Computer Science', '2nd Year');

INSERT INTO teachers (employee_id, full_name, department, contact_number, email, user_id)
SELECT 'EMP001', 'Dr. Maria Santos', 'Computer Science', '09171234567', 'maria.santos@school.edu', id
FROM users WHERE username = 'teacher1'
ON CONFLICT DO NOTHING;

INSERT INTO students (student_number, full_name, course, year_level, section_id, contact_number, email, status, user_id)
SELECT '2021-0001', 'Juan Dela Cruz', 'BS Information Technology', '3rd Year', s.id, '09181234567', 'juan.delacruz@student.edu', 'ACTIVE', u.id
FROM sections s, users u
WHERE s.name = 'BSIT-3A' AND u.username = 'student1'
ON CONFLICT DO NOTHING;

INSERT INTO subjects (subject_code, subject_name, teacher_id, section_id)
SELECT 'CS101', 'Introduction to Programming', t.id, s.id
FROM teachers t, sections s
WHERE t.employee_id = 'EMP001' AND s.name = 'BSIT-3A'
ON CONFLICT DO NOTHING;

INSERT INTO enrollments (student_id, subject_id)
SELECT st.id, sub.id
FROM students st, subjects sub
WHERE st.student_number = '2021-0001' AND sub.subject_code = 'CS101'
ON CONFLICT DO NOTHING;
