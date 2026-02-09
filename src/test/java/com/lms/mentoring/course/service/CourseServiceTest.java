package com.lms.mentoring.course.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

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
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).students(new HashSet<>()).build();
        Student student = Student.builder().id(studentId).build();
        when(repo.findById(courseId)).thenReturn(Optional.of(course));
        when(studentRepo.findById(studentId)).thenReturn(Optional.of(student));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // when
        service.enrollStudent(courseId, studentId);

        // then
        assertThat(course.getStudents()).contains(student);
    }

    @Test
    void update_WhenCourseExists_ShouldSaveAndReturnCourse() {
        // given
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        when(repo.existsById(courseId)).thenReturn(true);
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
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        when(repo.existsById(courseId)).thenReturn(false);

        // when & then
        assertThrows(EntityNotFoundException.class, () -> service.update(course));
        verify(repo, never()).save(any());
    }
}