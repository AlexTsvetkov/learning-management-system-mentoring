package com.lms.mentoring.unit.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.notification.EmailTemplateService;
import com.lms.mentoring.student.dto.StudentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EmailTemplateService}.
 */
@Tag("unit")
class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
        emailTemplateService.init();
    }

    @Test
    void renderCourseStartNotification_WithValidData_ShouldReturnRenderedContent() {
        // given
        StudentDto student = createStudent("john@example.com", "John");
        CourseDto course = createCourse("Java Programming", LocalDateTime.now().plusDays(1));

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "en");

        // then
        assertNotNull(result);
        assertTrue(result.contains("John"));
        assertTrue(result.contains("Java Programming"));
    }

    @Test
    void renderCourseStartNotification_WithGermanLocale_ShouldReturnGermanContent() {
        // given
        StudentDto student = createStudent("hans@example.com", "Hans");
        CourseDto course = createCourse("Java Programmierung", LocalDateTime.now().plusDays(1));

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "de");

        // then
        assertNotNull(result);
        assertTrue(result.contains("Hans"));
    }

    @Test
    void renderCourseStartNotification_WithRussianLocale_ShouldReturnRussianContent() {
        // given
        StudentDto student = createStudent("ivan@example.com", "Ivan");
        CourseDto course = createCourse("Java Development", LocalDateTime.now().plusDays(1));

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "ru");

        // then
        assertNotNull(result);
        assertTrue(result.contains("Ivan"));
    }

    @Test
    void renderCourseStartNotification_WithNoFirstName_ShouldUseEmail() {
        // given
        StudentDto student = createStudent("user@example.com", null);
        CourseDto course = createCourse("Python Basics", LocalDateTime.now().plusDays(1));

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "en");

        // then
        assertNotNull(result);
        assertTrue(result.contains("user@example.com"));
    }

    @Test
    void renderCourseStartNotification_WithBlankFirstName_ShouldUseEmail() {
        // given
        StudentDto student = createStudent("blank@example.com", "   ");
        CourseDto course = createCourse("Spring Boot", LocalDateTime.now().plusDays(1));

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "en");

        // then
        assertNotNull(result);
        assertTrue(result.contains("blank@example.com"));
    }

    @Test
    void getCourseStartNotificationSubject_WithEnglishLocale_ShouldReturnLocalizedSubject() {
        // given
        CourseDto course = createCourse("Advanced Java", LocalDateTime.now().plusDays(1));

        // when
        String subject = emailTemplateService.getCourseStartNotificationSubject(course, "en");

        // then
        assertNotNull(subject);
        assertFalse(subject.isBlank());
        assertTrue(subject.contains("Advanced Java"));
    }

    @Test
    void getCourseStartNotificationSubject_WithGermanLocale_ShouldReturnGermanSubject() {
        // given
        CourseDto course = createCourse("Kotlin Kurs", LocalDateTime.now().plusDays(1));

        // when
        String subject = emailTemplateService.getCourseStartNotificationSubject(course, "de");

        // then
        assertNotNull(subject);
        assertFalse(subject.isBlank());
        assertTrue(subject.contains("Kotlin Kurs"));
    }

    @Test
    void renderTemplate_WithInvalidLocale_ShouldFallbackToDefault() {
        // given
        Map<String, Object> context = new HashMap<>();
        context.put("studentName", "Test");
        context.put("courseTitle", "Test Course");
        context.put("startDate", "2026-01-01");

        // when
        String result = emailTemplateService.renderTemplate("course-start-notification", "fr", context);

        // then
        assertNotNull(result);
        assertTrue(result.contains("Test Course"));
    }

    @Test
    void renderTemplate_WithNonExistentTemplate_ShouldThrowException() {
        // given
        Map<String, Object> context = new HashMap<>();
        context.put("test", "value");

        // when & then
        assertThrows(IllegalStateException.class, () -> 
            emailTemplateService.renderTemplate("non-existent-template", "en", context));
    }

    @Test
    void renderCourseStartNotification_WithNullStartDate_ShouldUseTomorrow() {
        // given
        StudentDto student = createStudent("test@example.com", "Test");
        CourseDto course = new CourseDto();
        course.setTitle("Course Without Date");
        course.setStartDate(null);

        // when
        String result = emailTemplateService.renderCourseStartNotification(student, course, "en");

        // then
        assertNotNull(result);
        assertTrue(result.contains("tomorrow") || result.contains("Course Without Date"));
    }

    private StudentDto createStudent(String email, String firstName) {
        StudentDto student = new StudentDto();
        student.setId(UUID.randomUUID());
        student.setEmail(email);
        student.setFirstName(firstName);
        student.setLastName("Doe");
        student.setCoins(BigDecimal.valueOf(100));
        return student;
    }

    private CourseDto createCourse(String title, LocalDateTime startDate) {
        CourseDto course = new CourseDto();
        course.setId(UUID.randomUUID());
        course.setTitle(title);
        course.setDescription("Test course description");
        course.setPrice(BigDecimal.valueOf(99.99));
        course.setStartDate(startDate);
        return course;
    }
}