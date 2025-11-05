package com.lms.mentoring;

import com.lms.mentoring.student.model.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import com.lms.mentoring.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
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
