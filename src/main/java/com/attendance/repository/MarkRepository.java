package com.attendance.repository;

import com.attendance.model.Mark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentId(Long studentId);
    List<Mark> findBySubjectId(Long subjectId);
    void deleteBySubjectId(Long subjectId);
    Optional<Mark> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
}
