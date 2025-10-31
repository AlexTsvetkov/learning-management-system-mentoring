package com.example.project.course;

import com.example.project.course.controller.CourseController;
import com.example.project.course.dto.CourseDto;
import com.example.project.course.mapper.CourseMapper;
import com.example.project.course.model.Course;
import com.example.project.course.service.CourseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CourseControllerUnitTest {

    @Test
    void getReturnsCourseDtoWhenFound() {
        CourseService service = Mockito.mock(CourseService.class);
        CourseMapper mapper = Mockito.mock(CourseMapper.class);

        UUID id = UUID.randomUUID();
        Course c = Course.builder().id(id).title("Spring").build();
        CourseDto dto = CourseDto.builder().id(id).title("Spring").build();

        when(service.findById(id)).thenReturn(Optional.of(c));
        when(mapper.toDto(c)).thenReturn(dto);

        CourseController controller = new CourseController(service, mapper);
        ResponseEntity<CourseDto> resp = controller.get(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(dto);
    }

    @Test
    void getReturnsNotFoundWhenMissing() {
        CourseService service = Mockito.mock(CourseService.class);
        CourseMapper mapper = Mockito.mock(CourseMapper.class);
        UUID id = UUID.randomUUID();

        when(service.findById(id)).thenReturn(Optional.empty());

        CourseController controller = new CourseController(service, mapper);
        ResponseEntity<CourseDto> resp = controller.get(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
    }
}
