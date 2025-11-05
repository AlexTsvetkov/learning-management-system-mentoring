package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.CourseSettingsDto;
import com.lms.mentoring.course.model.CourseSettings;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseSettingsMapperTest {

    private final CourseSettingsMapper mapper = Mappers.getMapper(CourseSettingsMapper.class);

    @Test
    void shouldMapEntityToDto() {
        CourseSettings settings = CourseSettings.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .endDate(LocalDateTime.of(2025, 6, 30, 18, 0))
                .isPublic(true)
                .build();

        CourseSettingsDto dto = mapper.toDto(settings);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(settings.getId());
        assertThat(dto.getStartDate()).isEqualTo(settings.getStartDate());
        assertThat(dto.getEndDate()).isEqualTo(settings.getEndDate());
        assertThat(dto.getIsPublic()).isTrue();
    }

    @Test
    void shouldMapDtoToEntity() {
        CourseSettingsDto dto = CourseSettingsDto.builder()
                .id(UUID.randomUUID())
                .startDate(LocalDateTime.of(2025, 2, 1, 8, 0))
                .endDate(LocalDateTime.of(2025, 7, 15, 17, 30))
                .isPublic(false)
                .build();

        CourseSettings entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getStartDate()).isEqualTo(dto.getStartDate());
        assertThat(entity.getEndDate()).isEqualTo(dto.getEndDate());
        assertThat(entity.getIsPublic()).isFalse();
    }
}
