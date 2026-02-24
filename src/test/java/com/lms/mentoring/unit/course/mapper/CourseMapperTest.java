package com.lms.mentoring.unit.course.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CourseMapperTest {

    private final CourseMapper mapper = Mappers.getMapper(CourseMapper.class);

    @Test
    void toDto_WhenCourseWithSettings_ShouldMapAllFieldsCorrectly() {
        // given
        Course course = TestDataGenerator.createCourseWithSettings();

        // when
        CourseDto dto = mapper.toDto(course);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(course.getId());
        assertThat(dto.getTitle()).isEqualTo(course.getTitle());
        assertThat(dto.getDescription()).isEqualTo(course.getDescription());
        assertThat(dto.getPrice()).isEqualByComparingTo(course.getPrice());
        assertThat(dto.getCoinsPaid()).isEqualByComparingTo(course.getCoinsPaid());
        assertThat(dto.getStartDate()).isEqualTo(course.getSettings().getStartDate());
        assertThat(dto.getEndDate()).isEqualTo(course.getSettings().getEndDate());
        assertThat(dto.getIsPublic()).isEqualTo(course.getSettings().getIsPublic());
    }

    @Test
    void toEntity_WhenDtoWithAllFields_ShouldMapToEntityWithSettings() {
        // given
        CourseDto dto = TestDataGenerator.createCourseDtoWithSettings();

        // when
        Course course = mapper.toEntity(dto);

        // then
        assertThat(course).isNotNull();
        assertThat(course.getId()).isEqualTo(dto.getId());
        assertThat(course.getTitle()).isEqualTo(dto.getTitle());
        assertThat(course.getDescription()).isEqualTo(dto.getDescription());
        assertThat(course.getPrice()).isEqualByComparingTo(dto.getPrice());
        assertThat(course.getCoinsPaid()).isEqualByComparingTo(dto.getCoinsPaid());
        assertThat(course.getSettings()).isNotNull();
        assertThat(course.getSettings().getStartDate()).isEqualTo(dto.getStartDate());
        assertThat(course.getSettings().getEndDate()).isEqualTo(dto.getEndDate());
        assertThat(course.getSettings().getIsPublic()).isEqualTo(dto.getIsPublic());
    }
}