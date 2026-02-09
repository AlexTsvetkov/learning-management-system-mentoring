package com.lms.mentoring.student.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StudentServiceTest {

    private StudentRepository repo;
    private StudentService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(StudentRepository.class);
        service = new StudentService(repo);
    }

    @Test
    void create_WhenIdIsNull_ShouldAssignNewId() {
        // given
        Student student = Student.builder().coins(BigDecimal.ZERO).build();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        Student created = service.create(student);

        // then
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void chargeCoins_WhenSufficientBalance_ShouldReduceBalanceAndReturnTrue() {
        // given
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .coins(BigDecimal.valueOf(100))
                .build();
        when(repo.findById(student.getId())).thenReturn(Optional.of(student));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        boolean result = service.chargeCoins(student.getId(), BigDecimal.valueOf(30));

        // then
        assertThat(result).isTrue();
        assertThat(student.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    void chargeCoins_WhenInsufficientBalance_ShouldReturnFalseAndNotChangeBalance() {
        // given
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .coins(BigDecimal.valueOf(20))
                .build();
        when(repo.findById(student.getId())).thenReturn(Optional.of(student));

        // when
        boolean result = service.chargeCoins(student.getId(), BigDecimal.valueOf(30));

        // then
        assertThat(result).isFalse();
        assertThat(student.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void findCoursesByStudentId_WhenStudentExistsWithCourses_ShouldReturnCoursesList() {
        // given
        UUID studentId = UUID.randomUUID();
        Course course1 = Course.builder()
                .id(UUID.randomUUID())
                .title("Java Basics")
                .price(BigDecimal.valueOf(99.99))
                .build();
        Course course2 = Course.builder()
                .id(UUID.randomUUID())
                .title("Spring Boot")
                .price(BigDecimal.valueOf(149.99))
                .build();
        List<Course> courses = new ArrayList<>();
        courses.add(course1);
        courses.add(course2);
        Student student = Student.builder()
                .id(studentId)
                .firstName("John")
                .lastName("Doe")
                .courses(courses)
                .build();
        when(repo.findById(studentId)).thenReturn(Optional.of(student));

        // when
        List<Course> result = service.findCoursesByStudentId(studentId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Course::getTitle).containsExactly("Java Basics", "Spring Boot");
    }

    @Test
    void findCoursesByStudentId_WhenStudentNotFound_ShouldReturnEmptyList() {
        // given
        UUID studentId = UUID.randomUUID();
        when(repo.findById(studentId)).thenReturn(Optional.empty());

        // when
        List<Course> result = service.findCoursesByStudentId(studentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findCoursesByStudentId_WhenStudentHasNoCourses_ShouldReturnEmptyList() {
        // given
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .firstName("Jane")
                .lastName("Doe")
                .courses(new ArrayList<>())
                .build();
        when(repo.findById(studentId)).thenReturn(Optional.of(student));

        // when
        List<Course> result = service.findCoursesByStudentId(studentId);

        // then
        assertThat(result).isEmpty();
    }
}