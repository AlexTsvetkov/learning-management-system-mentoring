package com.lms.mentoring.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.util.TestDataGenerator;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender, "no-reply@example.com");
    }

    @Test
    void sendCourseStartingNotification_WhenValidStudentAndCourse_ShouldSendEmail() {
        // given
        StudentDto student = TestDataGenerator.createDefaultStudentDto();
        CourseDto course = TestDataGenerator.createDefaultCourseDto();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendCourseStartingNotification(student, course);

        // then
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void buildEmailTemplate_WhenFirstNameEmpty_ShouldUseEmailAsGreeting() {
        // given
        StudentDto student = TestDataGenerator.createStudentDtoWithoutFirstName();
        CourseDto course = TestDataGenerator.createDefaultCourseDto();

        // when
        String html = invokeBuildEmailTemplate(student, course);

        // then
        assertThat(html).contains(student.getEmail());
        assertThat(html).contains(course.getTitle());
    }

    @Test
    void buildEmailTemplate_WhenStartDateNull_ShouldUseTomorrowInTemplate() {
        // given
        StudentDto student = TestDataGenerator.createDefaultStudentDto();
        CourseDto course = TestDataGenerator.createCourseDtoWithoutStartDate();

        // when
        String html = invokeBuildEmailTemplate(student, course);

        // then
        assertThat(html).contains("tomorrow");
    }

    private String invokeBuildEmailTemplate(StudentDto student, CourseDto course) {
        try {
            var method = EmailService.class.getDeclaredMethod("buildEmailTemplate", StudentDto.class, CourseDto.class);
            method.setAccessible(true);
            return (String) method.invoke(emailService, student, course);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}