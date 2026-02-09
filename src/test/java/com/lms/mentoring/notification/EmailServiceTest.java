package com.lms.mentoring.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

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
        StudentDto student = StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .courses(Collections.emptyList())
                .build();
        CourseDto course = CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(100))
                .coinsPaid(BigDecimal.ZERO)
                .startDate(LocalDateTime.of(2025, 12, 2, 10, 0))
                .endDate(LocalDateTime.of(2025, 12, 30, 18, 0))
                .isPublic(true)
                .build();
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
        StudentDto student = StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("")
                .lastName("Doe")
                .email("john.doe@example.com")
                .courses(Collections.emptyList())
                .build();
        CourseDto course = CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(100))
                .coinsPaid(BigDecimal.ZERO)
                .startDate(LocalDateTime.of(2025, 12, 2, 10, 0))
                .endDate(LocalDateTime.of(2025, 12, 30, 18, 0))
                .isPublic(true)
                .build();

        // when
        String html = invokeBuildEmailTemplate(student, course);

        // then
        assertThat(html).contains("john.doe@example.com");
        assertThat(html).contains("Java Basics");
        assertThat(html).contains("2025-12-02");
    }

    @Test
    void buildEmailTemplate_WhenStartDateNull_ShouldUseTomorrowInTemplate() {
        // given
        StudentDto student = StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .courses(Collections.emptyList())
                .build();
        CourseDto course = CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(100))
                .coinsPaid(BigDecimal.ZERO)
                .startDate(null)
                .endDate(LocalDateTime.of(2025, 12, 30, 18, 0))
                .isPublic(true)
                .build();

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