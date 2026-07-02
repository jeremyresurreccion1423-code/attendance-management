package com.attendance.controller;

import com.attendance.model.Student;
import com.attendance.model.User;
import com.attendance.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final MarkService markService;
    private final TimetableService timetableService;
    private final ProfilePhotoService profilePhotoService;
    private final SharedAttendanceStudentProfileBridgeService sharedAttendanceStudentProfileBridgeService;

    private Student getCurrentStudent(Authentication auth) {
        User user = authService.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
        return sharedAttendanceStudentProfileBridgeService.ensureAttendanceStudent(user)
                .orElseThrow(() -> new IllegalStateException("Student record not found for user: " + auth.getName()));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Student student = getCurrentStudent(auth);
        User user = student.getUser();
        model.addAttribute("data", dashboardService.getStudentDashboard(student.getId(), user.getId()));
        model.addAttribute("student", student);
        model.addAttribute("notifications", notificationService.findByUser(user.getId()));
        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "student/dashboard";
    }

    @GetMapping("/trends")
    public String trends(Authentication auth, Model model) {
        Student student = getCurrentStudent(auth);
        User user = student.getUser();
        model.addAttribute("data", dashboardService.getStudentTrendsData(student.getId(), user.getId()));
        model.addAttribute("student", student);
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "student/trends";
    }

    @GetMapping("/attendance")
    public String attendanceHistory(Authentication auth, Model model) {
        Student student = getCurrentStudent(auth);
        model.addAttribute("attendance", attendanceService.findByStudent(student.getId()));
        model.addAttribute("percentage", attendanceService.getAttendancePercentage(student.getId()));
        return "student/attendance";
    }

    @GetMapping("/scan")
    public String scanPage(Authentication auth, Model model) {
        model.addAttribute("student", getCurrentStudent(auth));
        return "student/scan";
    }

    @PostMapping("/scan")
    public String scanQR(@RequestParam String qrCode,
                         @RequestParam(required = false) Double latitude,
                         @RequestParam(required = false) Double longitude,
                         Authentication auth,
                         RedirectAttributes redirect) {
        try {
            Student student = getCurrentStudent(auth);
            attendanceService.recordQR(student.getId(), qrCode, latitude, longitude);
            redirect.addFlashAttribute("message", "Attendance recorded successfully!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/scan";
    }

    @GetMapping("/grades")
    public String grades(Authentication auth, Model model) {
        Student student = getCurrentStudent(auth);
        model.addAttribute("marks", markService.findByStudent(student.getId()));
        return "student/grades";
    }

    @GetMapping("/schedule")
    public String schedule(Authentication auth, Model model) {
        Student student = getCurrentStudent(auth);
        model.addAttribute("schedules", timetableService.findByStudent(student.getId()));
        return "student/schedule";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/student/dashboard";
    }
}
