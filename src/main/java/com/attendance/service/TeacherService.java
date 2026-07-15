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

@Service
@RequiredArgsConstructor
public class TeacherService {

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

    @Transactional
    public Teacher save(Teacher teacher, String username, String password) {
        if (teacher.getFullName() == null || teacher.getFullName().isBlank()) {
            throw new BusinessException("Full name is required.");
        }
        if (teacher.getEmployeeId() == null || teacher.getEmployeeId().isBlank()) {
            throw new BusinessException("Employee ID is required.");
        }
        if (teacher.getDepartment() == null || teacher.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            throw new BusinessException("Email is required.");
        }
        if (teacher.getContactNumber() != null && !teacher.getContactNumber().isBlank()
                && !teacher.getContactNumber().trim().matches("^[0-9]+$")) {
            throw new BusinessException("Contact number must contain digits only.");
        }
        Department department = domainValidationService.requireDepartment(teacher.getDepartment().getId());
        teacher.setDepartment(department);

        if (teacherRepository.existsByEmployeeId(teacher.getEmployeeId().trim())) {
            throw new BusinessException("Employee ID already exists.");
        }
        if (teacherRepository.existsByEmailIgnoreCase(teacher.getEmail().trim())) {
            throw new BusinessException("Email already exists.");
        }

        String loginUsername = (username == null || username.isBlank())
                ? teacher.getEmployeeId().trim()
                : username.trim();
        if (password != null && !password.isBlank()) {
            if (authService.findByUsername(loginUsername).isPresent()) {
                throw new BusinessException("Username already exists.");
            }
            User user = authService.createUser(
                    loginUsername, password, Role.TEACHER, teacher.getEmail(), teacher.getFullName());
            teacher.setUser(user);
        }
        if (teacher.getStatus() == null) {
            teacher.setStatus(TeacherStatus.ACTIVE);
        }
        Teacher saved = teacherRepository.save(teacher);
        syncLoginAccessWithStatus(saved);
        return saved;
    }

    @Transactional
    public Teacher update(Long id, Teacher updated) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        if (updated.getDepartment() == null || updated.getDepartment().getId() == null) {
            throw new BusinessException("Teacher must belong to a department.");
        }
        Department department = domainValidationService.requireDepartment(updated.getDepartment().getId());
        if (updated.getEmail() != null && !updated.getEmail().isBlank()
                && teacherRepository.existsByEmailIgnoreCaseAndIdNot(updated.getEmail().trim(), id)) {
            throw new BusinessException("Email already exists.");
        }
        teacher.setFullName(updated.getFullName());
        teacher.setDepartment(department);
        teacher.setContactNumber(updated.getContactNumber());
        teacher.setEmail(updated.getEmail());
        if (updated.getStatus() != null) {
            teacher.setStatus(updated.getStatus());
        }
        Teacher saved = teacherRepository.save(teacher);
        syncLoginAccessWithStatus(saved);
        return saved;
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
        teacherRepository.deleteById(id);
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
