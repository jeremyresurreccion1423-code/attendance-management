package com.attendance.service;



import com.attendance.model.*;

import com.attendance.repository.*;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;



import java.time.LocalDate;

import java.time.format.DateTimeFormatter;

import java.util.*;

import java.util.stream.Collectors;



@Service

@RequiredArgsConstructor

public class DashboardService {



    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final DateTimeFormatter CHART_DATE = DateTimeFormatter.ofPattern("MMM d");

    private static final double LOW_ATTENDANCE_THRESHOLD = 75.0;



    private final StudentRepository studentRepository;

    private final SubjectRepository subjectRepository;

    private final AttendanceRepository attendanceRepository;

    private final EnrollmentRepository enrollmentRepository;

    private final MarkRepository markRepository;

    private final TimetableRepository timetableRepository;

    private final AttendanceService attendanceService;

    private final TimetableService timetableService;

    private final NotificationService notificationService;



    public Map<String, Object> getTeacherDashboard(Long teacherId) {

        Map<String, Object> data = new HashMap<>();

        List<Subject> subjects = subjectRepository.findByTeacherId(teacherId);

        data.put("totalClasses", subjects.size());



        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();



        Map<Long, Long> enrollmentCounts = new HashMap<>();

        for (Object[] row : enrollmentRepository.countEnrollmentsBySubjectIds(subjectIds)) {

            enrollmentCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());

        }

        long totalStudents = enrollmentCounts.values().stream().mapToLong(Long::longValue).sum();

        data.put("totalStudents", totalStudents);



        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.minusDays(6);

        LocalDate monthStart = today.withDayOfMonth(1);



        Map<String, Long> todayStats = initStatusMap();

        List<Attendance> todayAttendance = new ArrayList<>();

        try {

            todayAttendance = attendanceRepository.findBySubjectIdsAndDate(subjectIds, today);

            for (Attendance a : todayAttendance) {

                todayStats.merge(a.getStatus().name(), 1L, Long::sum);

            }

        } catch (RuntimeException ex) {

            log.warn("Unable to load teacher attendance rows for today; continuing with empty stats", ex);

        }

        data.put("attendanceToday", todayStats);

        data.put("attendanceWeekly", buildAttendanceTrend(subjects, weekStart, today));

        data.put("attendanceMonthly", buildAttendanceTrend(subjects, monthStart, today));



        data.put("dailyTrendChart", buildDailyTrendChart(subjectIds, weekStart, today));

        data.put("monthlyTrendChart", buildDailyTrendChart(subjectIds, monthStart, today));



        List<Map<String, Object>> lowAttendance = buildLowAttendanceAlerts(subjects);

        data.put("lowAttendanceAlerts", lowAttendance);



        List<Map<String, Object>> subjectPerformance = new ArrayList<>();

        for (Subject subject : subjects) {

            Map<String, Object> item = new HashMap<>();

            item.put("subject", subject.getSubjectName());

            long enrolled = enrollmentCounts.getOrDefault(subject.getId(), 0L);

            item.put("enrolled", enrolled);

            long present = todayAttendance.stream()

                    .filter(a -> a.getSubject().getId().equals(subject.getId()))

                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)

                    .count();

            item.put("present", present);

            item.put("attendanceRate", enrolled == 0 ? 0.0 : Math.round((present * 100.0 / enrolled) * 10.0) / 10.0);

            subjectPerformance.add(item);

        }

        data.put("subjectPerformance", subjectPerformance);

        data.put("performanceChart", buildPerformanceChart(subjectPerformance));



        DayOfWeek todayDay = DayOfWeek.valueOf(LocalDate.now().getDayOfWeek().name());

        data.put("upcomingClasses", timetableRepository.findByTeacherId(teacherId).stream()

                .filter(t -> t.getDayOfWeek() == todayDay)

                .collect(Collectors.toList()));



        return data;

    }



    public Map<String, Object> getStudentDashboard(Long studentId, Long userId) {

        Map<String, Object> data = new HashMap<>();

        Student student = studentRepository.findById(studentId).orElse(null);

        double attendancePercentage = attendanceService.getAttendancePercentage(studentId);

        DayOfWeek today = DayOfWeek.valueOf(LocalDate.now().getDayOfWeek().name());



        data.put("attendancePercentage", String.format("%.1f", attendancePercentage));

        List<Mark> grades = markRepository.findByStudentId(studentId);

        data.put("grades", grades);

        data.put("performanceChart", buildStudentGradeChart(grades));

        data.put("todaySchedule", timetableService.findByStudent(studentId).stream()

                .filter(t -> t.getDayOfWeek() == today)

                .collect(Collectors.toList()));



        List<Attendance> studentAttendance = new ArrayList<>();

        try {

            studentAttendance = attendanceRepository.findByStudentId(studentId);

        } catch (RuntimeException ex) {

            log.warn("Unable to load student attendance rows; continuing with empty history", ex);

        }

        data.put("attendanceHistory", studentAttendance.stream()

                .sorted(Comparator.comparing(Attendance::getAttendanceDate).reversed())

                .limit(10)

                .collect(Collectors.toList()));

        long total = studentAttendance.size();

        long present = studentAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE).count();

        long absent = studentAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

        data.put("attendanceStats", Map.of(

                "total", total,

                "present", present,

                "absent", absent,

                "percentage", Math.round(attendancePercentage * 10.0) / 10.0

        ));



        LocalDate todayDate = LocalDate.now();

        data.put("dailyTrendChart", buildStudentDailyTrend(studentId, todayDate.minusDays(6), todayDate));

        data.put("monthlyTrendChart", buildStudentDailyTrend(studentId, todayDate.withDayOfMonth(1), todayDate));



        data.put("lowAttendanceAlert", attendancePercentage > 0 && attendancePercentage < LOW_ATTENDANCE_THRESHOLD);

        if (student != null && student.getUser() != null && attendancePercentage > 0 && attendancePercentage < LOW_ATTENDANCE_THRESHOLD) {

            notificationService.createIfAbsent(student.getUser(), "Low Attendance Alert",

                    String.format("Your overall attendance is %.1f%%. Please improve to meet the 75%% requirement.", attendancePercentage), 7);

        }



        return data;

    }



    public Map<String, Object> getAdminDashboard() {

        Map<String, Object> data = new HashMap<>();

        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.minusDays(6);

        LocalDate monthStart = today.withDayOfMonth(1);



        data.put("totalStudents", studentRepository.count());

        data.put("totalSubjects", subjectRepository.count());

        data.put("todayAttendance", attendanceRepository.countByAttendanceDateBetween(today, today));

        data.put("weeklyAttendance", attendanceRepository.countByAttendanceDateBetween(weekStart, today));

        data.put("monthlyAttendance", attendanceRepository.countByAttendanceDateBetween(monthStart, today));



        data.put("dailyTrendChart", buildAdminDailyTrend(weekStart, today));

        data.put("monthlyTrendChart", buildAdminDailyTrend(monthStart, today));



        List<Map<String, Object>> lowAttendance = new ArrayList<>();

        for (Student student : studentRepository.findAll()) {

            double pct = attendanceService.getAttendancePercentage(student.getId());

            if (pct > 0 && pct < LOW_ATTENDANCE_THRESHOLD) {

                Map<String, Object> alert = new HashMap<>();

                alert.put("studentName", student.getFullName());

                alert.put("studentNumber", student.getStudentNumber());

                alert.put("percentage", String.format("%.1f", pct));

                lowAttendance.add(alert);

            }

        }

        data.put("lowAttendanceAlerts", lowAttendance);

        data.put("lowAttendanceCount", lowAttendance.size());



        return data;
    }

    public Map<String, Object> getAdminTrendsData() {
        return getAdminDashboard();
    }

    public Map<String, Object> getTeacherTrendsData(Long teacherId) {
        return getTeacherDashboard(teacherId);
    }

    public Map<String, Object> getStudentTrendsData(Long studentId, Long userId) {
        return getStudentDashboard(studentId, userId);
    }

    private List<Map<String, Object>> buildLowAttendanceAlerts(List<Subject> subjects) {

        List<Map<String, Object>> lowAttendance = new ArrayList<>();

        for (Subject subject : subjects) {

            List<Enrollment> enrollments = enrollmentRepository.findBySubjectId(subject.getId());

            for (Enrollment e : enrollments) {

                double pct = attendanceService.getAttendancePercentage(e.getStudent().getId());

                if (pct < LOW_ATTENDANCE_THRESHOLD && pct > 0) {

                    Map<String, Object> alert = new HashMap<>();

                    alert.put("studentName", e.getStudent().getFullName());

                    alert.put("subject", subject.getSubjectName());

                    alert.put("percentage", String.format("%.1f", pct));

                    lowAttendance.add(alert);

                    if (e.getStudent().getUser() != null) {

                        notificationService.createIfAbsent(e.getStudent().getUser(), "Low Attendance Alert",

                                String.format("Your attendance in %s is %.1f%%. Please improve to meet the 75%% requirement.",

                                        subject.getSubjectName(), pct), 7);

                    }

                }

            }

        }

        return lowAttendance;

    }



    private Map<String, Object> buildDailyTrendChart(List<Long> subjectIds, LocalDate start, LocalDate end) {

        List<String> labels = new ArrayList<>();

        List<Long> present = new ArrayList<>();

        List<Long> absent = new ArrayList<>();

        List<Long> late = new ArrayList<>();



        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            labels.add(date.format(CHART_DATE));

            long p = 0, a = 0, l = 0;

            if (!subjectIds.isEmpty()) {

                try {

                    for (Attendance att : attendanceRepository.findBySubjectIdsAndDate(subjectIds, date)) {

                        switch (att.getStatus()) {

                            case PRESENT -> p++;

                            case ABSENT -> a++;

                            case LATE -> l++;

                            default -> {}

                        }

                    }

                } catch (RuntimeException ex) {

                    log.warn("Unable to load trend for {}", date, ex);

                }

            }

            present.add(p);

            absent.add(a);

            late.add(l);

        }

        return Map.of("labels", labels, "present", present, "absent", absent, "late", late);

    }



    private Map<String, Object> buildStudentDailyTrend(Long studentId, LocalDate start, LocalDate end) {

        List<String> labels = new ArrayList<>();

        List<Long> present = new ArrayList<>();

        List<Long> absent = new ArrayList<>();



        List<Attendance> records = attendanceRepository.findByStudentId(studentId);

        Map<LocalDate, AttendanceStatus> byDate = records.stream()

                .collect(Collectors.toMap(Attendance::getAttendanceDate, Attendance::getStatus, (a, b) -> b));



        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            labels.add(date.format(CHART_DATE));

            AttendanceStatus status = byDate.get(date);

            if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE) {

                present.add(1L);

                absent.add(0L);

            } else if (status == AttendanceStatus.ABSENT) {

                present.add(0L);

                absent.add(1L);

            } else {

                present.add(0L);

                absent.add(0L);

            }

        }

        return Map.of("labels", labels, "present", present, "absent", absent);

    }



    private Map<String, Object> buildAdminDailyTrend(LocalDate start, LocalDate end) {

        List<String> labels = new ArrayList<>();

        List<Long> totals = new ArrayList<>();

        Map<LocalDate, Long> byDate = new HashMap<>();

        for (Object[] row : attendanceRepository.countGroupedByAttendanceDate(start, end)) {

            byDate.put((LocalDate) row[0], ((Number) row[1]).longValue());

        }



        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            labels.add(date.format(CHART_DATE));

            totals.add(byDate.getOrDefault(date, 0L));

        }

        return Map.of("labels", labels, "totals", totals);

    }



    private Map<String, Object> buildPerformanceChart(List<Map<String, Object>> subjectPerformance) {

        List<String> labels = subjectPerformance.stream()

                .map(m -> (String) m.get("subject"))

                .collect(Collectors.toList());

        List<Double> rates = subjectPerformance.stream()

                .map(m -> ((Number) m.get("attendanceRate")).doubleValue())

                .collect(Collectors.toList());

        return Map.of("labels", labels, "rates", rates);

    }



    private Map<String, Object> buildStudentGradeChart(List<Mark> grades) {

        List<String> labels = grades.stream()

                .map(m -> m.getSubject().getSubjectName())

                .collect(Collectors.toList());

        List<Double> values = grades.stream()

                .map(m -> m.getFinalGrade() != null ? m.getFinalGrade() : 0.0)

                .collect(Collectors.toList());

        return Map.of("labels", labels, "grades", values);

    }



    private Map<String, Long> buildAttendanceTrend(List<Subject> subjects, LocalDate start, LocalDate end) {

        Map<String, Long> stats = initStatusMap();

        for (Subject subject : subjects) {

            try {

                attendanceRepository.findBySubjectIdAndAttendanceDateBetween(subject.getId(), start, end)

                        .forEach(a -> stats.merge(a.getStatus().name(), 1L, Long::sum));

            } catch (RuntimeException ex) {

                log.warn("Unable to load attendance trend for subject {}", subject.getId(), ex);

            }

        }

        return stats;

    }



    private Map<String, Long> initStatusMap() {

        Map<String, Long> stats = new LinkedHashMap<>();

        for (AttendanceStatus status : AttendanceStatus.values()) {

            stats.put(status.name(), 0L);

        }

        return stats;

    }

}


