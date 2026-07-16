package com.attendance.service;

import com.attendance.dto.StudentSubjectRow;
import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final TimetableRepository timetableRepository;
    private final AttendanceQRRepository attendanceQRRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;
    private final JdbcTemplate jdbcTemplate;
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
        deleteRelatedRecords(id);
        subjectRepository.deleteById(id);
    }

    /**
     * Removes incomplete subject rows for a department so department SUBJECTS counts
     * match what admins see on the Subjects page.
     */
    @Transactional
    public int purgeIncompleteForDepartment(Long departmentId) {
        if (departmentId == null) {
            return 0;
        }
        List<Subject> incomplete = subjectRepository.findIncompleteByDepartmentId(departmentId);
        for (Subject subject : incomplete) {
            deleteRelatedRecords(subject.getId());
            subjectRepository.delete(subject);
        }
        return incomplete.size();
    }

    private void deleteRelatedRecords(Long subjectId) {
        deleteFaceRecognitionLogsIfPresent(subjectId);
        attendanceRepository.deleteBySubjectId(subjectId);
        markRepository.deleteBySubjectId(subjectId);
        enrollmentRepository.deleteBySubjectId(subjectId);
        attendanceQRRepository.deleteBySubjectId(subjectId);
        timetableRepository.deleteBySubjectId(subjectId);
    }

    /** Safe on DBs that never created face_recognition_log (ddl-auto=none). */
    private void deleteFaceRecognitionLogsIfPresent(Long subjectId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1 FROM information_schema.tables
                            WHERE table_schema = 'public' AND table_name = 'face_recognition_log'
                        )
                        """,
                Boolean.class);
        if (Boolean.TRUE.equals(exists)) {
            jdbcTemplate.update("DELETE FROM face_recognition_log WHERE subject_id = ?", subjectId);
        }
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

    @Transactional(readOnly = true)
    public List<StudentSubjectRow> listSubjectsForStudent(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> e.getSubject() != null)
                .map(e -> {
                    Subject subject = e.getSubject();
                    String teacher = subject.getTeacher() != null && subject.getTeacher().getFullName() != null
                            ? subject.getTeacher().getFullName() : "-";
                    String section = subject.getSection() != null && subject.getSection().getName() != null
                            ? subject.getSection().getName() : "-";
                    String department = subject.getDepartment() != null && subject.getDepartment().getName() != null
                            ? subject.getDepartment().getName() : "-";
                    return new StudentSubjectRow(
                            subject.getSubjectCode() != null ? subject.getSubjectCode() : "-",
                            subject.getSubjectName() != null ? subject.getSubjectName() : "-",
                            teacher,
                            section,
                            department
                    );
                })
                .sorted(Comparator.comparing(StudentSubjectRow::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
