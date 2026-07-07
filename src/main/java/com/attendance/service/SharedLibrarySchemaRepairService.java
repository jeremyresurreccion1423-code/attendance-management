package com.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keeps {@code library.student_profiles.user_id} aligned with shared {@code public.users}
 * (not legacy {@code library.users}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedLibrarySchemaRepairService {

    private final JdbcTemplate jdbcTemplate;

    public void repairStudentProfileUserForeignKey() {
        try {
            jdbcTemplate.update("""
                    UPDATE library.student_profiles sp
                    SET user_id = s.user_id
                    FROM public.students s
                    WHERE sp.user_id NOT IN (SELECT id FROM public.users)
                      AND LOWER(s.student_number) = LOWER(sp.student_id)
                      AND s.user_id IS NOT NULL
                    """);

            jdbcTemplate.update("""
                    UPDATE library.student_profiles sp
                    SET user_id = pu.id
                    FROM public.users pu
                    WHERE sp.user_id NOT IN (SELECT id FROM public.users)
                      AND LOWER(TRIM(pu.email)) IN (
                          SELECT LOWER(TRIM(s.email))
                          FROM public.students s
                          WHERE LOWER(s.student_number) = LOWER(sp.student_id)
                            AND s.email IS NOT NULL
                            AND TRIM(s.email) <> ''
                      )
                    """);

            int removed = jdbcTemplate.update("""
                    DELETE FROM library.student_profiles
                    WHERE user_id NOT IN (SELECT id FROM public.users)
                    """);
            if (removed > 0) {
                log.warn("Removed {} orphan library.student_profiles row(s) with invalid user_id", removed);
            }

            List<String> constraints = jdbcTemplate.queryForList("""
                    SELECT c.conname
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = t.relnamespace
                    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
                    WHERE n.nspname = 'library'
                      AND t.relname = 'student_profiles'
                      AND c.contype = 'f'
                      AND a.attname = 'user_id'
                    """, String.class);

            for (String constraintName : constraints) {
                jdbcTemplate.execute("ALTER TABLE library.student_profiles DROP CONSTRAINT IF EXISTS \""
                        + constraintName.replace("\"", "") + "\"");
            }

            jdbcTemplate.execute("""
                    ALTER TABLE library.student_profiles
                    ADD CONSTRAINT fk_student_profiles_user
                    FOREIGN KEY (user_id) REFERENCES public.users (id)
                    """);
            log.info("Repaired library.student_profiles.user_id foreign key -> public.users");
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("already exists")) {
                log.debug("library.student_profiles FK already points to public.users");
            } else {
                log.warn("Could not repair library.student_profiles FK: {}", message);
            }
        }
    }
}
