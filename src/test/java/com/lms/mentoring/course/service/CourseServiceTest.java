package com.lms.mentoring.course.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import com.lms.mentoring.util.TestDataGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseServiceTest {

    private CourseRepository repo;
    private StudentRepository studentRepo;
    private CourseService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(CourseRepository.class);
        studentRepo = Mockito.mock(StudentRepository.class);
        service = new CourseService(repo, studentRepo);
    }

    @Test
    void findStartingBetween_WhenNoCoursesInRange_ShouldReturnEmptyList() {
        // given
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(0);
        LocalDateTime end = start.plusDays(1).minusSeconds(1);
        when(repo.findBySettings_StartDateBetween(start, end)).thenReturn(Collections.emptyList());

        // when
        var result = service.findStartingBetween(start, end);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void enrollStudent_WhenCourseAndStudentExist_ShouldAddStudentToCourse() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        Student student = TestDataGenerator.createDefaultStudent();
        when(repo.findById(course.getId())).thenReturn(Optional.of(course));
        when(studentRepo.findById(student.getId())).thenReturn(Optional.of(student));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        service.enrollStudent(course.getId(), student.getId());

        // then
        assertThat(course.getStudents()).contains(student);
    }

    @Test
    void update_WhenCourseExists_ShouldSaveAndReturnCourse() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        when(repo.existsById(course.getId())).thenReturn(true);
        when(repo.save(course)).thenReturn(course);

        // when
        Course result = service.update(course);

        // then
        assertThat(result).isEqualTo(course);
        verify(repo).save(course);
    }

    @Test
    void update_WhenCourseNotFound_ShouldThrowEntityNotFoundException() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        when(repo.existsById(course.getId())).thenReturn(false);

        // when & then
        assertThrows(EntityNotFoundException.class, () -> service.update(course));
        verify(repo, never()).save(any());
    }
}