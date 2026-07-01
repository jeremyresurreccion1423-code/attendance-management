package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.Department;
import com.attendance.model.Section;
import com.attendance.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final DomainValidationService domainValidationService;

    public List<Section> findAll() {
        return sectionRepository.findAll();
    }

    public Optional<Section> findById(Long id) {
        return sectionRepository.findById(id);
    }

    @Transactional
    public Section save(Section section) {
        if (section.getDepartment() == null || section.getDepartment().getId() == null) {
            throw new BusinessException("Section must belong to a department.");
        }
        if (section.getYearLevel() == null || section.getYearLevel().isBlank()) {
            throw new BusinessException("Year level is required.");
        }
        if (section.getSectionName() == null || section.getSectionName().isBlank()) {
            throw new BusinessException("Section name is required.");
        }
        Department department = domainValidationService.requireDepartment(section.getDepartment().getId());
        section.setDepartment(department);
        section.setYearLevel(section.getYearLevel().trim());
        section.setSectionName(section.getSectionName().trim());
        return sectionRepository.save(section);
    }

    @Transactional
    public Section update(Long id, Section updated) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Section not found."));
        if (updated.getDepartment() == null || updated.getDepartment().getId() == null) {
            throw new BusinessException("Section must belong to a department.");
        }
        Department department = domainValidationService.requireDepartment(updated.getDepartment().getId());
        section.setSectionName(updated.getSectionName());
        section.setDepartment(department);
        section.setYearLevel(updated.getYearLevel());
        return sectionRepository.save(section);
    }

    @Transactional
    public void delete(Long id) {
        sectionRepository.deleteById(id);
    }

    public List<Section> findByDepartmentId(Long departmentId) {
        return sectionRepository.findByDepartmentIdOrderBySectionNameAsc(departmentId);
    }

    public List<Section> findByDepartmentIdAndYearLevel(Long departmentId, String yearLevel) {
        return sectionRepository.findByDepartmentIdAndYearLevelOrderBySectionNameAsc(departmentId, yearLevel);
    }

    public List<String> findYearLevelsByDepartmentId(Long departmentId) {
        return sectionRepository.findYearLevelsByDepartmentId(departmentId);
    }
}
