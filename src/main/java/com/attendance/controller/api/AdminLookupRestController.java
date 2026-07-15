package com.attendance.controller.api;

import com.attendance.dto.LookupDTO;
import com.attendance.model.Section;
import com.attendance.service.DepartmentService;
import com.attendance.service.SectionService;
import com.attendance.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/lookups")
@RequiredArgsConstructor
public class AdminLookupRestController {

    private final DepartmentService departmentService;
    private final TeacherService teacherService;
    private final SectionService sectionService;

    @GetMapping("/departments")
    public List<LookupDTO> departments() {
        return departmentService.findAll().stream()
                .map(d -> new LookupDTO(d.getId(), d.getName()))
                .toList();
    }

    @GetMapping("/year-levels")
    public List<String> yearLevels(@RequestParam Long departmentId) {
        return sectionService.findYearLevelsByDepartmentId(departmentId);
    }

    @GetMapping("/teachers")
    public List<LookupDTO> teachers(@RequestParam Long departmentId) {
        return teacherService.findByDepartmentId(departmentId).stream()
                .map(t -> new LookupDTO(t.getId(), t.getFullName()))
                .toList();
    }

    @GetMapping("/sections")
    public List<LookupDTO> sections(@RequestParam Long departmentId,
                                    @RequestParam(required = false) String yearLevel) {
        List<Section> sections = (yearLevel == null || yearLevel.isBlank())
                ? sectionService.findByDepartmentId(departmentId)
                : sectionService.findByDepartmentIdAndYearLevel(departmentId, yearLevel);
        return sections.stream()
                .map(s -> new LookupDTO(s.getId(), s.getSectionName() + " (" + s.getYearLevel() + ")"))
                .toList();
    }
}
