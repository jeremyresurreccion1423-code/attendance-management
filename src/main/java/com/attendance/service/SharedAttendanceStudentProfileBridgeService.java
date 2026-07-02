package com.attendance.service;

import com.attendance.model.Department;
import com.attendance.model.Section;
import com.attendance.model.Student;
import com.attendance.model.StudentStatus;
import com.attendance.model.User;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.SectionRepository;
import com.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ensures {@code public.students} exists for shared {@code public.users} accounts,
 * using Library registration data when available.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedAttendanceStudentProfileBridgeService {

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Student> ensureAttendanceStudent(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }

        Optional<Student> existing = studentRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            return existing;
        }

        List<Map<String, Object>> libraryProfiles = findLibraryProfiles(user);
        if (!libraryProfiles.isEmpty()) {
            Map<String, Object> profile = libraryProfiles.get(0);
            return syncFromLibraryProfile(
                    user,
                    stringValue(profile.get("student_id")),
                    stringValue(profile.get("full_name")),
                    stringValue(profile.get("phone")),
                    stringValue(profile.get("course")),
                    user.getEmail());
        }

        return syncFromLibraryProfile(
                user,
                "",
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                "",
                "General",
                user.getEmail());
    }

    /**
     * Creates or links an Attendance student from Library registration data.
     * Safe to call multiple times — skips when a profile already exists.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Student> syncFromLibraryProfile(
            User user,
            String studentNumber,
            String fullName,
            String contactNumber,
            String courseName,
            String email) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }

        Optional<Student> byUser = studentRepository.findByUserId(user.getId());
        if (byUser.isPresent()) {
            return byUser;
        }

        studentNumber = stringValue(studentNumber);
        if (!studentNumber.isBlank()) {
            Optional<Student> byNumber = studentRepository.findByStudentNumber(studentNumber);
            if (byNumber.isPresent()) {
                Student linked = byNumber.get();
                if (linked.getUser() == null || !user.getId().equals(linked.getUser().getId())) {
                    linked.setUser(user);
                    linked = studentRepository.save(linked);
                    log.info("Linked Attendance student {} to shared user {}", studentNumber, user.getUsername());
                }
                return Optional.of(linked);
            }
        } else {
            studentNumber = nextStudentNumberFromLibrary(user);
            if (studentNumber.isBlank()) {
                studentNumber = generateFallbackStudentNumber();
            }
        }

        if (fullName.isBlank()) {
            fullName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getUsername();
        }

        Department department = resolveDepartment(courseName);
        Section section = resolveDefaultSection(department);

        String resolvedEmail = stringValue(email);
        if (!resolvedEmail.isBlank() && studentRepository.existsByEmailIgnoreCase(resolvedEmail)) {
            resolvedEmail = null;
        }

        Student student = Student.builder()
                .user(user)
                .studentNumber(studentNumber)
                .fullName(fullName)
                .email(resolvedEmail)
                .contactNumber(stringValue(contactNumber))
                .department(department)
                .section(section)
                .yearLevel(section.getYearLevel())
                .status(StudentStatus.ACTIVE)
                .build();

        Student saved = studentRepository.save(student);
        log.info("Auto-provisioned Attendance student {} for shared user {}", studentNumber, user.getUsername());
        return Optional.of(saved);
    }

    private List<Map<String, Object>> findLibraryProfiles(User user) {
        String email = stringValue(user.getEmail());
        String username = stringValue(user.getUsername());

        return jdbcTemplate.queryForList("""
                SELECT sp.student_id, sp.full_name, sp.phone, sp.course
                FROM library.student_profiles sp
                INNER JOIN public.users lu ON lu.id = sp.user_id
                WHERE sp.user_id = ?
                   OR (? <> '' AND LOWER(lu.email) = LOWER(?))
                   OR (? <> '' AND LOWER(lu.username) = LOWER(?))
                ORDER BY CASE WHEN sp.user_id = ? THEN 0 ELSE 1 END
                LIMIT 1
                """,
                user.getId(),
                email, email,
                username, username,
                user.getId());
    }

    private String nextStudentNumberFromLibrary(User user) {
        List<String> ids = jdbcTemplate.queryForList("""
                SELECT sp.student_id
                FROM library.student_profiles sp
                WHERE sp.user_id = ?
                LIMIT 1
                """, String.class, user.getId());
        return ids.isEmpty() ? "" : stringValue(ids.get(0));
    }

    private String generateFallbackStudentNumber() {
        long count = studentRepository.count();
        return "101-" + String.format("%03d", count + 1);
    }

    private Department resolveDepartment(String course) {
        if (!course.isBlank()) {
            return departmentRepository.findByNameIgnoreCase(course)
                    .orElseGet(() -> departmentRepository.save(Department.builder()
                            .name(course)
                            .description("Synced from Library registration")
                            .build()));
        }
        return departmentRepository.findByNameIgnoreCase("General")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name("General")
                        .description("Default department")
                        .build()));
    }

    private Section resolveDefaultSection(Department department) {
        List<Section> sections = sectionRepository.findByDepartmentIdOrderBySectionNameAsc(department.getId());
        if (!sections.isEmpty()) {
            return sections.get(0);
        }
        return sectionRepository.save(Section.builder()
                .sectionName(department.getName() + "-LIB")
                .department(department)
                .yearLevel("1st Year")
                .build());
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
