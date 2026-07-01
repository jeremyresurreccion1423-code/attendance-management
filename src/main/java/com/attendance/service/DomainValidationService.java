package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.Department;
import com.attendance.model.Section;
import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DomainValidationService {

    private final DepartmentRepository departmentRepository;

    public Department requireDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new BusinessException("Department is required.");
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("Department does not exist."));
    }

    public void validateTeacherDepartment(Teacher teacher, Department department) {
        if (teacher == null || teacher.getId() == null) {
            throw new BusinessException("Subject must have a teacher.");
        }
        if (teacher.getDepartment() == null || department == null) {
            throw new BusinessException("Teacher must belong to a department.");
        }
        if (!teacher.getDepartment().getId().equals(department.getId())) {
            throw new BusinessException("Teacher must belong to the selected department.");
        }
    }

    public void validateSectionDepartment(Section section, Department department) {
        if (section == null || section.getId() == null) {
            throw new BusinessException("Subject must have a section.");
        }
        if (section.getDepartment() == null || department == null) {
            throw new BusinessException("Section must belong to a department.");
        }
        if (!section.getDepartment().getId().equals(department.getId())) {
            throw new BusinessException("Section must belong to the selected department.");
        }
    }

    public void validateSubject(Subject subject) {
        if (subject.getDepartment() == null || subject.getDepartment().getId() == null) {
            throw new BusinessException("Subject must belong to a department.");
        }
        Department department = requireDepartment(subject.getDepartment().getId());
        if (subject.getTeacher() == null || subject.getTeacher().getId() == null) {
            throw new BusinessException("Subject must have a teacher.");
        }
        if (subject.getSection() == null || subject.getSection().getId() == null) {
            throw new BusinessException("Subject must have a section.");
        }
        validateTeacherDepartment(subject.getTeacher(), department);
        validateSectionDepartment(subject.getSection(), department);
    }
}
