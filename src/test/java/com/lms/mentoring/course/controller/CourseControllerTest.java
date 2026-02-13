package com.lms.mentoring.course.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.service.CourseService;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

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
        Course course = TestDataGenerator.createDefaultCourse();
        CourseDto dto = TestDataGenerator.createDefaultCourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        when(service.findAll()).thenReturn(List.of(course));
        when(mapper.toDto(course)).thenReturn(dto);

        // when
        List<CourseDto> result = controller.all();

        // then
        assertEquals(1, result.size());
        assertEquals(course.getTitle(), result.get(0).getTitle());
        verify(service, times(1)).findAll();
        verify(mapper, times(1)).toDto(course);
    }

    @Test
    void get_WhenCourseExists_ShouldReturnOkWithCourseDto() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        CourseDto dto = TestDataGenerator.createDefaultCourseDto();
        dto.setId(course.getId());
        when(service.findById(course.getId())).thenReturn(Optional.of(course));
        when(mapper.toDto(course)).thenReturn(dto);

        // when
        ResponseEntity<CourseDto> response = controller.get(course.getId());

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto.getTitle(), response.getBody().getTitle());
    }

    @Test
    void get_WhenCourseNotFound_ShouldReturnNotFound() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        when(service.findById(course.getId())).thenReturn(Optional.empty());

        // when
        ResponseEntity<CourseDto> response = controller.get(course.getId());

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void create_WhenValidDto_ShouldReturnCreatedWithCourseDto() {
        // given
        CourseDto inputDto = TestDataGenerator.createDefaultCourseDto();
        inputDto.setId(null);
        Course course = TestDataGenerator.createCourseWithoutId();
        Course created = TestDataGenerator.createDefaultCourse();
        CourseDto createdDto = TestDataGenerator.createDefaultCourseDto();
        createdDto.setId(created.getId());
        when(mapper.toEntity(inputDto)).thenReturn(course);
        when(service.create(course)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(createdDto);

        // when
        ResponseEntity<CourseDto> response = controller.create(inputDto);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(createdDto.getTitle(), response.getBody().getTitle());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void update_WhenValidDto_ShouldReturnOkWithUpdatedCourseDto() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        CourseDto inputDto = TestDataGenerator.createDefaultCourseDto();
        Course updated = TestDataGenerator.createDefaultCourse();
        updated.setId(course.getId());
        updated.setTitle("Updated Title");
        CourseDto updatedDto = TestDataGenerator.createDefaultCourseDto();
        updatedDto.setId(course.getId());
        updatedDto.setTitle("Updated Title");
        when(mapper.toEntity(inputDto)).thenReturn(course);
        when(service.update(course)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        // when
        ResponseEntity<CourseDto> response = controller.update(course.getId(), inputDto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Title", response.getBody().getTitle());
        assertEquals(course.getId(), response.getBody().getId());
    }

    @Test
    void delete_WhenCourseExists_ShouldReturnNoContent() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        doNothing().when(service).delete(course.getId());

        // when
        ResponseEntity<Void> response = controller.delete(course.getId());

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).delete(course.getId());
    }
}