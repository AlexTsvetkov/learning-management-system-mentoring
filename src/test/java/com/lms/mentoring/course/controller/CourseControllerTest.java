package com.lms.mentoring.course.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.service.CourseService;
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

class CourseControllerTest {

    private CourseService service;
    private CourseMapper mapper;
    private CourseController controller;

    @BeforeEach
    void setUp() {
        service = mock(CourseService.class);
        mapper = mock(CourseMapper.class);
        controller = new CourseController(service, mapper);
    }

    @Test
    void all_WhenCoursesExist_ShouldReturnCourseDtoList() {
        // given
        Course course = Course.builder()
                .id(UUID.randomUUID())
                .title("Math 101")
                .price(BigDecimal.valueOf(100))
                .build();
        CourseDto dto = CourseDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .price(course.getPrice())
                .build();
        when(service.findAll()).thenReturn(List.of(course));
        when(mapper.toDto(course)).thenReturn(dto);

        // when
        List<CourseDto> result = controller.all();

        // then
        assertEquals(1, result.size());
        assertEquals("Math 101", result.getFirst().getTitle());
        verify(service, times(1)).findAll();
        verify(mapper, times(1)).toDto(course);
    }

    @Test
    void get_WhenCourseExists_ShouldReturnOkWithCourseDto() {
        // given
        UUID id = UUID.randomUUID();
        Course course = Course.builder().id(id).title("Physics").build();
        CourseDto dto = CourseDto.builder().id(id).title("Physics").build();
        when(service.findById(id)).thenReturn(Optional.of(course));
        when(mapper.toDto(course)).thenReturn(dto);

        // when
        ResponseEntity<CourseDto> response = controller.get(id);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Physics", response.getBody().getTitle());
    }

    @Test
    void get_WhenCourseNotFound_ShouldReturnNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Optional.empty());

        // when
        ResponseEntity<CourseDto> response = controller.get(id);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void create_WhenValidDto_ShouldReturnCreatedWithCourseDto() {
        // given
        CourseDto dto = CourseDto.builder().title("Chemistry").build();
        Course course = Course.builder().title("Chemistry").build();
        Course created = Course.builder().id(UUID.randomUUID()).title("Chemistry").build();
        CourseDto createdDto = CourseDto.builder().id(created.getId()).title("Chemistry").build();
        when(mapper.toEntity(dto)).thenReturn(course);
        when(service.create(course)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(createdDto);

        // when
        ResponseEntity<CourseDto> response = controller.create(dto);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Chemistry", response.getBody().getTitle());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void update_WhenValidDto_ShouldReturnOkWithUpdatedCourseDto() {
        // given
        UUID id = UUID.randomUUID();
        CourseDto dto = CourseDto.builder().title("History").build();
        Course course = Course.builder().id(id).title("History").build();
        Course updated = Course.builder().id(id).title("World History").build();
        CourseDto updatedDto = CourseDto.builder().id(id).title("World History").build();
        when(mapper.toEntity(dto)).thenReturn(course);
        when(service.update(course)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        // when
        ResponseEntity<CourseDto> response = controller.update(id, dto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("World History", response.getBody().getTitle());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void delete_WhenCourseExists_ShouldReturnNoContent() {
        // given
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        // when
        ResponseEntity<Void> response = controller.delete(id);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).delete(id);
    }
}