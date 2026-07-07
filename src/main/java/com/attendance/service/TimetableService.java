package com.attendance.service;

import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceQRRepository attendanceQRRepository;

    public List<Timetable> findAll() {
        return timetableRepository.findAll();
    }

    public List<Timetable> findPublished() {
        return timetableRepository.findByPublishedTrue();
    }

    public List<Timetable> findTodaySchedule() {
        DayOfWeek today = DayOfWeek.valueOf(LocalDate.now().getDayOfWeek().name());
        return timetableRepository.findByDayOfWeekAndPublishedTrue(today);
    }

    public List<Timetable> findByTeacher(Long teacherId) {
        return timetableRepository.findByTeacherId(teacherId);
    }

    public List<Timetable> findByStudent(Long studentId) {
        // Optimized single query - prevents N+1 query problem
        return timetableRepository.findByStudentId(studentId);
    }

    public List<Timetable> findPublishedBySubjectAndDate(Long subjectId, LocalDate date) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        return timetableRepository.findBySubjectIdAndDayOfWeekAndPublishedTrue(subjectId, dayOfWeek)
                .stream()
                .sorted(Comparator.comparing(Timetable::getStartTime))
                .toList();
    }

    @Transactional
    public Timetable save(Timetable timetable) {
        Subject subject = subjectRepository.findById(timetable.getSubject().getId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        timetable.setSubject(subject);
        if (timetable.getTeacher() != null && timetable.getTeacher().getId() != null) {
            Teacher teacher = teacherRepository.findById(timetable.getTeacher().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            timetable.setTeacher(teacher);
        }
        return timetableRepository.save(timetable);
    }

    @Transactional
    public Timetable update(Long id, Timetable updated) {
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        timetable.setDayOfWeek(updated.getDayOfWeek());
        timetable.setStartTime(updated.getStartTime());
        timetable.setEndTime(updated.getEndTime());
        timetable.setRoom(updated.getRoom());
        if (updated.getTeacher() != null && updated.getTeacher().getId() != null) {
            Teacher teacher = teacherRepository.findById(updated.getTeacher().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            timetable.setTeacher(teacher);
        }
        return timetableRepository.save(timetable);
    }

    @Transactional
    public void publish(Long id) {
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        timetable.setPublished(true);
        timetableRepository.save(timetable);
    }

    @Transactional
    public void delete(Long id) {
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Schedule not found."));
        attendanceQRRepository.deleteByTimetable_Id(id);
        timetableRepository.delete(timetable);
    }
}
