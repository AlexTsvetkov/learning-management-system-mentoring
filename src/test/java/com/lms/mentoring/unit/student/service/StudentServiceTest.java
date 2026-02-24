package com.lms.mentoring.unit.student.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import com.lms.mentoring.student.service.StudentService;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
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
        Student student = TestDataGenerator.createStudentWithoutId();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        Student created = service.create(student);

        // then
        assertThat(created.getId()).isNotNull();
    }

    @Test
    void chargeCoins_WhenSufficientBalance_ShouldReduceBalanceAndReturnTrue() {
        // given
        Student student = TestDataGenerator.createStudentWithCoins(BigDecimal.valueOf(100));
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
        Student student = TestDataGenerator.createStudentWithCoins(BigDecimal.valueOf(20));
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
        Course course1 = TestDataGenerator.createDefaultCourse();
        Course course2 = TestDataGenerator.createDefaultCourse();
        course2.setTitle("Spring Boot");
        Student student = TestDataGenerator.createStudentWithCourses(List.of(course1, course2));
        when(repo.findByIdWithCourses(student.getId())).thenReturn(Optional.of(student));

        // when
        List<Course> result = service.findCoursesByStudentId(student.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Course::getTitle).containsExactly("Java Basics", "Spring Boot");
    }

    @Test
    void findCoursesByStudentId_WhenStudentNotFound_ShouldReturnEmptyList() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        when(repo.findByIdWithCourses(student.getId())).thenReturn(Optional.empty());

        // when
        List<Course> result = service.findCoursesByStudentId(student.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findCoursesByStudentId_WhenStudentHasNoCourses_ShouldReturnEmptyList() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        when(repo.findByIdWithCourses(student.getId())).thenReturn(Optional.of(student));

        // when
        List<Course> result = service.findCoursesByStudentId(student.getId());

        // then
        assertThat(result).isEmpty();
    }
}