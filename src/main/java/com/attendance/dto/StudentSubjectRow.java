package com.attendance.dto;

public record StudentSubjectRow(
        String code,
        String name,
        String teacher,
        String section,
        String department
) {
}
