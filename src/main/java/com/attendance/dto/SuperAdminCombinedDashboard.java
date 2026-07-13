package com.attendance.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record SuperAdminCombinedDashboard(
        Map<String, Object> attendance,
        Map<String, Object> library,
        boolean libraryAvailable
) {
    @SuppressWarnings("unchecked")
    public static SuperAdminCombinedDashboard of(Map<String, Object> attendance, Map<String, Object> library, boolean libraryAvailable) {
        return new SuperAdminCombinedDashboard(attendance, library, libraryAvailable);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> lowAttendanceAlerts() {
        Object alerts = attendance.get("lowAttendanceAlerts");
        if (alerts instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }
}
