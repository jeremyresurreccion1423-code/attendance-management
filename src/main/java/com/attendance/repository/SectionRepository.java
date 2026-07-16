package com.attendance.repository;

import com.attendance.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentIdOrderBySectionNameAsc(Long departmentId);

    List<Section> findByDepartmentIdAndYearLevelOrderBySectionNameAsc(Long departmentId, String yearLevel);

    List<Section> findByDepartmentIdAndYearLevelIgnoreCaseOrderBySectionNameAsc(Long departmentId, String yearLevel);

    boolean existsByDepartmentIdAndYearLevelIgnoreCaseAndSectionNameIgnoreCase(
            Long departmentId, String yearLevel, String sectionName);

    boolean existsByDepartmentIdAndYearLevelIgnoreCaseAndSectionNameIgnoreCaseAndIdNot(
            Long departmentId, String yearLevel, String sectionName, Long id);

    @Query("SELECT DISTINCT s.yearLevel FROM Section s WHERE s.department.id = :departmentId ORDER BY s.yearLevel")
    List<String> findDistinctYearLevelsByDepartmentId(Long departmentId);
}
