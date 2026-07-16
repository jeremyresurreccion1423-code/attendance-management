package com.attendance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationV2Initializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    description VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS department_id BIGINT");
        jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'");
        jdbcTemplate.execute("ALTER TABLE sections ADD COLUMN IF NOT EXISTS department_id BIGINT");
        jdbcTemplate.execute("ALTER TABLE students ADD COLUMN IF NOT EXISTS department_id BIGINT");
        jdbcTemplate.execute("ALTER TABLE subjects ADD COLUMN IF NOT EXISTS department_id BIGINT");

        seedDepartmentsFromLegacyData();
        backfillDepartmentIds();
        addForeignKeysIfMissing();
        normalizeAttendanceMethodConstraint();
        addAttendanceQrTimetableColumn();
        ensureFaceRecognitionLogTable();
        releaseEmailsOnDeletedLoginAccounts();

        log.info("Schema migration v2 completed");
    }

    private void ensureFaceRecognitionLogTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS face_recognition_log (
                    id BIGSERIAL PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    subject_id BIGINT,
                    result VARCHAR(20) NOT NULL,
                    confidence_score DOUBLE PRECISION,
                    image_path VARCHAR(255),
                    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        try {
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE face_recognition_log
                            ADD CONSTRAINT fk_face_recognition_log_student
                            FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE face_recognition_log
                            ADD CONSTRAINT fk_face_recognition_log_subject
                            FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
        } catch (Exception ex) {
            log.warn("face_recognition_log FK may already exist: {}", ex.getMessage());
        }
    }

    /**
     * Older deletes only renamed the username and left the email on disabled users,
     * which blocked re-adding students/teachers with the same email.
     */
    private void releaseEmailsOnDeletedLoginAccounts() {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET email = NULL
                WHERE enabled = FALSE
                  AND username LIKE '%\\_deleted\\_%' ESCAPE '\\'
                  AND email IS NOT NULL
                  AND TRIM(email) <> ''
                  AND NOT EXISTS (SELECT 1 FROM students s WHERE s.user_id = users.id)
                  AND NOT EXISTS (SELECT 1 FROM teachers t WHERE t.user_id = users.id)
                """);
        if (updated > 0) {
            log.info("Released email on {} previously deleted login account(s)", updated);
        }
    }

    private void seedDepartmentsFromLegacyData() {
        jdbcTemplate.execute("""
                INSERT INTO departments (name)
                SELECT DISTINCT TRIM(src.name) FROM (
                    SELECT department AS name FROM teachers WHERE department IS NOT NULL AND TRIM(department) <> ''
                    UNION
                    SELECT course AS name FROM sections WHERE course IS NOT NULL AND TRIM(course) <> ''
                    UNION
                    SELECT course AS name FROM students WHERE course IS NOT NULL AND TRIM(course) <> ''
                ) src
                WHERE TRIM(src.name) <> ''
                ON CONFLICT (name) DO NOTHING
                """);
    }

    private void backfillDepartmentIds() {
        jdbcTemplate.update("""
                UPDATE teachers t
                SET department_id = d.id
                FROM departments d
                WHERE t.department_id IS NULL
                  AND t.department IS NOT NULL
                  AND LOWER(TRIM(t.department)) = LOWER(TRIM(d.name))
                """);

        jdbcTemplate.update("""
                UPDATE sections s
                SET department_id = d.id
                FROM departments d
                WHERE s.department_id IS NULL
                  AND s.course IS NOT NULL
                  AND LOWER(TRIM(s.course)) = LOWER(TRIM(d.name))
                """);

        jdbcTemplate.update("""
                UPDATE students st
                SET department_id = d.id
                FROM departments d
                WHERE st.department_id IS NULL
                  AND st.course IS NOT NULL
                  AND LOWER(TRIM(st.course)) = LOWER(TRIM(d.name))
                """);

        jdbcTemplate.update("""
                UPDATE subjects sub
                SET department_id = t.department_id
                FROM teachers t
                WHERE sub.department_id IS NULL
                  AND sub.teacher_id = t.id
                  AND t.department_id IS NOT NULL
                """);

        jdbcTemplate.update("""
                UPDATE subjects sub
                SET department_id = s.department_id
                FROM sections s
                WHERE sub.department_id IS NULL
                  AND sub.section_id = s.id
                  AND s.department_id IS NOT NULL
                """);

        // Teachers with null department but assigned subjects inherit that subject department.
        jdbcTemplate.update("""
                UPDATE teachers t
                SET department_id = sub.department_id
                FROM subjects sub
                WHERE t.department_id IS NULL
                  AND sub.teacher_id = t.id
                  AND sub.department_id IS NOT NULL
                """);

        jdbcTemplate.update("""
                UPDATE teachers
                SET status = 'ACTIVE'
                WHERE status IS NULL
                """);

        jdbcTemplate.execute("""
                INSERT INTO departments (name) VALUES ('General')
                ON CONFLICT (name) DO NOTHING
                """);

        jdbcTemplate.update("""
                UPDATE teachers
                SET department_id = (SELECT id FROM departments WHERE name = 'General' LIMIT 1)
                WHERE department_id IS NULL
                """);

        jdbcTemplate.update("""
                UPDATE sections
                SET department_id = (SELECT id FROM departments WHERE name = 'General' LIMIT 1)
                WHERE department_id IS NULL
                """);

        jdbcTemplate.update("""
                UPDATE students
                SET department_id = (SELECT id FROM departments WHERE name = 'General' LIMIT 1)
                WHERE department_id IS NULL
                """);
    }

    private void addForeignKeysIfMissing() {
        try {
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE teachers
                            ADD CONSTRAINT fk_teachers_department
                            FOREIGN KEY (department_id) REFERENCES departments(id);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE sections
                            ADD CONSTRAINT fk_sections_department
                            FOREIGN KEY (department_id) REFERENCES departments(id);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE students
                            ADD CONSTRAINT fk_students_department
                            FOREIGN KEY (department_id) REFERENCES departments(id);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE subjects
                            ADD CONSTRAINT fk_subjects_department
                            FOREIGN KEY (department_id) REFERENCES departments(id);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
        } catch (Exception ex) {
            log.warn("FK constraints may already exist or legacy rows block constraints: {}", ex.getMessage());
        }
    }

    private void addAttendanceQrTimetableColumn() {
        jdbcTemplate.execute("ALTER TABLE attendance_qr ADD COLUMN IF NOT EXISTS timetable_id BIGINT");
        try {
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                        ALTER TABLE attendance_qr
                            ADD CONSTRAINT fk_attendance_qr_timetable
                            FOREIGN KEY (timetable_id) REFERENCES timetables(id);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$
                    """);
        } catch (Exception ex) {
            log.warn("attendance_qr timetable FK may already exist: {}", ex.getMessage());
        }
    }

    private void normalizeAttendanceMethodConstraint() {
        jdbcTemplate.update("""
                UPDATE attendance
                SET method = 'MANUAL'
                WHERE method IS NULL
                   OR method NOT IN ('MANUAL', 'QR', 'AUTO')
                """);

        jdbcTemplate.execute("""
                DO $$ BEGIN
                    ALTER TABLE attendance DROP CONSTRAINT IF EXISTS attendance_method_check;
                    ALTER TABLE attendance
                        ADD CONSTRAINT attendance_method_check
                        CHECK (method IN ('MANUAL', 'QR', 'AUTO'));
                END $$;
                """);
    }
}
