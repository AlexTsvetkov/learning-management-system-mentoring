package com.lms.mentoring.course;

import com.lms.mentoring.course.controller.CourseController;
import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.model.Course;
import com.lms.mentoring.course.service.CourseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
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

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
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

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
