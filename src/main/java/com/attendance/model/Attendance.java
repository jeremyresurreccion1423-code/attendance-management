package com.attendance.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    private LocalTime timeIn;

    private LocalTime timeOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AttendanceMethod method = AttendanceMethod.MANUAL;

    private Double latitude;

    private Double longitude;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
