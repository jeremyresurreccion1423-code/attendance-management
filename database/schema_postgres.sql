-- Attendance Management System - PostgreSQL Schema for Supabase
-- Run this in Supabase SQL Editor to create tables

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT')),
    last_login TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sections table
CREATE TABLE IF NOT EXISTS sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    course VARCHAR(100),
    year_level VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
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

-- Teachers table
CREATE TABLE IF NOT EXISTS teachers (
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

-- Subjects table
CREATE TABLE IF NOT EXISTS subjects (
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

-- Enrollments table
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, subject_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Attendance table
CREATE TABLE IF NOT EXISTS attendance (
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

-- Attendance QR codes
CREATE TABLE IF NOT EXISTS attendance_qr (
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
CREATE TABLE IF NOT EXISTS face_recognition_log (
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

-- Marks table
CREATE TABLE IF NOT EXISTS marks (
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

-- Timetables table
CREATE TABLE IF NOT EXISTS timetables (
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

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    read_flag BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    generated_by BIGINT,
    file_path VARCHAR(255),
    parameters TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
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
