package com.lms.mentoring.notification;

import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.model.Course;
import com.lms.mentoring.course.service.CourseService;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Scheduled job that runs daily to notify students about courses starting tomorrow.
 */
@Component
public class CourseStartNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CourseStartNotificationScheduler.class);

    private final CourseService courseService;
    private final EmailService emailService;
    private final Executor emailExecutor;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;

    public CourseStartNotificationScheduler(CourseService courseService,
                                            EmailService emailService,
                                            @Qualifier("emailNotificationExecutor") Executor emailExecutor,
                                            CourseMapper courseMapper,
                                            StudentMapper studentMapper) {
        this.courseService = courseService;
        this.emailService = emailService;
        this.emailExecutor = emailExecutor;
        this.courseMapper = courseMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * Runs daily at 07:00 Europe/Berlin time.
     * Finds courses starting the next day and sends emails to all enrolled students.
     */
//    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Berlin")
    @Scheduled(fixedRate = 30000) // Runs every 30 seconds (30000 milliseconds)
    @Transactional(readOnly = true)
    public void notifyCoursesStartingTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfDay = LocalDateTime.of(tomorrow, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(tomorrow, LocalTime.MAX);

        List<Course> startingCourses = courseService.findStartingBetween(startOfDay, endOfDay);
        if (startingCourses == null || startingCourses.isEmpty()) {
            log.info("No courses starting tomorrow ({}). Skipping notification job.", tomorrow);
            return;
        }

        log.info("Found {} course(s) starting on {}. Sending notifications...", startingCourses.size(), tomorrow);
        for (Course course : startingCourses) {
            if (course.getStudents() == null || course.getStudents().isEmpty()) {
                log.info("Course '{}' has no enrolled students. Skipping.", course.getTitle());
                continue;
            }
            for (Student student : course.getStudents()) {
                emailExecutor.execute(() -> emailService.sendCourseStartingNotification(studentMapper.toDto(student), courseMapper.toDto(course)));
            }
        }
        log.info("Finished scheduling email notifications for courses starting on {}", tomorrow);
    }
}
