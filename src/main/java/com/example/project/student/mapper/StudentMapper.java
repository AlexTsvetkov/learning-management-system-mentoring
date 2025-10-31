package com.example.project.student.mapper;

import com.example.project.course.mapper.CourseMapper;
import com.example.project.student.dto.StudentDto;
import com.example.project.student.model.Student;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CourseMapper.class})
public interface StudentMapper {
    StudentDto toDto(Student s);

    Student toEntity(StudentDto dto);
}
