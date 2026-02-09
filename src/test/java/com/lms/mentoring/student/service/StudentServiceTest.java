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
    void createAssignsIdIfMissing() {
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        Student s = Student.builder().coins(BigDecimal.ZERO).build();
        Student created = service.create(s);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void chargeCoinsReducesBalance() {
        Student s = Student.builder().id(UUID.randomUUID()).coins(BigDecimal.valueOf(100)).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        boolean ok = service.chargeCoins(s.getId(), BigDecimal.valueOf(30));

        assertThat(ok).isTrue();
        assertThat(s.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    void chargeCoinsReturnsFalseWhenInsufficientBalance() {
        Student s = Student.builder().id(UUID.randomUUID()).coins(BigDecimal.valueOf(20)).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));

        boolean ok = service.chargeCoins(s.getId(), BigDecimal.valueOf(30));

        assertThat(ok).isFalse();
        assertThat(s.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void findCoursesByStudentIdReturnsCoursesWhenStudentExists() {
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

        List<Course> result = service.findCoursesByStudentId(studentId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Course::getTitle).containsExactly("Java Basics", "Spring Boot");
    }

    @Test
    void findCoursesByStudentIdReturnsEmptyListWhenStudentNotFound() {
        UUID studentId = UUID.randomUUID();
        when(repo.findById(studentId)).thenReturn(Optional.empty());

        List<Course> result = service.findCoursesByStudentId(studentId);

        assertThat(result).isEmpty();
    }

    @Test
    void findCoursesByStudentIdReturnsEmptyListWhenStudentHasNoCourses() {
        UUID studentId = UUID.randomUUID();

        Student student = Student.builder()
                .id(studentId)
                .firstName("Jane")
                .lastName("Doe")
                .courses(new ArrayList<>())
                .build();

        when(repo.findById(studentId)).thenReturn(Optional.of(student));

        List<Course> result = service.findCoursesByStudentId(studentId);

        assertThat(result).isEmpty();
    }
}