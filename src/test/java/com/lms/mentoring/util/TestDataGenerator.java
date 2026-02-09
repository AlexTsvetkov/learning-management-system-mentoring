package com.lms.mentoring.util;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.CourseSettings;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for generating test data.
 * Provides factory methods for creating valid entities and DTOs for testing.
 */
public final class TestDataGenerator {

    private TestDataGenerator() {
        // Utility class, no instantiation
    }

    // ==================== Student ====================

    public static Student createDefaultStudent() {
        return Student.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(2000, 1, 15))
                .coins(BigDecimal.valueOf(100))
                .courses(new ArrayList<>())
                .build();
    }

    public static Student createStudentWithCoins(BigDecimal coins) {
        Student student = createDefaultStudent();
        student.setCoins(coins);
        return student;
    }

    public static Student createStudentWithCourses(List<Course> courses) {
        Student student = createDefaultStudent();
        student.setCourses(new ArrayList<>(courses));
        return student;
    }

    public static Student createStudentWithoutId() {
        return Student.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .coins(BigDecimal.ZERO)
                .build();
    }

    public static StudentDto createDefaultStudentDto() {
        return StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(2000, 1, 15))
                .coins(BigDecimal.valueOf(100))
                .courses(Collections.emptyList())
                .build();
    }

    public static StudentDto createStudentDtoWithCourses(List<CourseDto> courses) {
        StudentDto dto = createDefaultStudentDto();
        dto.setCourses(new ArrayList<>(courses));
        return dto;
    }

    public static StudentDto createStudentDtoWithoutFirstName() {
        return StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("")
                .lastName("Doe")
                .email("john.doe@example.com")
                .coins(BigDecimal.valueOf(100))
                .courses(Collections.emptyList())
                .build();
    }

    // ==================== Course ====================

    public static Course createDefaultCourse() {
        return Course.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(99.99))
                .coinsPaid(BigDecimal.valueOf(50))
                .students(new HashSet<>())
                .build();
    }

    public static Course createCourseWithPrice(BigDecimal price) {
        Course course = createDefaultCourse();
        course.setCoinsPaid(price);
        return course;
    }

    public static Course createCourseWithSettings() {
        CourseSettings settings = CourseSettings.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 6, 1, 9, 0))
                .endDate(LocalDateTime.of(2025, 12, 31, 17, 0))
                .isPublic(true)
                .build();

        return Course.builder()
                .id(UUID.randomUUID())
                .title("Java Masterclass")
                .description("Comprehensive Java course covering core and advanced topics")
                .price(BigDecimal.valueOf(199.99))
                .coinsPaid(BigDecimal.valueOf(100))
                .settings(settings)
                .students(new HashSet<>())
                .build();
    }

    public static Course createCourseWithoutId() {
        return Course.builder()
                .title("Spring Boot")
                .description("Learn Spring Boot")
                .price(BigDecimal.valueOf(149.99))
                .coinsPaid(BigDecimal.valueOf(75))
                .build();
    }

    public static CourseDto createDefaultCourseDto() {
        return CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(99.99))
                .coinsPaid(BigDecimal.valueOf(50))
                .startDate(LocalDateTime.of(2025, 6, 1, 9, 0))
                .endDate(LocalDateTime.of(2025, 12, 31, 17, 0))
                .isPublic(true)
                .build();
    }

    public static CourseDto createCourseDtoWithSettings() {
        return CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Spring Boot Deep Dive")
                .description("Advanced Spring Boot course")
                .price(BigDecimal.valueOf(299.50))
                .coinsPaid(BigDecimal.valueOf(120))
                .startDate(LocalDateTime.of(2025, 3, 1, 8, 0))
                .endDate(LocalDateTime.of(2025, 8, 31, 17, 0))
                .isPublic(false)
                .build();
    }

    public static CourseDto createCourseDtoWithoutStartDate() {
        return CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .description("Learn Java from scratch")
                .price(BigDecimal.valueOf(100))
                .coinsPaid(BigDecimal.ZERO)
                .startDate(null)
                .endDate(LocalDateTime.of(2025, 12, 30, 18, 0))
                .isPublic(true)
                .build();
    }

    // ==================== CourseSettings ====================

    public static CourseSettings createDefaultCourseSettings() {
        return CourseSettings.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endDate(LocalDateTime.of(2025, 6, 10, 17, 0))
                .isPublic(true)
                .build();
    }
}