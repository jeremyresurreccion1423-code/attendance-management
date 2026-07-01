package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LookupDTO {
    private final Long id;
    private final String label;
}
