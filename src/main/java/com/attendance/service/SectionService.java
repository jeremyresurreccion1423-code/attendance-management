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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        Set<String> available = new LinkedHashSet<>();
        for (String raw : sectionRepository.findDistinctYearLevelsByDepartmentId(departmentId)) {
            String normalized = normalizeYearLevel(raw);
            if (normalized != null) {
                available.add(normalized);
            }
        }
        available.addAll(DEFAULT_YEAR_LEVELS);
        if (available.isEmpty()) {
            return DEFAULT_YEAR_LEVELS;
        }

        List<String> ordered = new ArrayList<>();
        for (String level : DEFAULT_YEAR_LEVELS) {
            if (available.remove(level)) {
                ordered.add(level);
            }
        }
        available.stream()
                .filter(this::isValidYearLevel)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(ordered::add);
        return ordered;
    }

    private String normalizeYearLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim();
        int comma = cleaned.indexOf(',');
        if (comma >= 0) {
            cleaned = cleaned.substring(0, comma).trim();
        }
        int mark = cleaned.indexOf('§');
        if (mark >= 0) {
            cleaned = cleaned.substring(0, mark).trim();
        }
        int amp = cleaned.toLowerCase().indexOf("&section");
        if (amp >= 0) {
            cleaned = cleaned.substring(0, amp).trim();
        }
        int ion = cleaned.toLowerCase().indexOf("ionid=");
        if (ion >= 0) {
            cleaned = cleaned.substring(0, ion).trim();
        }
        if (cleaned.isBlank()) {
            return null;
        }
        for (String level : DEFAULT_YEAR_LEVELS) {
            if (level.equalsIgnoreCase(cleaned)) {
                return level;
            }
        }
        return isValidYearLevel(cleaned) ? cleaned : null;
    }

    private boolean isValidYearLevel(String level) {
        return level != null && level.matches("(?i)\\d+(st|nd|rd|th)\\s+Year");
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
        String normalized = normalizeYearLevel(yearLevel);
        if (normalized == null) {
            return List.of();
        }
        List<Section> matched = sectionRepository
                .findByDepartmentIdAndYearLevelIgnoreCaseOrderBySectionNameAsc(departmentId, normalized);
        if (!matched.isEmpty()) {
            return matched;
        }
        // Fallback for legacy/corrupted year_level values (e.g. "2nd Year§ionId=2")
        return sectionRepository.findByDepartmentIdOrderBySectionNameAsc(departmentId).stream()
                .filter(s -> normalized.equalsIgnoreCase(normalizeYearLevel(s.getYearLevel())))
                .toList();
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
