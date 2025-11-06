package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.dto.CourseSettingsDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.CourseSettings;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class CourseMapperTest {

    private final CourseMapper mapper = Mappers.getMapper(CourseMapper.class);
    private final CourseSettingsMapper courseSettingsMapper = Mappers.getMapper(CourseSettingsMapper.class);

    {
        // Manually inject CourseSettingsMapper into CourseMapperImpl
        if (mapper instanceof CourseMapperImpl courseImpl) {
            courseImpl.setCourseSettingsMapper(courseSettingsMapper);
        }
    }

    @Test
    void shouldMapEntityToDto() {
        CourseSettings settings = CourseSettings.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 1, 10, 9, 0))
                .endDate(LocalDateTime.of(2025, 6, 10, 17, 0))
                .isPublic(true)
                .build();

        Course course = Course.builder()
                .id(UUID.randomUUID())
                .title("Java Masterclass")
                .description("Comprehensive Java course covering core and advanced topics.")
                .price(BigDecimal.valueOf(199.99))
                .coinsPaid(BigDecimal.valueOf(50))
                .settings(settings)
                .build();

        CourseDto dto = mapper.toDto(course);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(course.getId());
        assertThat(dto.getTitle()).isEqualTo("Java Masterclass");
        assertThat(dto.getDescription()).contains("Java course");
        assertThat(dto.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
        assertThat(dto.getCoinsPaid()).isEqualByComparingTo(BigDecimal.valueOf(50));

        assertThat(dto.getSettings()).isNotNull();
        assertThat(dto.getSettings().getId()).isEqualTo(settings.getId());
        assertThat(dto.getSettings().getIsPublic()).isTrue();
    }

    @Test
    void shouldMapDtoToEntity() {
        CourseSettingsDto settingsDto = CourseSettingsDto.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 3, 1, 8, 0))
                .endDate(LocalDateTime.of(2025, 8, 31, 17, 0))
                .isPublic(false)
                .build();

        CourseDto dto = CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Spring Boot Deep Dive")
                .description("Advanced Spring Boot course")
                .price(BigDecimal.valueOf(299.50))
                .coinsPaid(BigDecimal.valueOf(120))
                .settings(settingsDto)
                .build();

        Course course = mapper.toEntity(dto);

        assertThat(course).isNotNull();
        assertThat(course.getId()).isEqualTo(dto.getId());
        assertThat(course.getTitle()).isEqualTo("Spring Boot Deep Dive");
        assertThat(course.getDescription()).isEqualTo("Advanced Spring Boot course");
        assertThat(course.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(299.50));
        assertThat(course.getCoinsPaid()).isEqualByComparingTo(BigDecimal.valueOf(120));

        assertThat(course.getSettings()).isNotNull();
        assertThat(course.getSettings().getId()).isEqualTo(settingsDto.getId());
        assertThat(course.getSettings().getIsPublic()).isFalse();
    }
}
