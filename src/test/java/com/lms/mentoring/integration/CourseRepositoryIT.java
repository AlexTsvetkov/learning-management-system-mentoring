package com.lms.mentoring.integration;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.CourseSettings;
import com.lms.mentoring.course.repository.CourseRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CourseRepository.
 * These tests use the actual database and Spring Data JPA context.
 */
@Tag("integration")
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class CourseRepositoryIT {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void findAllWithSettings_ShouldReturnCoursesWithSettings() {
        // given
        Course course = createTestCourse("Test Course");
        courseRepository.save(course);

        // when
        List<Course> result = courseRepository.findAllWithSettings();

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(c -> c.getTitle().equals("Test Course"))).isTrue();
    }

    @Test
    void findAllBy_ShouldReturnPaginatedResults() {
        // given
        for (int i = 0; i < 5; i++) {
            Course course = createTestCourse("Course " + i);
            courseRepository.save(course);
        }

        // when
        Page<Course> page = courseRepository.findAllBy(PageRequest.of(0, 3));

        // then
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(3);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void findByStartDateBetweenWithSettings_ShouldReturnCoursesStartingInRange() {
        // given
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Course course = createTestCourseWithStartDate("Tomorrow Course", tomorrow);
        courseRepository.save(course);

        LocalDateTime rangeStart = tomorrow.minusHours(1);
        LocalDateTime rangeEnd = tomorrow.plusHours(1);

        // when
        List<Course> result = courseRepository.findByStartDateBetweenWithSettings(rangeStart, rangeEnd);

        // then
        assertThat(result).anyMatch(c -> c.getTitle().equals("Tomorrow Course"));
    }

    private Course createTestCourse(String title) {
        CourseSettings settings = CourseSettings.builder()
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(30))
                .isPublic(true)
                .build();

        return Course.builder()
                .title(title)
                .description("Test course description")
                .price(BigDecimal.valueOf(99.99))
                .coinsPaid(BigDecimal.ZERO)
                .settings(settings)
                .build();
    }

    private Course createTestCourseWithStartDate(String title, LocalDateTime startDate) {
        CourseSettings settings = CourseSettings.builder()
                .startDate(startDate)
                .endDate(startDate.plusDays(30))
                .isPublic(true)
                .build();

        return Course.builder()
                .title(title)
                .description("Test course description")
                .price(BigDecimal.valueOf(99.99))
                .coinsPaid(BigDecimal.ZERO)
                .settings(settings)
                .build();
    }
}