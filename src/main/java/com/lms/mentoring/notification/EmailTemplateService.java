package com.lms.mentoring.notification;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for loading and rendering Mustache email templates
 * with support for internationalization based on student locale.
 */
@Service
public class EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateService.class);
    private static final String TEMPLATE_BASE_PATH = "templates/email/";
    private static final String TEMPLATE_EXTENSION = ".mustache";
    private static final String DEFAULT_LOCALE = "en";

    private final MustacheFactory mustacheFactory;
    private final Map<String, Mustache> templateCache = new ConcurrentHashMap<>();

    public EmailTemplateService() {
        this.mustacheFactory = new DefaultMustacheFactory();
    }

    @PostConstruct
    public void init() {
        // Pre-load common templates for body
        loadTemplate("course-start-notification", "en");
        loadTemplate("course-start-notification", "de");
        loadTemplate("course-start-notification", "ru");
        // Pre-load common templates for subject
        loadTemplate("course-start-subject", "en");
        loadTemplate("course-start-subject", "de");
        loadTemplate("course-start-subject", "ru");
    }

    /**
     * Renders a course start notification email template for the given locale.
     *
     * @param student  the student DTO
     * @param course   the course DTO
     * @param locale   the student's preferred locale
     * @return the rendered HTML email content
     */
    public String renderCourseStartNotification(StudentDto student, CourseDto course, String locale) {
        Map<String, Object> context = new HashMap<>();
        context.put("studentName", getStudentDisplayName(student));
        context.put("courseTitle", course.getTitle());
        context.put("startDate", getFormattedStartDate(course));

        return renderTemplate("course-start-notification", locale, context);
    }

    /**
     * Gets the display name for the student.
     */
    private String getStudentDisplayName(StudentDto student) {
        if (student.getFirstName() != null && !student.getFirstName().isBlank()) {
            return student.getFirstName();
        }
        return student.getEmail();
    }

    /**
     * Gets the formatted start date from the course.
     */
    private String getFormattedStartDate(CourseDto course) {
        if (course.getStartDate() != null) {
            return course.getStartDate().toLocalDate().toString();
        }
        return "tomorrow";
    }

    /**
     * Renders a template with the given name, locale, and context.
     *
     * @param templateName the base name of the template (without locale suffix)
     * @param locale       the locale to use for the template
     * @param context      the context data for the template
     * @return the rendered template content
     */
    public String renderTemplate(String templateName, String locale, Map<String, Object> context) {
        Mustache template = getTemplate(templateName, locale);
        if (template == null) {
            log.warn("Template not found for {} with locale {}, falling back to default", 
                    templateName, locale);
            template = getTemplate(templateName, DEFAULT_LOCALE);
        }

        if (template == null) {
            log.error("Default template not found for {}", templateName);
            throw new IllegalStateException("Template not found: " + templateName);
        }

        StringWriter writer = new StringWriter();
        template.execute(writer, context);
        return writer.toString();
    }

    /**
     * Gets a cached template or loads it if not cached.
     */
    private Mustache getTemplate(String templateName, String locale) {
        String cacheKey = templateName + "_" + locale;
        return templateCache.computeIfAbsent(cacheKey, key -> loadTemplate(templateName, locale));
    }

    /**
     * Loads a template from the classpath.
     */
    private Mustache loadTemplate(String templateName, String locale) {
        String templatePath = TEMPLATE_BASE_PATH + templateName + "_" + locale + TEMPLATE_EXTENSION;
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                log.info("Template not found at path: {}", templatePath);
                return null;
            }
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            Mustache template = mustacheFactory.compile(reader, templatePath);
            log.info("Loaded template: {}", templatePath);
            return template;
        } catch (Exception e) {
            log.error("Failed to load template: {}", templatePath, e);
            return null;
        }
    }

    /**
     * Gets the subject line for a course start notification email based on locale.
     * Uses Mustache template for localization.
     *
     * @param course the course DTO
     * @param locale the student's preferred locale
     * @return the localized subject line
     */
    public String getCourseStartNotificationSubject(CourseDto course, String locale) {
        Map<String, Object> context = new HashMap<>();
        context.put("courseTitle", course.getTitle());
        
        return renderTemplate("course-start-subject", locale, context).trim();
    }
}