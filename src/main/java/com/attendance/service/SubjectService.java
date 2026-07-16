package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final DomainValidationService domainValidationService;

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    public Page<Subject> findPage(Long departmentId, Pageable pageable) {
        if (departmentId != null) {
            return subjectRepository.findByDepartmentId(departmentId, pageable);
        }
        return subjectRepository.findAllByOrderBySubjectNameAsc(pageable);
    }

    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    public List<Subject> findByTeacherId(Long teacherId) {
        return subjectRepository.findByTeacherId(teacherId);
    }

    @Transactional
    public Subject save(Subject subject) {
        validateSubjectFields(subject, null);
        if (subjectRepository.existsBySubjectCode(subject.getSubjectCode().trim())) {
            throw new BusinessException("Subject code already exists.");
        }
        resolveRelations(subject);
        domainValidationService.validateSubject(subject);
        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject update(Long id, Subject updated) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Subject not found."));
        validateSubjectFields(updated, id);
        subject.setSubjectName(updated.getSubjectName().trim());
        subject.setDescription(updated.getDescription());
        if (updated.getDepartment() != null && updated.getDepartment().getId() != null) {
            subject.setDepartment(domainValidationService.requireDepartment(updated.getDepartment().getId()));
        }
        if (updated.getTeacher() != null && updated.getTeacher().getId() != null) {
            Teacher teacher = teacherRepository.findById(updated.getTeacher().getId())
                    .orElseThrow(() -> new BusinessException("Teacher not found."));
            subject.setTeacher(teacher);
        }
        if (updated.getSection() != null && updated.getSection().getId() != null) {
            Section section = sectionRepository.findById(updated.getSection().getId())
                    .orElseThrow(() -> new BusinessException("Section not found."));
            subject.setSection(section);
        }
        domainValidationService.validateSubject(subject);
        return subjectRepository.save(subject);
    }

    @Transactional
    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new BusinessException("Subject not found.");
        }
        if (!enrollmentRepository.findBySubjectId(id).isEmpty()) {
            throw new BusinessException("Cannot delete subject with enrolled students.");
        }
        subjectRepository.deleteById(id);
    }

    @Transactional
    public void assignStudents(Long subjectId, List<Long> studentIds) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Subject not found."));
        for (Long studentId : studentIds) {
            if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new BusinessException("Student not found: " + studentId));
                enrollmentRepository.save(Enrollment.builder()
                        .student(student)
                        .subject(subject)
                        .build());
            }
        }
    }

    public List<Enrollment> getEnrollments(Long subjectId) {
        return enrollmentRepository.findBySubjectId(subjectId);
    }

    @Transactional
    public List<Subject> findByDepartmentId(Long departmentId) {
        if (departmentId == null) {
            return List.of();
        }
        List<Subject> subjects = subjectRepository.findAllForDepartment(departmentId);
        for (Subject subject : subjects) {
            repairDepartmentIfMissing(subject, departmentId);
        }
        return subjects.stream()
                .filter(this::isDisplayableSubject)
                .sorted(Comparator.comparing(
                        s -> s.getSubjectName() != null ? s.getSubjectName().toLowerCase() : "",
                        Comparator.naturalOrder()))
                .toList();
    }

    private void repairDepartmentIfMissing(Subject subject, Long departmentId) {
        Department target = null;
        if (subject.getSection() != null
                && subject.getSection().getDepartment() != null
                && departmentId.equals(subject.getSection().getDepartment().getId())) {
            target = subject.getSection().getDepartment();
        } else if (subject.getTeacher() != null
                && subject.getTeacher().getDepartment() != null
                && departmentId.equals(subject.getTeacher().getDepartment().getId())) {
            target = subject.getTeacher().getDepartment();
        }
        if (target == null) {
            return;
        }
        if (subject.getDepartment() == null
                || subject.getDepartment().getId() == null
                || !departmentId.equals(subject.getDepartment().getId())) {
            subject.setDepartment(target);
            subjectRepository.save(subject);
        }
    }

    private boolean isDisplayableSubject(Subject subject) {
        return subject != null
                && subject.getSubjectCode() != null && !subject.getSubjectCode().isBlank()
                && subject.getSubjectName() != null && !subject.getSubjectName().isBlank()
                && subject.getTeacher() != null
                && subject.getSection() != null;
    }

    private void resolveRelations(Subject subject) {
        if (subject.getDepartment() == null || subject.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        subject.setDepartment(domainValidationService.requireDepartment(subject.getDepartment().getId()));

        if (subject.getTeacher() == null || subject.getTeacher().getId() == null) {
            throw new BusinessException("Teacher is required.");
        }
        Teacher teacher = teacherRepository.findById(subject.getTeacher().getId())
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        subject.setTeacher(teacher);

        if (subject.getSection() == null || subject.getSection().getId() == null) {
            throw new BusinessException("Section is required.");
        }
        Section section = sectionRepository.findById(subject.getSection().getId())
                .orElseThrow(() -> new BusinessException("Section not found."));
        subject.setSection(section);
    }

    private void validateSubjectFields(Subject subject, Long currentId) {
        if (subject.getSubjectCode() == null || subject.getSubjectCode().isBlank()) {
            throw new BusinessException("Subject code is required.");
        }
        if (subject.getSubjectName() == null || subject.getSubjectName().isBlank()) {
            throw new BusinessException("Subject name is required.");
        }
        if (subject.getDepartment() == null || subject.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        if (subject.getTeacher() == null || subject.getTeacher().getId() == null) {
            throw new BusinessException("Teacher is required.");
        }
        if (subject.getSection() == null || subject.getSection().getId() == null) {
            throw new BusinessException("Section is required.");
        }
        String code = subject.getSubjectCode().trim();
        boolean duplicateCode = currentId == null
                ? subjectRepository.existsBySubjectCode(code)
                : subjectRepository.existsBySubjectCodeAndIdNot(code, currentId);
        if (duplicateCode) {
            throw new BusinessException("Subject code already exists.");
        }
    }

    public List<Subject> filterBySearch(List<Subject> subjects, String query) {
        if (query == null || query.isBlank()) {
            return subjects;
        }
        String q = query.trim().toLowerCase();
        return subjects.stream()
                .filter(s -> containsIgnoreCase(s.getSubjectCode(), q)
                        || containsIgnoreCase(s.getSubjectName(), q)
                        || (s.getTeacher() != null && containsIgnoreCase(s.getTeacher().getFullName(), q)))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
