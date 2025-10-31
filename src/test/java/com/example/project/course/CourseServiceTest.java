package com.example.project.course;

import com.example.project.course.model.Course;
import com.example.project.course.repository.CourseRepository;
import com.example.project.course.service.CourseService;
import com.example.project.student.model.Student;
import com.example.project.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
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
        Course c = Course.builder().id(courseId).students(new ArrayList<>()).build();
        Student s = Student.builder().id(studentId).build();

        when(repo.findById(courseId)).thenReturn(Optional.of(c));
        when(studentRepo.findById(studentId)).thenReturn(Optional.of(s));
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        service.enrollStudent(courseId, studentId);

        assertThat(c.getStudents()).contains(s);
    }
}
