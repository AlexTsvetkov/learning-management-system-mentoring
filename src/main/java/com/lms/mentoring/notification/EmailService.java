package com.lms.mentoring.notification;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;
import jakarta.mail.MessagingException;
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

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:no-reply@example.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends an email to a student reminding them that their course starts tomorrow.
     */
    public void sendCourseStartingNotification(StudentDto student, CourseDto course) {
        log.info("Sending course starting notification for student: {} and course: {}", student, course);
        String to = student.getEmail();
        String subject = String.format("Reminder: Course \"%s\" starts tomorrow", course.getTitle());
        String htmlBody = buildEmailTemplate(student, course);

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
            log.info("Sent course start notification to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send course start email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    /**
     * Builds a simple HTML email template for course start reminder.
     */
    private String buildEmailTemplate(StudentDto student, CourseDto course) {
        String name = (student.getFirstName() != null && !student.getFirstName().isBlank())
                ? student.getFirstName()
                : student.getEmail();

        String startDate = (course.getStartDate() != null)
                ? course.getStartDate().toLocalDate().toString()
                : "tomorrow";

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                    <h2 style="color: #2E86C1;">Course Reminder</h2>
                    <p>Hello %s,</p>
                    <p>This is a friendly reminder that your course <strong>%s</strong> starts on
                    <strong>%s</strong>.</p>
                    <p>Please make sure you have reviewed the course materials and are ready to begin.</p>
                    <p>Best regards,<br>
                    <em>The Learning Management System Team</em></p>
                    <hr>
                    <p style="font-size: 12px; color: #888;">This is an automated email — please do not reply.</p>
                </body>
                </html>
                """.formatted(name, course.getTitle(), startDate);
    }
}
