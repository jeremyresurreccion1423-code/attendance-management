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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AuthService authService;
    private final DomainValidationService domainValidationService;
    private final SharedLibraryStudentProfileSyncService sharedLibraryStudentProfileSyncService;

    public List<Student> findAll() {
        return studentRepository.findAll();
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

    @Transactional
    public Student save(Student student, String username, String password) {
        validateStudent(student, null);

        if (studentRepository.existsByStudentNumber(student.getStudentNumber())) {
            throw new BusinessException("Student number already exists.");
        }
        if (student.getEmail() != null && !student.getEmail().isBlank()
                && studentRepository.existsByEmailIgnoreCase(student.getEmail().trim())) {
            throw new BusinessException("Email already exists.");
        }

        resolveRelations(student);

        String loginUsername = (username == null || username.isBlank())
                ? student.getStudentNumber()
                : username.trim();
        if (password != null && !password.isBlank()) {
            User user = authService.createUser(
                    loginUsername, password, Role.STUDENT, student.getEmail(), student.getFullName());
            student.setUser(user);
        }
        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.ACTIVE);
        }
        Student saved = studentRepository.save(student);
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
        student.setFullName(updated.getFullName());
        student.setYearLevel(updated.getYearLevel());
        student.setContactNumber(updated.getContactNumber());
        student.setEmail(updated.getEmail());
        student.setStatus(updated.getStatus());
        student.setDepartment(domainValidationService.requireDepartment(updated.getDepartment().getId()));
        if (updated.getSection() != null && updated.getSection().getId() != null) {
            Section section = sectionRepository.findById(updated.getSection().getId())
                    .orElseThrow(() -> new BusinessException("Section not found."));
            if (section.getDepartment() == null
                    || !section.getDepartment().getId().equals(student.getDepartment().getId())) {
                throw new BusinessException("Section must belong to the student's department.");
            }
            if (updated.getYearLevel() != null && !updated.getYearLevel().equals(section.getYearLevel())) {
                throw new BusinessException("Section must match the selected year level.");
            }
            student.setSection(section);
        }
        return studentRepository.save(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.deleteById(id);
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

    private void validateStudent(Student student, Long currentId) {
        if (student.getDepartment() == null || student.getDepartment().getId() == null) {
            throw new BusinessException("Student must belong to a department.");
        }
        if (student.getSection() == null || student.getSection().getId() == null) {
            throw new BusinessException("Student must belong to a section.");
        }
        if (student.getYearLevel() == null || student.getYearLevel().isBlank()) {
            throw new BusinessException("Year level is required.");
        }
        if (student.getEmail() != null && !student.getEmail().isBlank() && currentId != null
                && studentRepository.existsByEmailIgnoreCaseAndIdNot(student.getEmail().trim(), currentId)) {
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
