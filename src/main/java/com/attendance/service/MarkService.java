package com.attendance.service;

import com.attendance.model.Mark;
import com.attendance.model.Student;
import com.attendance.model.Subject;
import com.attendance.repository.MarkRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkService {

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public List<Mark> findByStudent(Long studentId) {
        return markRepository.findByStudentId(studentId);
    }

    public List<Mark> findBySubject(Long subjectId) {
        return markRepository.findBySubjectId(subjectId);
    }

    public Optional<Mark> findByStudentAndSubject(Long studentId, Long subjectId) {
        return markRepository.findByStudentIdAndSubjectId(studentId, subjectId);
    }

    @Transactional
    public Mark saveOrUpdate(Long studentId, Long subjectId, Double quiz, Double exam, Double assignment) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        Mark mark = markRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(Mark.builder().student(student).subject(subject).build());

        if (quiz != null) mark.setQuizScore(quiz);
        if (exam != null) mark.setExamScore(exam);
        if (assignment != null) mark.setAssignmentScore(assignment);
        mark.setFinalGrade(computeGrade(mark));
        mark.setUpdatedAt(LocalDateTime.now());

        return markRepository.save(mark);
    }

    public double computeGrade(Mark mark) {
        // Weighted: Quiz 20%, Assignment 30%, Exam 50%
        double quiz = mark.getQuizScore() != null ? mark.getQuizScore() : 0;
        double assignment = mark.getAssignmentScore() != null ? mark.getAssignmentScore() : 0;
        double exam = mark.getExamScore() != null ? mark.getExamScore() : 0;
        return (quiz * 0.20) + (assignment * 0.30) + (exam * 0.50);
    }

    @Transactional
    public void computeAllGradesForSubject(Long subjectId) {
        markRepository.findBySubjectId(subjectId).forEach(mark -> {
            mark.setFinalGrade(computeGrade(mark));
            mark.setUpdatedAt(LocalDateTime.now());
            markRepository.save(mark);
        });
    }
}
