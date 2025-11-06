package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.CourseSettingsDto;
import com.lms.mentoring.course.entity.CourseSettings;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseSettingsMapper {
    CourseSettingsDto toDto(CourseSettings entity);

    CourseSettings toEntity(CourseSettingsDto dto);
}

