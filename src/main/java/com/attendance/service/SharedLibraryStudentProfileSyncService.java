package com.attendance.service;

import com.attendance.model.Student;
import com.attendance.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
    private final SharedLibrarySchemaRepairService sharedLibrarySchemaRepairService;

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

        if (!publicUserExists(user.getId())) {
            log.warn("Skipping Library profile sync: user id {} not found in public.users for username={}",
                    user.getId(), user.getUsername());
            return;
        }

        String course = student.getDepartment() != null ? stringValue(student.getDepartment().getName()) : "General";

        try {
            insertLibraryProfile(studentId, fullName, student, course, user.getId());
        } catch (DataAccessException ex) {
            if (isMissingPublicUserForeignKey(ex)) {
                log.warn("Library profile sync hit stale FK (library.users); repairing and retrying for user id {}",
                        user.getId());
                sharedLibrarySchemaRepairService.repairStudentProfileUserForeignKey();
                insertLibraryProfile(studentId, fullName, student, course, user.getId());
            } else {
                throw ex;
            }
        }

        log.info("Auto-provisioned Library student profile {} for Attendance user {}", studentId, user.getUsername());
    }

    private void insertLibraryProfile(String studentId, String fullName, Student student, String course, Long userId) {
        jdbcTemplate.update("""
                INSERT INTO library.student_profiles
                    (student_id, full_name, phone, course, user_id, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())
                """,
                studentId,
                fullName,
                stringValue(student.getContactNumber()),
                course,
                userId);
    }

    private boolean publicUserExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.users WHERE id = ?",
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    private boolean isMissingPublicUserForeignKey(DataAccessException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null
                && message.contains("fk_student_profiles_user")
                && message.contains("is not present in table \"users\"");
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
