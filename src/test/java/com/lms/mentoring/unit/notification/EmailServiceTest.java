package com.lms.mentoring.unit.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.notification.EmailService;
import com.lms.mentoring.notification.EmailTemplateService;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.util.TestDataGenerator;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailTemplateService templateService;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateService = mock(EmailTemplateService.class);
        emailService = new EmailService(mailSender, templateService, "no-reply@example.com");
    }

    @Test
    void sendCourseStartingNotification_WhenValidStudentAndCourse_ShouldSendEmail() {
        // given
        StudentDto student = TestDataGenerator.createDefaultStudentDto();
        CourseDto course = TestDataGenerator.createDefaultCourseDto();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getCourseStartNotificationSubject(any(CourseDto.class), eq("en")))
                .thenReturn("Reminder: Course starts tomorrow");
        when(templateService.renderCourseStartNotification(any(StudentDto.class), any(CourseDto.class), eq("en")))
                .thenReturn("<html><body>Test email</body></html>");

        // when
        emailService.sendCourseStartingNotification(student, course);

        // then
        verify(mailSender, times(1)).send(mimeMessage);
        verify(templateService).getCourseStartNotificationSubject(course, "en");
        verify(templateService).renderCourseStartNotification(student, course, "en");
    }

    @Test
    void sendCourseStartingNotification_WhenStudentHasLocale_ShouldUseStudentLocale() {
        // given
        StudentDto student = TestDataGenerator.createDefaultStudentDto();
        student.setLocale("de");
        CourseDto course = TestDataGenerator.createDefaultCourseDto();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getCourseStartNotificationSubject(any(CourseDto.class), eq("de")))
                .thenReturn("Erinnerung: Kurs beginnt morgen");
        when(templateService.renderCourseStartNotification(any(StudentDto.class), any(CourseDto.class), eq("de")))
                .thenReturn("<html><body>German email</body></html>");

        // when
        emailService.sendCourseStartingNotification(student, course);

        // then
        verify(mailSender, times(1)).send(mimeMessage);
        verify(templateService).getCourseStartNotificationSubject(course, "de");
        verify(templateService).renderCourseStartNotification(student, course, "de");
    }
}