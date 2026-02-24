package com.lms.mentoring.integration;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.CourseSettings;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StudentRepository.
 * These tests use the actual database and Spring Data JPA context.
 */
@Tag("integration")
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class StudentRepositoryIT {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void findAllWithCourses_ShouldReturnStudentsWithCourses() {
        // given
        Student student = createTestStudent("John", "Doe", "john.doe@test.com");
        studentRepository.save(student);

        // when
        List<Student> result = studentRepository.findAllWithCourses();

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(s -> s.getEmail().equals("john.doe@test.com"))).isTrue();
    }

    @Test
    void findByIdWithCourses_ShouldReturnStudentWithCourses() {
        // given
        Student student = createTestStudent("Jane", "Smith", "jane.smith@test.com");
        Student savedStudent = studentRepository.save(student);

        // when
        Optional<Student> result = studentRepository.findByIdWithCourses(savedStudent.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("jane.smith@test.com");
        assertThat(result.get().getCourses()).isNotNull();
    }

    @Test
    void findAllBy_ShouldReturnPaginatedResults() {
        // given
        for (int i = 0; i < 5; i++) {
            Student student = createTestStudent("First" + i, "Last" + i, "user" + i + "@test.com");
            studentRepository.save(student);
        }

        // when
        Page<Student> page = studentRepository.findAllBy(PageRequest.of(0, 3));

        // then
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(3);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void findByLocale_ShouldReturnStudentsWithMatchingLocale() {
        // given
        Student germanStudent = createTestStudent("Hans", "Mueller", "hans@test.com");
        germanStudent.setLocale("de");
        studentRepository.save(germanStudent);

        Student englishStudent = createTestStudent("John", "Smith", "john@test.com");
        englishStudent.setLocale("en");
        studentRepository.save(englishStudent);

        // when
        List<Student> germanStudents = studentRepository.findByLocale("de");

        // then
        assertThat(germanStudents).anyMatch(s -> s.getEmail().equals("hans@test.com"));
        assertThat(germanStudents).noneMatch(s -> s.getEmail().equals("john@test.com"));
    }

    @Test
    void findByCourseId_ShouldReturnStudentsEnrolledInCourse() {
        // given
        Course course = createTestCourse("Test Course");
        Course savedCourse = courseRepository.save(course);

        Student student = createTestStudent("Alex", "Johnson", "alex@test.com");
        student.getCourses().add(savedCourse);
        studentRepository.save(student);

        // when
        List<Student> result = studentRepository.findByCourseId(savedCourse.getId());

        // then
        assertThat(result).anyMatch(s -> s.getEmail().equals("alex@test.com"));
    }

    @Test
    void findAllWithCourses_ShouldEagerlyFetchCourseSettings() {
        // given
        Course course = createTestCourse("Course With Settings");
        Course savedCourse = courseRepository.save(course);

        Student student = createTestStudent("Bob", "Brown", "bob@test.com");
        student.getCourses().add(savedCourse);
        studentRepository.save(student);

        // when
        List<Student> result = studentRepository.findAllWithCourses();

        // then
        assertThat(result).isNotEmpty();
        Student foundStudent = result.stream()
                .filter(s -> s.getEmail().equals("bob@test.com"))
                .findFirst()
                .orElseThrow();
        
        // This should not throw LazyInitializationException
        assertThat(foundStudent.getCourses()).isNotEmpty();
        Course enrolledCourse = foundStudent.getCourses().get(0);
        assertThat(enrolledCourse.getSettings()).isNotNull();
        assertThat(enrolledCourse.getSettings().getIsPublic()).isTrue();
    }

    @Test
    void findAllBasic_ShouldReturnStudentsWithoutCourses() {
        // given
        Student student = createTestStudent("Chris", "Williams", "chris@test.com");
        studentRepository.save(student);

        // when
        List<Student> result = studentRepository.findAllBasic();

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(s -> s.getEmail().equals("chris@test.com"))).isTrue();
    }

    @Test
    void save_ShouldPersistStudentWithLocale() {
        // given
        Student student = createTestStudent("Ivan", "Petrov", "ivan@test.com");
        student.setLocale("ru");

        // when
        Student savedStudent = studentRepository.save(student);
        studentRepository.flush();

        // then
        Optional<Student> result = studentRepository.findById(savedStudent.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getLocale()).isEqualTo("ru");
    }

    private Student createTestStudent(String firstName, String lastName, String email) {
        return Student.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .dateOfBirth(LocalDate.of(2000, 1, 15))
                .coins(BigDecimal.valueOf(100))
                .locale("en")
                .courses(new java.util.ArrayList<>())
                .build();
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
}