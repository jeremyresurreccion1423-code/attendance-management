package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private static final Pattern STUDENT_NUMBER_101 = Pattern.compile("^101-(\\d+)$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿÑñ .'-]{2,150}$");

    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AuthService authService;
    private final DomainValidationService domainValidationService;
    private final SharedLibraryStudentProfileSyncService sharedLibraryStudentProfileSyncService;

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    /** Newest students first (for Create dashboard / recent lists). */
    public List<Student> findRecent(int limit) {
        int max = Math.max(1, limit);
        return studentRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(Student::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Student::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(max)
                .toList();
    }

    public Page<Student> findPage(Long departmentId, Pageable pageable) {
        if (departmentId != null) {
            return studentRepository.findByDepartmentId(departmentId, pageable);
        }
        return studentRepository.findAllByOrderByFullNameAsc(pageable);
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    public Optional<Student> findByUserId(Long userId) {
        return studentRepository.findByUserId(userId);
    }

    /**
     * Next auto student number in the series {@code 101-001}, {@code 101-002}, ...
     */
    public String generateNextStudentNumber() {
        int maxSeq = 0;
        for (Student existing : studentRepository.findAll()) {
            if (existing.getStudentNumber() == null) {
                continue;
            }
            Matcher matcher = STUDENT_NUMBER_101.matcher(existing.getStudentNumber().trim());
            if (matcher.matches()) {
                maxSeq = Math.max(maxSeq, Integer.parseInt(matcher.group(1)));
            }
        }
        return "101-" + String.format("%03d", maxSeq + 1);
    }

    @Transactional
    public Student save(Student student, String username, String password) {
        // Always assign the next 101-XXX number for new students.
        student.setStudentNumber(generateNextStudentNumber());

        validateStudent(student, null);

        if (studentRepository.existsByStudentNumber(student.getStudentNumber().trim())) {
            student.setStudentNumber(generateNextStudentNumber());
            if (studentRepository.existsByStudentNumber(student.getStudentNumber().trim())) {
                throw new BusinessException("Unable to generate a unique student number. Please try again.");
            }
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new BusinessException("Email is required.");
        }
        student.setEmail(student.getEmail().trim().toLowerCase());
        if (studentRepository.existsByEmailIgnoreCase(student.getEmail())) {
            throw new BusinessException("Email already exists.");
        }

        resolveRelations(student);

        String loginUsername = (username == null || username.isBlank())
                ? student.getStudentNumber().trim()
                : username.trim();
        boolean wantsLogin = (username != null && !username.isBlank())
                || (password != null && !password.isBlank());
        if (wantsLogin) {
            if (password == null || password.isBlank()) {
                throw new BusinessException("Login password is required when creating a login account.");
            }
            if (username != null && !username.isBlank() && username.trim().length() < 3) {
                throw new BusinessException("Login username must be at least 3 characters.");
            }
            var existingUser = authService.findByUsername(loginUsername);
            if (existingUser.isPresent()) {
                User orphan = existingUser.get();
                if (orphan.getRole() == Role.STUDENT && studentRepository.findByUserId(orphan.getId()).isEmpty()) {
                    authService.changePassword(orphan, password);
                    orphan.setEmail(student.getEmail());
                    orphan.setFullName(student.getFullName().trim());
                    orphan.setEnabled(true);
                    student.setUser(orphan);
                } else {
                    throw new BusinessException(
                            "Login username '" + loginUsername + "' already exists. "
                                    + "Choose a different username, or leave Login Username/Password blank.");
                }
            } else {
                User user = authService.createUser(
                        loginUsername, password, Role.STUDENT, student.getEmail(), student.getFullName());
                student.setUser(user);
            }
        }
        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.ACTIVE);
        }
        if (student.getContactNumber() != null && !student.getContactNumber().isBlank()) {
            student.setContactNumber(student.getContactNumber().trim());
        } else {
            student.setContactNumber(null);
        }
        Student saved = studentRepository.save(student);
        syncLoginAccessWithStatus(saved);
        if (saved.getUser() != null) {
            try {
                sharedLibraryStudentProfileSyncService.syncFromAttendanceStudent(saved.getUser(), saved);
            } catch (Exception ex) {
                log.error(
                        "Attendance student {} saved but Library profile sync failed for user {}: {}",
                        saved.getStudentNumber(),
                        saved.getUser().getUsername(),
                        ex.getMessage(),
                        ex);
            }
        }
        return saved;
    }

    @Transactional
    public Student update(Long id, Student updated) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Student not found."));
        validateStudent(updated, id);
        student.setFullName(updated.getFullName().trim());
        student.setYearLevel(updated.getYearLevel());
        student.setContactNumber(updated.getContactNumber());
        student.setEmail(updated.getEmail().trim());
        student.setStatus(updated.getStatus());
        student.setDepartment(domainValidationService.requireDepartment(updated.getDepartment().getId()));
        if (updated.getSection() == null || updated.getSection().getId() == null) {
            throw new BusinessException("Section is required.");
        }
        Section section = sectionRepository.findById(updated.getSection().getId())
                .orElseThrow(() -> new BusinessException("Section not found."));
        if (section.getDepartment() == null
                || !section.getDepartment().getId().equals(student.getDepartment().getId())) {
            throw new BusinessException("Section must belong to the student's department.");
        }
        if (updated.getYearLevel() == null || !updated.getYearLevel().equals(section.getYearLevel())) {
            throw new BusinessException("Section must match the selected year level.");
        }
        student.setSection(section);
        Student saved = studentRepository.save(student);
        syncLoginAccessWithStatus(saved);
        return saved;
    }

    private void syncLoginAccessWithStatus(Student student) {
        if (student.getUser() == null || student.getStatus() == null) {
            return;
        }
        boolean canLogin = student.getStatus() == StudentStatus.ACTIVE;
        authService.updateAccountEnabledById(student.getUser().getId(), canLogin);
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Student not found."));
        User linkedUser = student.getUser();
        student.setUser(null);
        studentRepository.save(student);
        studentRepository.delete(student);
        if (linkedUser != null) {
            // Free login username so a new student can reuse it.
            authService.disableAndReleaseUsername(linkedUser);
        }
    }

    @Transactional
    public void archive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Student not found."));
        student.setStatus(StudentStatus.ARCHIVED);
        Student saved = studentRepository.save(student);
        syncLoginAccessWithStatus(saved);
    }

    @Transactional
    public void unarchive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Student not found."));
        student.setStatus(StudentStatus.ACTIVE);
        Student saved = studentRepository.save(student);
        syncLoginAccessWithStatus(saved);
    }

    public long count() {
        return studentRepository.count();
    }

    public List<Student> findByDepartmentId(Long departmentId) {
        return studentRepository.findByDepartmentIdOrderByFullNameAsc(departmentId);
    }

    public List<Student> findByDepartmentIdAndYearLevel(Long departmentId, String yearLevel) {
        return studentRepository.findByDepartmentIdAndYearLevelOrderByFullNameAsc(departmentId, yearLevel);
    }

    public List<Student> findByDepartmentIdAndYearLevelAndSection(Long departmentId, String yearLevel, Long sectionId) {
        return studentRepository.findByDepartmentIdAndYearLevelAndSectionIdOrderByFullNameAsc(departmentId, yearLevel, sectionId);
    }

    public List<Student> filterBySearch(List<Student> students, String query) {
        if (query == null || query.isBlank()) {
            return students;
        }
        String q = query.trim().toLowerCase();
        return students.stream()
                .filter(s -> matchesSearch(s, q))
                .toList();
    }

    public List<Student> filterByStatus(List<Student> students, StudentStatus status) {
        if (status == null) {
            return students;
        }
        return students.stream()
                .filter(s -> s.getStatus() == status)
                .toList();
    }

    private void validateStudent(Student student, Long currentId) {
        if (student.getFullName() == null || student.getFullName().isBlank()) {
            throw new BusinessException("Full name is required.");
        }
        String fullName = student.getFullName().trim();
        if (fullName.length() < 2) {
            throw new BusinessException("Full name must be at least 2 characters.");
        }
        if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            throw new BusinessException("Full name may only contain letters, spaces, and . ' - characters.");
        }
        student.setFullName(fullName);

        if (currentId == null && (student.getStudentNumber() == null || student.getStudentNumber().isBlank())) {
            throw new BusinessException("Student number is required.");
        }
        if (student.getDepartment() == null || student.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        if (student.getSection() == null || student.getSection().getId() == null) {
            throw new BusinessException("Section is required.");
        }
        if (student.getYearLevel() == null || student.getYearLevel().isBlank()) {
            throw new BusinessException("Year level is required.");
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new BusinessException("Email is required.");
        }
        if (student.getContactNumber() != null && !student.getContactNumber().isBlank()) {
            String contact = student.getContactNumber().trim();
            if (!contact.matches("^[0-9]+$")) {
                throw new BusinessException("Contact number must contain digits only.");
            }
            if (contact.length() < 7 || contact.length() > 15) {
                throw new BusinessException("Contact number must be 7 to 15 digits.");
            }
            student.setContactNumber(contact);
        }
        if (currentId != null && studentRepository.existsByEmailIgnoreCaseAndIdNot(student.getEmail().trim(), currentId)) {
            throw new BusinessException("Email already exists.");
        }
    }

    private void resolveRelations(Student student) {
        Department department = domainValidationService.requireDepartment(student.getDepartment().getId());
        student.setDepartment(department);
        Section section = sectionRepository.findById(student.getSection().getId())
                .orElseThrow(() -> new BusinessException("Section not found."));
        if (section.getDepartment() == null || !section.getDepartment().getId().equals(department.getId())) {
            throw new BusinessException("Section must belong to the selected department.");
        }
        if (!student.getYearLevel().equals(section.getYearLevel())) {
            throw new BusinessException("Section must match the selected year level.");
        }
        student.setSection(section);
    }

    private boolean matchesSearch(Student student, String query) {
        return containsIgnoreCase(student.getFullName(), query)
                || containsIgnoreCase(student.getStudentNumber(), query)
                || containsIgnoreCase(student.getEmail(), query)
                || (student.getDepartment() != null && containsIgnoreCase(student.getDepartment().getName(), query));
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
