package com.attendance.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Builder.Default
    private Double quizScore = 0.0;

    @Builder.Default
    private Double examScore = 0.0;

    @Builder.Default
    private Double assignmentScore = 0.0;

    private Double finalGrade;

    private String remarks;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
