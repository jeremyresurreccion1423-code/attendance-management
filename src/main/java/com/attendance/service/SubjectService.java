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
        if (subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
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
        subject.setSubjectName(updated.getSubjectName());
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

    public List<Subject> findByDepartmentId(Long departmentId) {
        return subjectRepository.findByDepartmentIdOrderBySubjectNameAsc(departmentId);
    }

    private void resolveRelations(Subject subject) {
        if (subject.getDepartment() == null || subject.getDepartment().getId() == null) {
            throw new BusinessException("Subject must belong to a department.");
        }
        subject.setDepartment(domainValidationService.requireDepartment(subject.getDepartment().getId()));

        if (subject.getTeacher() == null || subject.getTeacher().getId() == null) {
            throw new BusinessException("Subject must have a teacher from the selected department.");
        }
        Teacher teacher = teacherRepository.findById(subject.getTeacher().getId())
                .orElseThrow(() -> new BusinessException("Teacher not found."));
        subject.setTeacher(teacher);

        if (subject.getSection() == null || subject.getSection().getId() == null) {
            throw new BusinessException("Subject must have a section. Create a section in the selected department first.");
        }
        Section section = sectionRepository.findById(subject.getSection().getId())
                .orElseThrow(() -> new BusinessException("Section not found."));
        subject.setSection(section);
    }
}
