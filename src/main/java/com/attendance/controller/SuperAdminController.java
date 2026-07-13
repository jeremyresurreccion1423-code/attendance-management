package com.attendance.controller;

import com.attendance.model.Department;
import com.attendance.model.Section;
import com.attendance.model.Student;
import com.attendance.model.StudentStatus;
import com.attendance.model.Teacher;
import com.attendance.service.DepartmentService;
import com.attendance.service.ProfilePhotoService;
import com.attendance.service.StudentService;
import com.attendance.service.SuperAdminDashboardService;
import com.attendance.service.TeacherService;
import com.attendance.util.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Centralized Super Admin dashboard. Provides a single hub of links into every
 * admin-only feature of the Attendance Management System (native, same app) and
 * every admin-only feature of the Library Management System (via SSO bridge links).
 * Also hosts a dedicated Create dashboard for departments, students, and teachers.
 */
@Controller
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminDashboardService superAdminDashboardService;
    private final DepartmentService departmentService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ProfilePhotoService profilePhotoService;

    @GetMapping("/super-admin")
    public String dashboard(Model model) {
        var combined = superAdminDashboardService.getCombinedDashboard();
        model.addAttribute("attendance", combined.attendance());
        model.addAttribute("library", combined.library());
        model.addAttribute("libraryAvailable", combined.libraryAvailable());
        model.addAttribute("lowAttendanceAlerts", combined.lowAttendanceAlerts());
        return "super-admin/dashboard";
    }

    @GetMapping("/super-admin/create")
    public String create(Model model, Authentication auth) {
        Student student = new Student();
        student.setSection(new Section());
        student.setDepartment(new Department());
        student.setStatus(StudentStatus.ACTIVE);

        Teacher teacher = new Teacher();
        teacher.setDepartment(new Department());

        var departmentList = departmentService.findAll();
        model.addAttribute("student", student);
        model.addAttribute("teacher", teacher);
        model.addAttribute("departmentList", departmentList);
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "super-admin/create";
    }

    @PostMapping("/super-admin/students")
    public String addStudent(@ModelAttribute Student student,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) String password,
                             RedirectAttributes redirect) {
        try {
            ValidationHelper.requireText(student.getFullName(), "Full name");
            ValidationHelper.requireText(student.getStudentNumber(), "Student number");
            ValidationHelper.validateEmail(student.getEmail());
            if (password != null && !password.isBlank()) {
                ValidationHelper.validatePassword(password);
            }
            studentService.save(student, username, password);
            redirect.addFlashAttribute("message", "Student added successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/create#student";
    }

    @PostMapping("/super-admin/teachers")
    public String addTeacher(@ModelAttribute Teacher teacher,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) String password,
                             RedirectAttributes redirect) {
        try {
            ValidationHelper.requireText(teacher.getFullName(), "Full name");
            ValidationHelper.requireText(teacher.getEmployeeId(), "Employee ID");
            ValidationHelper.validateEmail(teacher.getEmail());
            if (password != null && !password.isBlank()) {
                ValidationHelper.validatePassword(password);
            }
            teacherService.save(teacher, username, password);
            redirect.addFlashAttribute("message", "Teacher added successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/create#teacher";
    }
}
