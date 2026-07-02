package com.attendance.service;

import com.attendance.model.Student;
import com.attendance.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Creates {@code library.student_profiles} when a student is registered in Attendance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedLibraryStudentProfileSyncService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncFromAttendanceStudent(User user, Student student) {
        if (user == null || user.getId() == null || student == null) {
            return;
        }

        if (findLibraryProfileId(user.getId(), student.getStudentNumber()).isPresent()) {
            return;
        }

        String studentId = stringValue(student.getStudentNumber());
        if (studentId.isBlank()) {
            log.warn("Skipping Library profile sync: blank student number for user {}", user.getUsername());
            return;
        }

        if (libraryProfileExistsByStudentId(studentId)) {
            linkLibraryProfileToUser(studentId, user.getId());
            return;
        }

        String fullName = stringValue(student.getFullName());
        if (fullName.isBlank()) {
            fullName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        }

        String course = student.getDepartment() != null ? stringValue(student.getDepartment().getName()) : "General";

        jdbcTemplate.update("""
                INSERT INTO library.student_profiles
                    (student_id, full_name, phone, course, user_id, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())
                """,
                studentId,
                fullName,
                stringValue(student.getContactNumber()),
                course,
                user.getId());

        log.info("Auto-provisioned Library student profile {} for Attendance user {}", studentId, user.getUsername());
    }

    private Optional<Long> findLibraryProfileId(Long userId, String studentNumber) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM library.student_profiles
                WHERE user_id = ?
                   OR (? <> '' AND LOWER(student_id) = LOWER(?))
                LIMIT 1
                """, Long.class, userId, stringValue(studentNumber), stringValue(studentNumber));
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    private boolean libraryProfileExistsByStudentId(String studentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library.student_profiles WHERE LOWER(student_id) = LOWER(?)",
                Integer.class,
                studentId);
        return count != null && count > 0;
    }

    private void linkLibraryProfileToUser(String studentId, Long userId) {
        jdbcTemplate.update(
                "UPDATE library.student_profiles SET user_id = ? WHERE LOWER(student_id) = LOWER(?)",
                userId, studentId);
        log.info("Linked existing Library profile {} to user id {}", studentId, userId);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
