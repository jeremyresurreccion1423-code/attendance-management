-- Allow the ARCHIVED status value for students (used by the Archive/Unarchive buttons).
ALTER TABLE students DROP CONSTRAINT IF EXISTS students_status_check;
ALTER TABLE students ADD CONSTRAINT students_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED', 'ARCHIVED'));
