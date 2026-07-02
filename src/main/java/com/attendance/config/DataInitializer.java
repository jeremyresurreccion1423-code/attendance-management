package com.attendance.config;

import com.attendance.model.*;
import com.attendance.repository.*;
import com.attendance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TimetableRepository timetableRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) {
        User adminUser = userRepository.findByUsername("admin")
                .orElseGet(() -> authService.createUser(
                        "admin", "admin123", Role.ADMIN, "edulibrary67+admin@gmail.com", "System Admin"));

        User teacherUser = userRepository.findByUsername("teacher1")
                .orElseGet(() -> authService.createUser(
                        "teacher1", "teacher123", Role.TEACHER, "maria.santos@school.edu", "Dr. Maria Santos"));

        User studentUser = userRepository.findByUsername("student1")
                .orElseGet(() -> authService.createUser(
                        "student1", "student123", Role.STUDENT, "juan.delacruz@student.edu", "Juan Dela Cruz"));

        Department bsit = departmentRepository.findByNameIgnoreCase("BSIT")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name("BSIT")
                        .description("BS Information Technology")
                        .build()));

        if (teacherRepository.findByUserId(teacherUser.getId()).isEmpty()) {
            Teacher teacher = Teacher.builder()
                    .user(teacherUser)
                    .employeeId("EMP001")
                    .fullName("Dr. Maria Santos")
                    .department(bsit)
                    .contactNumber("09171234567")
                    .email("maria.santos@school.edu")
                    .status(TeacherStatus.ACTIVE)
                    .build();
            teacherRepository.save(teacher);
        }

        Section section = sectionRepository.findAll().stream()
                .filter(s -> "BSIT-3A".equals(s.getName()))
                .findFirst()
                .orElseGet(() -> sectionRepository.save(Section.builder()
                        .sectionName("BSIT-3A")
                        .department(bsit)
                        .yearLevel("3rd Year")
                        .build()));

        if (studentRepository.findByUserId(studentUser.getId()).isEmpty()) {
            Student student = Student.builder()
                    .user(studentUser)
                    .studentNumber("2021-0001")
                    .fullName("Juan Dela Cruz")
                    .department(bsit)
                    .yearLevel("3rd Year")
                    .section(section)
                    .contactNumber("09181234567")
                    .email("juan.delacruz@student.edu")
                    .status(StudentStatus.ACTIVE)
                    .build();
            studentRepository.save(student);
        }

        Teacher teacher = teacherRepository.findByUserId(teacherUser.getId()).orElse(null);
        Student student = studentRepository.findByUserId(studentUser.getId()).orElse(null);

        if (teacher != null && student != null) {
            Subject subject = subjectRepository.findAll().stream()
                    .filter(s -> "CS301".equals(s.getSubjectCode()))
                    .findFirst()
                    .orElseGet(() -> subjectRepository.save(Subject.builder()
                            .subjectCode("CS301")
                            .subjectName("Database Systems")
                            .department(bsit)
                            .teacher(teacher)
                            .section(section)
                            .description("Introduction to database design and SQL")
                            .build()));

            if (subject.getTeacher() == null) {
                subject.setTeacher(teacher);
                subjectRepository.save(subject);
            }
            if (subject.getDepartment() == null) {
                subject.setDepartment(bsit);
                subjectRepository.save(subject);
            }
            if (subject.getSection() == null) {
                subject.setSection(section);
                subjectRepository.save(subject);
            }

            if (enrollmentRepository.findByStudentIdAndSubjectId(student.getId(), subject.getId()).isEmpty()) {
                enrollmentRepository.save(Enrollment.builder().student(student).subject(subject).build());
            }

            if (timetableRepository.findAll().stream()
                    .noneMatch(t -> t.getSubject().getId().equals(subject.getId())
                            && t.getDayOfWeek() == DayOfWeek.MONDAY)) {
                timetableRepository.save(Timetable.builder()
                        .subject(subject)
                        .teacher(teacher)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(10, 0))
                        .room("Room 301")
                        .published(true)
                        .build());
            }

            if (timetableRepository.findAll().stream()
                    .noneMatch(t -> t.getSubject().getId().equals(subject.getId())
                            && t.getDayOfWeek() == DayOfWeek.WEDNESDAY)) {
                timetableRepository.save(Timetable.builder()
                        .subject(subject)
                        .teacher(teacher)
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .startTime(LocalTime.of(13, 0))
                        .endTime(LocalTime.of(15, 0))
                        .room("Lab 2")
                        .published(true)
                        .build());
            }
        }

        System.out.println("=== Sample data initialized/verified ===");
        System.out.println("Admin:    admin / admin123");
        System.out.println("Teacher:  teacher1 / teacher123");
        System.out.println("Student:  student1 / student123");
    }
}
