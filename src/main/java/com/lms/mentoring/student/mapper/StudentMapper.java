package com.lms.mentoring.student.mapper;

import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.model.Student;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CourseMapper.class})
public interface StudentMapper {
    StudentDto toDto(Student s);

    Student toEntity(StudentDto dto);
}
