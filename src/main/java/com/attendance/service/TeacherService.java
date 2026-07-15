package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private static final Pattern EMPLOYEE_ID_EMP = Pattern.compile("^EMP-(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPLOYEE_ID_EMP_COMPACT = Pattern.compile("^EMP(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿÑñ .'-]{2,150}$");

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final AuthService authService;
    private final DomainValidationService domainValidationService;

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public Page<Teacher> findPage(Long departmentId, Pageable pageable) {
        if (departmentId != null) {
            return teacherRepository.findByDepartmentId(departmentId, pageable);
        }
        return teacherRepository.findAllByOrderByFullNameAsc(pageable);
    }

    public Optional<Teacher> findById(Long id) {
        return teacherRepository.findById(id);
    }

    public Optional<Teacher> findByUserId(Long userId) {
        return teacherRepository.findByUserId(userId);
    }

    /**
     * Next auto employee ID in the series {@code EMP-001}, {@code EMP-002}, ...
     */
    public String generateNextEmployeeId() {
        int maxSeq = 0;
        for (Teacher existing : teacherRepository.findAll()) {
            if (existing.getEmployeeId() == null) {
                continue;
            }
            String id = existing.getEmployeeId().trim();
            Matcher dashed = EMPLOYEE_ID_EMP.matcher(id);
            Matcher compact = EMPLOYEE_ID_EMP_COMPACT.matcher(id);
            if (dashed.matches()) {
                maxSeq = Math.max(maxSeq, Integer.parseInt(dashed.group(1)));
            } else if (compact.matches()) {
                maxSeq = Math.max(maxSeq, Integer.parseInt(compact.group(1)));
            }
        }
        return "EMP-" + String.format("%03d", maxSeq + 1);
    }

    @Transactional
    public Teacher save(Teacher teacher, String username, String password) {
        // Always assign the next EMP-XXX ID for new teachers.
        teacher.setEmployeeId(generateNextEmployeeId());

        validateTeacher(teacher, null);

        if (teacherRepository.existsByEmployeeId(teacher.getEmployeeId().trim())) {
            teacher.setEmployeeId(generateNextEmployeeId());
            if (teacherRepository.existsByEmployeeId(teacher.getEmployeeId().trim())) {
                throw new BusinessException("Unable to generate a unique employee ID. Please try again.");
            }
        }

        teacher.setEmail(teacher.getEmail().trim().toLowerCase());
        if (teacherRepository.existsByEmailIgnoreCase(teacher.getEmail())) {
            throw new BusinessException("Email already exists.");
        }

        Department department = domainValidationService.requireDepartment(teacher.getDepartment().getId());
        teacher.setDepartment(department);

        String loginUsername = (username == null || username.isBlank())
                ? teacher.getEmployeeId().trim()
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
                if (orphan.getRole() == Role.TEACHER && teacherRepository.findByUserId(orphan.getId()).isEmpty()) {
                    authService.changePassword(orphan, password);
                    orphan.setEmail(teacher.getEmail());
                    orphan.setFullName(teacher.getFullName().trim());
                    orphan.setEnabled(true);
                    teacher.setUser(orphan);
                } else {
                    throw new BusinessException(
                            "Login username '" + loginUsername + "' already exists. "
                                    + "Choose a different username, or leave Login Username/Password blank.");
                }
            } else {
                User user = authService.createUser(
                        loginUsername, password, Role.TEACHER, teacher.getEmail(), teacher.getFullName());
                teacher.setUser(user);
            }
        }
        if (teacher.getStatus() == null) {
            teacher.setStatus(TeacherStatus.ACTIVE);
        }
        if (teacher.getContactNumber() != null && !teacher.getContactNumber().isBlank()) {
            teacher.setContactNumber(teacher.getContactNumber().trim());
        } else {
            teacher.setContactNumber(null);
        }
        Teacher saved = teacherRepository.save(teacher);
        syncLoginAccessWithStatus(saved);
        return saved;
    }

    @Transactional
    public Teacher update(Long id, Teacher updated) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        validateTeacher(updated, id);
        if (updated.getDepartment() == null || updated.getDepartment().getId() == null) {
            throw new BusinessException("Teacher must belong to a department.");
        }
        Department department = domainValidationService.requireDepartment(updated.getDepartment().getId());
        if (updated.getEmail() != null && !updated.getEmail().isBlank()
                && teacherRepository.existsByEmailIgnoreCaseAndIdNot(updated.getEmail().trim(), id)) {
            throw new BusinessException("Email already exists.");
        }
        teacher.setFullName(updated.getFullName().trim());
        teacher.setDepartment(department);
        teacher.setContactNumber(updated.getContactNumber() != null && !updated.getContactNumber().isBlank()
                ? updated.getContactNumber().trim() : null);
        teacher.setEmail(updated.getEmail().trim().toLowerCase());
        if (updated.getStatus() != null) {
            teacher.setStatus(updated.getStatus());
        }
        Teacher saved = teacherRepository.save(teacher);
        syncLoginAccessWithStatus(saved);
        return saved;
    }

    private void validateTeacher(Teacher teacher, Long currentId) {
        if (teacher.getFullName() == null || teacher.getFullName().isBlank()) {
            throw new BusinessException("Full name is required.");
        }
        String fullName = teacher.getFullName().trim();
        if (fullName.length() < 2) {
            throw new BusinessException("Full name must be at least 2 characters.");
        }
        if (!FULL_NAME_PATTERN.matcher(fullName).matches()) {
            throw new BusinessException("Full name may only contain letters, spaces, and . ' - characters.");
        }
        teacher.setFullName(fullName);

        if (currentId == null && (teacher.getEmployeeId() == null || teacher.getEmployeeId().isBlank())) {
            throw new BusinessException("Employee ID is required.");
        }
        if (teacher.getDepartment() == null || teacher.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            throw new BusinessException("Email is required.");
        }
        if (teacher.getContactNumber() != null && !teacher.getContactNumber().isBlank()) {
            String contact = teacher.getContactNumber().trim();
            if (!contact.matches("^[0-9]+$")) {
                throw new BusinessException("Contact number must contain digits only.");
            }
            if (contact.length() < 7 || contact.length() > 15) {
                throw new BusinessException("Contact number must be 7 to 15 digits.");
            }
            teacher.setContactNumber(contact);
        }
    }

    private void syncLoginAccessWithStatus(Teacher teacher) {
        if (teacher.getUser() == null || teacher.getStatus() == null) {
            return;
        }
        boolean canLogin = teacher.getStatus() == TeacherStatus.ACTIVE;
        authService.updateAccountEnabledById(teacher.getUser().getId(), canLogin);
    }

    @Transactional
    public void delete(Long id) {
        if (subjectRepository.countByTeacherId(id) > 0) {
            throw new BusinessException("Cannot delete teacher assigned to one or more subjects.");
        }
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        User linkedUser = teacher.getUser();
        teacher.setUser(null);
        teacherRepository.save(teacher);
        teacherRepository.delete(teacher);
        if (linkedUser != null) {
            authService.disableAndReleaseUsername(linkedUser);
        }
    }

    public List<Teacher> filterBySearch(List<Teacher> teachers, String query) {
        if (query == null || query.isBlank()) {
            return teachers;
        }
        String q = query.trim().toLowerCase();
        return teachers.stream()
                .filter(t -> containsIgnoreCase(t.getFullName(), q)
                        || containsIgnoreCase(t.getEmployeeId(), q)
                        || containsIgnoreCase(t.getEmail(), q))
                .toList();
    }

    public List<Teacher> filterByStatus(List<Teacher> teachers, TeacherStatus status) {
        if (status == null) {
            return teachers;
        }
        return teachers.stream().filter(t -> t.getStatus() == status).toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    @Transactional
    public void assignSubjects(Long teacherId, List<Long> subjectIds) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        for (Long subjectId : subjectIds) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new BusinessException("Subject not found: " + subjectId));
            subject.setTeacher(teacher);
            subjectRepository.save(subject);
        }
    }

    public long count() {
        return teacherRepository.count();
    }

    public List<Teacher> findByDepartmentId(Long departmentId) {
        return teacherRepository.findByDepartmentIdOrderByFullNameAsc(departmentId);
    }
}
