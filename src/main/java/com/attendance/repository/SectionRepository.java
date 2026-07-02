package com.attendance.repository;

import com.attendance.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentIdOrderBySectionNameAsc(Long departmentId);

    List<Section> findByDepartmentIdAndYearLevelOrderBySectionNameAsc(Long departmentId, String yearLevel);
}
