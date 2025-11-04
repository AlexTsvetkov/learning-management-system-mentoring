package com.example.project.student.mapper;


import com.example.project.student.dto.StudentDto;
import com.example.project.student.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StudentMapperTest {

    @Autowired
    private StudentMapper mapper; // now Spring injects a fully initialized mapper

    @Test
    void testToDto() {
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .coins(BigDecimal.valueOf(100))
                .build();

        StudentDto dto = mapper.toDto(student);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(student.getId());
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(dto.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void testToEntity() {
        StudentDto dto = new StudentDto();
        dto.setId(UUID.randomUUID());
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setEmail("jane.smith@example.com");
        dto.setDateOfBirth(LocalDate.of(1995, 5, 20));
        dto.setCoins(BigDecimal.valueOf(200));

        Student student = mapper.toEntity(dto);

        assertThat(student).isNotNull();
        assertThat(student.getId()).isEqualTo(dto.getId());
        assertThat(student.getFirstName()).isEqualTo("Jane");
        assertThat(student.getLastName()).isEqualTo("Smith");
        assertThat(student.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(student.getCoins()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }
}
