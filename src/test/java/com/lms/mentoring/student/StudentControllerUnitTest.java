package com.lms.mentoring.student;

import com.lms.mentoring.student.controller.StudentController;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentControllerUnitTest {

    @Test
    void getReturnsStudentDtoWhenFound() {
        StudentService service = Mockito.mock(StudentService.class);
        StudentMapper mapper = Mockito.mock(StudentMapper.class);

        UUID id = UUID.randomUUID();
        Student s = Student.builder().id(id).firstName("John").build();
        StudentDto dto = StudentDto.builder().id(id).firstName("John").build();

        Mockito.when(service.findById(id)).thenReturn(Optional.of(s));
        Mockito.when(mapper.toDto(s)).thenReturn(dto);

        StudentController controller = new StudentController(service, mapper);
        ResponseEntity<StudentDto> resp = controller.get(id);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(resp.getBody()).isEqualTo(dto);
    }

    @Test
    void getReturnsNotFoundWhenMissing() {
        StudentService service = Mockito.mock(StudentService.class);
        StudentMapper mapper = Mockito.mock(StudentMapper.class);
        UUID id = UUID.randomUUID();

        Mockito.when(service.findById(id)).thenReturn(Optional.empty());

        StudentController controller = new StudentController(service, mapper);
        ResponseEntity<StudentDto> resp = controller.get(id);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
