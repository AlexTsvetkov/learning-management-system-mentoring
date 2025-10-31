package com.example.project.course.mapper;

import com.example.project.course.dto.CourseSettingsDto;
import com.example.project.course.model.CourseSettings;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseSettingsMapper {
    CourseSettingsDto toDto(CourseSettings entity);

    CourseSettings toEntity(CourseSettingsDto dto);
}

