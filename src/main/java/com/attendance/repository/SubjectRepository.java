package com.attendance.repository;

import com.attendance.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    List<Subject> findByTeacherId(Long teacherId);
    List<Subject> findBySectionId(Long sectionId);
    boolean existsBySubjectCode(String subjectCode);

    List<Subject> findByDepartmentIdOrderBySubjectNameAsc(Long departmentId);
    Page<Subject> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Subject> findAllByOrderBySubjectNameAsc(Pageable pageable);
}
