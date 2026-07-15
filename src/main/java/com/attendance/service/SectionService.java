package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.Department;
import com.attendance.model.Section;
import com.attendance.repository.SectionRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SectionService {

    private static final List<String> DEFAULT_YEAR_LEVELS = List.of(
            "1st Year", "2nd Year", "3rd Year", "4th Year");

    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final DomainValidationService domainValidationService;

    public List<Section> findAll() {
        return sectionRepository.findAll();
    }

    public Optional<Section> findById(Long id) {
        return sectionRepository.findById(id);
    }

    public List<String> findYearLevelsByDepartmentId(Long departmentId) {
        domainValidationService.requireDepartment(departmentId);
        List<String> fromDb = sectionRepository.findDistinctYearLevelsByDepartmentId(departmentId);
        if (fromDb.isEmpty()) {
            return DEFAULT_YEAR_LEVELS;
        }
        List<String> merged = new ArrayList<>(fromDb);
        for (String level : DEFAULT_YEAR_LEVELS) {
            if (!merged.contains(level)) {
                merged.add(level);
            }
        }
        return merged;
    }

    @Transactional
    public Section save(Section section) {
        validateSection(section, null);
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
        validateSection(updated, id);
        Department department = domainValidationService.requireDepartment(updated.getDepartment().getId());
        section.setSectionName(updated.getSectionName().trim());
        section.setDepartment(department);
        section.setYearLevel(updated.getYearLevel().trim());
        return sectionRepository.save(section);
    }

    @Transactional
    public void delete(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Section not found."));
        long linkedStudents = studentRepository.findBySectionId(id).size();
        long linkedSubjects = subjectRepository.findBySectionId(id).size();
        if (linkedStudents > 0 || linkedSubjects > 0) {
            throw new BusinessException("Cannot delete section with linked students or subjects.");
        }
        sectionRepository.delete(section);
    }

    public List<Section> findByDepartmentId(Long departmentId) {
        return sectionRepository.findByDepartmentIdOrderBySectionNameAsc(departmentId);
    }

    public List<Section> findByDepartmentIdAndYearLevel(Long departmentId, String yearLevel) {
        return sectionRepository.findByDepartmentIdAndYearLevelOrderBySectionNameAsc(departmentId, yearLevel);
    }

    private void validateSection(Section section, Long currentId) {
        if (section.getDepartment() == null || section.getDepartment().getId() == null) {
            throw new BusinessException("Department is required.");
        }
        if (section.getYearLevel() == null || section.getYearLevel().isBlank()) {
            throw new BusinessException("Year level is required.");
        }
        if (section.getSectionName() == null || section.getSectionName().isBlank()) {
            throw new BusinessException("Section name is required.");
        }
        Long departmentId = section.getDepartment().getId();
        String yearLevel = section.getYearLevel().trim();
        String name = section.getSectionName().trim();
        boolean duplicate = currentId == null
                ? sectionRepository.existsByDepartmentIdAndYearLevelIgnoreCaseAndSectionNameIgnoreCase(
                        departmentId, yearLevel, name)
                : sectionRepository.existsByDepartmentIdAndYearLevelIgnoreCaseAndSectionNameIgnoreCaseAndIdNot(
                        departmentId, yearLevel, name, currentId);
        if (duplicate) {
            throw new BusinessException("A section with this name already exists for the selected department and year level.");
        }
    }
}
