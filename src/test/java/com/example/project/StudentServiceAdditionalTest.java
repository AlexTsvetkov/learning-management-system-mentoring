package com.example.project;

import com.example.project.student.model.Student;
import com.example.project.student.repository.StudentRepository;
import com.example.project.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

public class StudentServiceAdditionalTest {
    @Test
    void createAssignsIdIfMissing() {
        StudentRepository repo = Mockito.mock(StudentRepository.class);
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        StudentService service = new StudentService(repo);

        Student s = Student.builder().coins(java.math.BigDecimal.ZERO).build();
        Student created = service.create(s);
        assertThat(created.getId()).isNotNull();
    }
}
