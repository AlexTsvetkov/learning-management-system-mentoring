package com.example.project;

import com.example.project.student.model.Student;
import com.example.project.student.repository.StudentRepository;
import com.example.project.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class StudentServiceTest {
    @Test
    void chargeCoinsReducesBalance() {
        StudentRepository repo = Mockito.mock(StudentRepository.class);
        Student s = Student.builder().id(UUID.randomUUID()).coins(java.math.BigDecimal.valueOf(100)).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        StudentService service = new StudentService(repo);
        boolean ok = service.chargeCoins(s.getId(), java.math.BigDecimal.valueOf(30));
        assertThat(ok).isTrue();
        assertThat(s.getCoins()).isEqualByComparingTo(java.math.BigDecimal.valueOf(70));
    }
}
