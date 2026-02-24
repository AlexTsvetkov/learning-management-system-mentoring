package com.lms.mentoring.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service responsible for sending email notifications to students.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String DEFAULT_LOCALE = "en";

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        EmailTemplateService templateService,
                        @Value("${spring.mail.username:no-reply@example.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends an email to a student reminding them that their course starts tomorrow.
     * Uses the student's locale preference for email localization.
     */
    public void sendCourseStartingNotification(StudentDto student, CourseDto course) {
        log.info("Sending course starting notification for student: {} and course: {}", student, course);
        
        String to = student.getEmail();
        String locale = getStudentLocale(student);
        String subject = templateService.getCourseStartNotificationSubject(course, locale);
        String htmlBody = templateService.renderCourseStartNotification(student, course, locale);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML email
            mailSender.send(message);
            log.info("Sent course start notification to {} in locale {}", to, locale);
        } catch (Exception ex) {
            log.error("Failed to send course start email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    /**
     * Gets the student's locale preference, defaulting to English if not set.
     */
    private String getStudentLocale(StudentDto student) {
        String locale = student.getLocale();
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        // Extract language code if full locale (e.g., "en_US" -> "en")
        if (locale.contains("_")) {
            locale = locale.split("_")[0];
        }
        return locale.toLowerCase();
    }
}