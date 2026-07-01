package com.attendance.repository;

import com.attendance.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentIdOrderBySectionNameAsc(Long departmentId);

    List<Section> findByDepartmentIdAndYearLevelOrderBySectionNameAsc(Long departmentId, String yearLevel);

    @Query("SELECT DISTINCT s.yearLevel FROM Section s WHERE s.department.id = :departmentId AND s.yearLevel IS NOT NULL ORDER BY s.yearLevel")
    List<String> findYearLevelsByDepartmentId(@Param("departmentId") Long departmentId);
}
