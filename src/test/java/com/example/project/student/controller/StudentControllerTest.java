package com.example.project.student.controller;

import com.example.project.student.dto.StudentDto;
import com.example.project.student.mapper.StudentMapper;
import com.example.project.student.model.Student;
import com.example.project.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentControllerTest {

    private StudentService service;
    private StudentMapper mapper;
    private StudentController controller;

    @BeforeEach
    void setUp() {
        service = mock(StudentService.class);
        mapper = mock(StudentMapper.class);
        controller = new StudentController(service, mapper);
    }

    @Test
    void shouldReturnAllStudents() {
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .coins(BigDecimal.valueOf(100))
                .build();
        StudentDto dto = StudentDto.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .coins(student.getCoins())
                .build();

        when(service.findAll()).thenReturn(List.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        List<StudentDto> result = controller.all();

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(service, times(1)).findAll();
        verify(mapper, times(1)).toDto(student);
    }

    @Test
    void shouldReturnStudentById() {
        UUID id = UUID.randomUUID();
        Student student = Student.builder().id(id).firstName("Jane").build();
        StudentDto dto = StudentDto.builder().id(id).firstName("Jane").build();

        when(service.findById(id)).thenReturn(Optional.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        ResponseEntity<StudentDto> response = controller.get(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Jane", response.getBody().getFirstName());
    }

    @Test
    void shouldReturnNotFoundWhenStudentMissing() {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<StudentDto> response = controller.get(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldCreateStudent() {
        StudentDto dto = StudentDto.builder().firstName("Alice").build();
        Student student = Student.builder().firstName("Alice").build();
        Student created = Student.builder().id(UUID.randomUUID()).firstName("Alice").build();
        StudentDto createdDto = StudentDto.builder().id(created.getId()).firstName("Alice").build();

        when(mapper.toEntity(dto)).thenReturn(student);
        when(service.create(student)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(createdDto);

        ResponseEntity<StudentDto> response = controller.create(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void shouldUpdateStudent() {
        UUID id = UUID.randomUUID();
        StudentDto dto = StudentDto.builder().firstName("Bob").build();
        Student student = Student.builder().id(id).firstName("Bob").build();
        Student updated = Student.builder().id(id).firstName("Bobby").build();
        StudentDto updatedDto = StudentDto.builder().id(id).firstName("Bobby").build();

        when(mapper.toEntity(dto)).thenReturn(student);
        when(service.update(student)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        ResponseEntity<StudentDto> response = controller.update(id, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bobby", response.getBody().getFirstName());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void shouldDeleteStudent() {
        UUID id = UUID.randomUUID();

        doNothing().when(service).delete(id);

        ResponseEntity<Void> response = controller.delete(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).delete(id);
    }
}
