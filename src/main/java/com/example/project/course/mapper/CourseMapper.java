package com.example.project.course.mapper;

import com.example.project.course.dto.CourseDto;
import com.example.project.course.model.Course;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CourseSettingsMapper.class})
public interface CourseMapper {
    CourseDto toDto(Course entity);

    Course toEntity(CourseDto dto);
}


