package com.attendance.service;

import com.attendance.dto.DepartmentDTO;
import com.attendance.exception.BusinessException;
import com.attendance.model.Department;
import com.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    public List<DepartmentDTO> findAllDetailed() {
        return findAll().stream().map(this::toDetailedDto).toList();
    }

    public Page<DepartmentDTO> findAllDetailed(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(this::toDetailedDto);
    }

    public DepartmentDTO findDetailedById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found."));
        return toDetailedDto(department);
    }

    public List<String> findAllNames() {
        return findAll().stream().map(Department::getName).toList();
    }

    @Transactional
    public Department save(Department department) {
        if (department.getName() == null || department.getName().isBlank()) {
            throw new BusinessException("Department name is required.");
        }
        String name = department.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Department already exists.");
        }
        department.setName(name);
        if (department.getDescription() != null) {
            department.setDescription(department.getDescription().trim());
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department update(Long id, Department updated) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found."));
        if (updated.getName() == null || updated.getName().isBlank()) {
            throw new BusinessException("Department name is required.");
        }
        String name = updated.getName().trim();
        departmentRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Department name already exists.");
            }
        });
        department.setName(name);
        department.setDescription(updated.getDescription() != null ? updated.getDescription().trim() : null);
        return departmentRepository.save(department);
    }

    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found."));
        long linked = departmentRepository.countTeachersByDepartmentId(id)
                + departmentRepository.countSectionsByDepartmentId(id)
                + departmentRepository.countStudentsByDepartmentId(id)
                + departmentRepository.countSubjectsByDepartmentId(id);
        if (linked > 0) {
            throw new BusinessException("Cannot delete department with linked teachers, sections, students, or subjects.");
        }
        departmentRepository.delete(department);
    }

    private DepartmentDTO toDetailedDto(Department department) {
        Long id = department.getId();
        return DepartmentDTO.from(
                department,
                departmentRepository.countTeachersByDepartmentId(id),
                departmentRepository.countSectionsByDepartmentId(id),
                departmentRepository.countStudentsByDepartmentId(id),
                departmentRepository.countSubjectsByDepartmentId(id)
        );
    }
}
