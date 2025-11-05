package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.model.Course;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CourseSettingsMapper.class})
public interface CourseMapper {
    CourseDto toDto(Course entity);

    Course toEntity(CourseDto dto);
}


