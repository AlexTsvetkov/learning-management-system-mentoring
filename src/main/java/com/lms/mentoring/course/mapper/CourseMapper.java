package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.CourseSettings;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.SETTER)
public interface CourseMapper {

    @Mapping(source = "settings.startDate", target = "startDate")
    @Mapping(source = "settings.endDate", target = "endDate")
    @Mapping(source = "settings.isPublic", target = "isPublic")
    CourseDto toDto(Course entity);

    @Mapping(target = "settings", expression = "java(mapToSettings(dto))")
    Course toEntity(CourseDto dto);

    // Helper method for reverse mapping
    default CourseSettings mapToSettings(CourseDto dto) {
        if (dto == null) {
            return null;
        }
        return CourseSettings.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isPublic(dto.getIsPublic())
                .build();
    }
}


