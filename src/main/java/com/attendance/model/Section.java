package com.attendance.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String sectionName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 20)
    private String yearLevel;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getName() {
        return sectionName;
    }

    public void setName(String name) {
        this.sectionName = name;
    }

    /** @deprecated legacy column kept for migration only */
    @Deprecated
    @Column(name = "course", insertable = false, updatable = false)
    private String course;
}
