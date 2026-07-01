package com.attendance.controller;

import com.attendance.model.*;
import com.attendance.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.attendance.util.ValidationHelper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final SectionService sectionService;
    private final DepartmentService departmentService;
    private final ProfilePhotoService profilePhotoService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        model.addAttribute("data", dashboardService.getAdminDashboard());
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "admin/dashboard";
    }

    @GetMapping("/trends")
    public String trends(Model model, Authentication auth) {
        model.addAttribute("data", dashboardService.getAdminTrendsData());
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "admin/trends";
    }

    @GetMapping("/create")
    public String create(Model model, Authentication auth) {
        Student student = new Student();
        student.setSection(new Section());
        student.setDepartment(new Department());
        student.setStatus(StudentStatus.ACTIVE);

        Teacher teacher = new Teacher();
        teacher.setDepartment(new Department());
        Subject subject = new Subject();
        subject.setTeacher(new Teacher());
        subject.setSection(new Section());
        subject.setDepartment(new Department());
        Section section = new Section();
        section.setDepartment(new Department());

        model.addAttribute("student", student);
        model.addAttribute("teacher", teacher);
        model.addAttribute("subject", subject);
        model.addAttribute("section", section);
        model.addAttribute("department", new Department());
        model.addAttribute("departments", departmentService.findAllNames());
        model.addAttribute("departmentList", departmentService.findAll());
        model.addAttribute("sections", sectionService.findAll());
        model.addAttribute("teachers", teacherService.findAll());
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "admin/create";
    }

    @GetMapping("/departments")
    public String departments(Model model, Authentication auth) {
        var departments = departmentService.findAllDetailed();
        model.addAttribute("departments", departments);
        model.addAttribute("totalTeachers", departments.stream().mapToLong(d -> d.getTeacherCount()).sum());
        model.addAttribute("totalStudents", departments.stream().mapToLong(d -> d.getStudentCount()).sum());
        model.addAttribute("totalSections", departments.stream().mapToLong(d -> d.getSectionCount()).sum());
        model.addAttribute("department", new Department());
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "admin/departments";
    }

    @PostMapping("/departments/{id}/update")
    public String updateDepartment(@PathVariable Long id,
                                   @ModelAttribute Department department,
                                   RedirectAttributes redirect) {
        try {
            departmentService.update(id, department);
            redirect.addFlashAttribute("message", "Department updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            departmentService.delete(id);
            redirect.addFlashAttribute("message", "Department deleted");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments")
    public String addDepartmentFromPage(@ModelAttribute Department department, RedirectAttributes redirect) {
        try {
            ValidationHelper.requireText(department.getName(), "Department name");
            departmentService.save(department);
            redirect.addFlashAttribute("message", "Department created successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/create#department";
    }

    @GetMapping("/students")
    public String students(Model model,
                          Authentication auth,
                          @RequestParam(required = false) Long departmentId,
                          @RequestParam(required = false) String yearLevel,
                          @RequestParam(required = false) Long sectionId,
                          @RequestParam(required = false) Boolean viewAll,
                          @RequestParam(required = false) String search) {
        boolean isViewAll = Boolean.TRUE.equals(viewAll);
        boolean showStudentList = false;
        List<Student> students = List.of();

        if (isViewAll && departmentId == null) {
            students = studentService.findAll();
            showStudentList = true;
        } else if (departmentId != null) {
            if (yearLevel != null && !yearLevel.isBlank()) {
                if (sectionId != null) {
                    students = studentService.findByDepartmentIdAndYearLevelAndSection(departmentId, yearLevel, sectionId);
                    showStudentList = true;
                } else if (isViewAll) {
                    students = studentService.findByDepartmentIdAndYearLevel(departmentId, yearLevel);
                    showStudentList = true;
                }
            } else if (isViewAll) {
                students = studentService.findByDepartmentId(departmentId);
                showStudentList = true;
            }
        }

        students = studentService.filterBySearch(students, search);

        model.addAttribute("students", students);
        model.addAttribute("sections", sectionService.findAll());
        model.addAttribute("departmentList", departmentService.findAll());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYearLevel", yearLevel);
        model.addAttribute("selectedSectionId", sectionId);
        model.addAttribute("viewAll", isViewAll);
        model.addAttribute("showStudentList", showStudentList);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));

        if (departmentId != null) {
            departmentService.findAll().stream()
                    .filter(d -> d.getId().equals(departmentId))
                    .findFirst()
                    .ifPresent(d -> model.addAttribute("selectedDepartmentName", d.getName()));
        }

        if (departmentId != null && yearLevel != null && !yearLevel.isBlank()) {
            model.addAttribute("filteredSections", sectionService.findByDepartmentIdAndYearLevel(departmentId, yearLevel));
        } else {
            model.addAttribute("filteredSections", List.of());
        }

        if (sectionId != null) {
            model.addAttribute("selectedSectionName",
                    sectionService.findById(sectionId).map(Section::getName).orElse(""));
        }

        Student student = new Student();
        student.setSection(new Section());
        student.setStatus(StudentStatus.ACTIVE);
        student.setDepartment(new Department());
        if (departmentId != null) {
            Department dept = new Department();
            dept.setId(departmentId);
            student.setDepartment(dept);
        }
        if (yearLevel != null && !yearLevel.isBlank()) {
            student.setYearLevel(yearLevel);
        }
        model.addAttribute("student", student);
        return "admin/students";
    }

    @PostMapping("/students")
    public String addStudent(@ModelAttribute Student student,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) String password,
                             @RequestParam(required = false) Long returnDepartmentId,
                             @RequestParam(required = false) String returnYearLevel,
                             @RequestParam(required = false) Long returnSectionId,
                             @RequestParam(required = false) Boolean returnViewAll,
                             @RequestParam(required = false) String returnSearch,
                             @RequestParam(required = false) String returnTo,
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
        if ("create".equalsIgnoreCase(returnTo)) {
            return "redirect:/admin/create#student";
        }
        Long deptId = returnDepartmentId != null ? returnDepartmentId
                : (student.getDepartment() != null ? student.getDepartment().getId() : null);
        return buildStudentsRedirect(deptId, returnYearLevel != null ? returnYearLevel : student.getYearLevel(),
                returnSectionId != null ? returnSectionId : (student.getSection() != null ? student.getSection().getId() : null),
                returnViewAll, returnSearch);
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id,
                                @RequestParam(required = false) Long returnDepartmentId,
                                @RequestParam(required = false) String returnYearLevel,
                                @RequestParam(required = false) Long returnSectionId,
                                @RequestParam(required = false) Boolean returnViewAll,
                                @RequestParam(required = false) String returnSearch,
                                RedirectAttributes redirect) {
        studentService.delete(id);
        redirect.addFlashAttribute("message", "Student deleted");
        return buildStudentsRedirect(returnDepartmentId, returnYearLevel, returnSectionId, returnViewAll, returnSearch);
    }

    @PostMapping("/students/{id}/update")
    public String updateStudent(@PathVariable Long id,
                                @ModelAttribute Student student,
                                @RequestParam(required = false) Long returnDepartmentId,
                                @RequestParam(required = false) String returnYearLevel,
                                @RequestParam(required = false) Long returnSectionId,
                                @RequestParam(required = false) Boolean returnViewAll,
                                @RequestParam(required = false) String returnSearch,
                                RedirectAttributes redirect) {
        try {
            ValidationHelper.requireText(student.getFullName(), "Full name");
            ValidationHelper.validateEmail(student.getEmail());
            studentService.update(id, student);
            redirect.addFlashAttribute("message", "Student updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        Long deptId = returnDepartmentId != null ? returnDepartmentId
                : (student.getDepartment() != null ? student.getDepartment().getId() : null);
        return buildStudentsRedirect(deptId, returnYearLevel != null ? returnYearLevel : student.getYearLevel(),
                returnSectionId != null ? returnSectionId : (student.getSection() != null ? student.getSection().getId() : null),
                returnViewAll, returnSearch);
    }

    private String buildStudentsRedirect(Long departmentId,
                                         String yearLevel,
                                         Long sectionId,
                                         Boolean viewAll,
                                         String search) {
        List<String> params = new ArrayList<>();
        if (departmentId != null) {
            params.add("departmentId=" + departmentId);
        }
        if (yearLevel != null && !yearLevel.isBlank()) {
            params.add("yearLevel=" + URLEncoder.encode(yearLevel, StandardCharsets.UTF_8));
        }
        if (sectionId != null) {
            params.add("sectionId=" + sectionId);
        }
        if (Boolean.TRUE.equals(viewAll)) {
            params.add("viewAll=true");
        }
        if (search != null && !search.isBlank()) {
            params.add("search=" + URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
        }
        if (params.isEmpty()) {
            return "redirect:/admin/students";
        }
        return "redirect:/admin/students?" + String.join("&", params);
    }

    @GetMapping("/teachers")
    public String teachers(Model model,
                           Authentication auth,
                           @RequestParam(required = false) Long departmentId) {
        List<Teacher> teachers = departmentId != null
                ? teacherService.findByDepartmentId(departmentId)
                : teacherService.findAll();

        model.addAttribute("teachers", teachers);
        model.addAttribute("departmentList", departmentService.findAll());
        model.addAttribute("selectedDepartmentId", departmentId);
        if (departmentId != null) {
            departmentService.findAll().stream()
                    .filter(d -> d.getId().equals(departmentId))
                    .findFirst()
                    .ifPresent(d -> model.addAttribute("selectedDepartmentName", d.getName()));
        }
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "admin/teachers";
    }

    @PostMapping("/teachers")
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
        return "redirect:/admin/create#teacher";
    }

    @PostMapping("/teachers/{id}/delete")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirect) {
        teacherService.delete(id);
        redirect.addFlashAttribute("message", "Teacher deleted");
        return "redirect:/admin/teachers";
    }

    @PostMapping("/teachers/{id}/update")
    public String updateTeacher(@PathVariable Long id,
                                @ModelAttribute Teacher teacher,
                                RedirectAttributes redirect) {
        try {
            ValidationHelper.requireText(teacher.getFullName(), "Full name");
            ValidationHelper.validateEmail(teacher.getEmail());
            teacherService.update(id, teacher);
            redirect.addFlashAttribute("message", "Teacher updated successfully");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/subjects")
    public String subjects(Model model,
                           Authentication auth,
                           @RequestParam(required = false) Long departmentId) {
        List<Subject> subjects = departmentId != null
                ? subjectService.findByDepartmentId(departmentId)
                : subjectService.findAll();

        model.addAttribute("subjects", subjects);
        model.addAttribute("teachers", teacherService.findAll());
        model.addAttribute("sections", sectionService.findAll());
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("departmentList", departmentService.findAll());
        model.addAttribute("selectedDepartmentId", departmentId);
        if (departmentId != null) {
            departmentService.findAll().stream()
                    .filter(d -> d.getId().equals(departmentId))
                    .findFirst()
                    .ifPresent(d -> model.addAttribute("selectedDepartmentName", d.getName()));
        }
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));

        Subject subject = new Subject();
        subject.setTeacher(new Teacher());
        subject.setSection(new Section());
        subject.setDepartment(new Department());
        model.addAttribute("subject", subject);
        return "admin/subjects";
    }

    @PostMapping("/subjects")
    public String addSubject(@ModelAttribute Subject subject, RedirectAttributes redirect) {
        try {
            subjectService.save(subject);
            redirect.addFlashAttribute("message", "Subject created");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/create#subject";
    }

    @PostMapping("/subjects/{id}/assign-students")
    public String assignStudents(@PathVariable Long id,
                                 @RequestParam(required = false) List<Long> studentIds,
                                 RedirectAttributes redirect) {
        if (studentIds == null || studentIds.isEmpty()) {
            redirect.addFlashAttribute("error", "Please select at least one student");
            return "redirect:/admin/subjects";
        }
        subjectService.assignStudents(id, studentIds);
        redirect.addFlashAttribute("message", "Students assigned");
        return "redirect:/admin/subjects";
    }

    @GetMapping("/sections")
    public String sections(Model model,
                           Authentication auth,
                           @RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) String yearLevel) {
        List<Section> sections;
        if (departmentId != null) {
            if (yearLevel != null && !yearLevel.isEmpty()) {
                sections = sectionService.findByDepartmentIdAndYearLevel(departmentId, yearLevel);
            } else {
                sections = sectionService.findByDepartmentId(departmentId);
            }
        } else {
            sections = sectionService.findAll();
        }

        model.addAttribute("sections", sections);
        model.addAttribute("departmentList", departmentService.findAll());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedYearLevel", yearLevel);
        if (departmentId != null) {
            departmentService.findAll().stream()
                    .filter(d -> d.getId().equals(departmentId))
                    .findFirst()
                    .ifPresent(d -> model.addAttribute("selectedDepartmentName", d.getName()));
        }
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));

        Section section = new Section();
        section.setDepartment(new Department());
        if (departmentId != null) {
            Department dept = new Department();
            dept.setId(departmentId);
            section.setDepartment(dept);
        }
        if (yearLevel != null && !yearLevel.isEmpty()) {
            section.setYearLevel(yearLevel);
        }
        model.addAttribute("section", section);
        return "admin/sections";
    }

    @PostMapping("/sections")
    public String addSection(@ModelAttribute Section section, RedirectAttributes redirect) {
        try {
            sectionService.save(section);
            redirect.addFlashAttribute("message", "Section created");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/create#section";
    }
}
