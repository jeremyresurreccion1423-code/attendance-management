package com.attendance.controller.api;

import com.attendance.dto.DepartmentDTO;
import com.attendance.dto.PageResponse;
import com.attendance.model.Department;
import com.attendance.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentRestController {

    private final DepartmentService departmentService;

    @GetMapping
    public PageResponse<DepartmentDTO> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(departmentService.findAllDetailed(
                PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/{id}")
    public DepartmentDTO get(@PathVariable Long id) {
        return departmentService.findDetailedById(id);
    }

    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@RequestBody Department department) {
        Department saved = departmentService.save(department);
        return ResponseEntity.ok(departmentService.findDetailedById(saved.getId()));
    }

    @PutMapping("/{id}")
    public DepartmentDTO update(@PathVariable Long id, @RequestBody Department department) {
        departmentService.update(id, department);
        return departmentService.findDetailedById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
