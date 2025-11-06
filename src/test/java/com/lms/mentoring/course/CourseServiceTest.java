package com.lms.mentoring.course;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.course.service.CourseService;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CourseServiceTest {

    @Test
    void findStartingBetweenDelegatesToRepo() {
        CourseRepository repo = Mockito.mock(CourseRepository.class);
        StudentRepository studentRepo = Mockito.mock(StudentRepository.class);
        CourseService service = new CourseService(repo, studentRepo);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(0);
        LocalDateTime end = start.plusDays(1).minusSeconds(1);
        when(repo.findBySettings_StartDateBetween(start, end)).thenReturn(Collections.emptyList());
        assertThat(service.findStartingBetween(start, end)).isEmpty();
    }

    @Test
    void enrollStudentAddsStudentToCourse() {
        CourseRepository repo = Mockito.mock(CourseRepository.class);
        StudentRepository studentRepo = Mockito.mock(StudentRepository.class);
        CourseService service = new CourseService(repo, studentRepo);

        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Course c = Course.builder().id(courseId).students(new HashSet<>()).build();
        Student s = Student.builder().id(studentId).build();

        when(repo.findById(courseId)).thenReturn(Optional.of(c));
        when(studentRepo.findById(studentId)).thenReturn(Optional.of(s));
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        service.enrollStudent(courseId, studentId);

        assertThat(c.getStudents()).contains(s);
    }

    @Test
    void updateSavesAndReturnsCourseWhenExists() {
        CourseRepository repo = Mockito.mock(CourseRepository.class);
        StudentRepository studentRepo = Mockito.mock(StudentRepository.class);
        CourseService service = new CourseService(repo, studentRepo);

        UUID courseId = UUID.randomUUID();
        Course c = Course.builder().id(courseId).build();

        when(repo.existsById(courseId)).thenReturn(true);
        when(repo.save(c)).thenReturn(c);

        Course result = service.update(c);

        assertThat(result).isEqualTo(c);
        Mockito.verify(repo).save(c);
    }

    @Test
    void updateThrowsWhenCourseNotFound() {
        CourseRepository repo = Mockito.mock(CourseRepository.class);
        StudentRepository studentRepo = Mockito.mock(StudentRepository.class);
        CourseService service = new CourseService(repo, studentRepo);

        UUID courseId = UUID.randomUUID();
        Course c = Course.builder().id(courseId).build();

        when(repo.existsById(courseId)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.persistence.EntityNotFoundException.class,
                () -> service.update(c)
        );

        Mockito.verify(repo, Mockito.never()).save(Mockito.any());
    }

}
