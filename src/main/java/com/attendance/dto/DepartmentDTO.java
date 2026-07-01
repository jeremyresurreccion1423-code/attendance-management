package com.attendance.dto;

import com.attendance.model.Department;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DepartmentDTO {
    private final Long id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final long teacherCount;
    private final long sectionCount;
    private final long studentCount;
    private final long subjectCount;

    public static DepartmentDTO from(Department department) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .build();
    }

    public static DepartmentDTO from(Department department,
                                     long teacherCount,
                                     long sectionCount,
                                     long studentCount,
                                     long subjectCount) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .teacherCount(teacherCount)
                .sectionCount(sectionCount)
                .studentCount(studentCount)
                .subjectCount(subjectCount)
                .build();
    }
}
