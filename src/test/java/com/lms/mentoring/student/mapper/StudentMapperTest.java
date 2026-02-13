package com.lms.mentoring.student.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

    private final StudentMapper mapper = Mappers.getMapper(StudentMapper.class);
    private final CourseMapper courseMapper = Mappers.getMapper(CourseMapper.class);

    @BeforeEach
    void setUp() {
        // Manually inject CourseMapper into StudentMapperImpl
        if (mapper instanceof StudentMapperImpl impl) {
            impl.setCourseMapper(courseMapper);
        }
    }

    @Test
    void toDto_WhenStudentWithCourses_ShouldMapAllFieldsIncludingCourses() {
        // given
        Course course = TestDataGenerator.createDefaultCourse();
        Student student = TestDataGenerator.createStudentWithCourses(List.of(course));

        // when
        StudentDto dto = mapper.toDto(student);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(student.getId());
        assertThat(dto.getFirstName()).isEqualTo(student.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(student.getLastName());
        assertThat(dto.getEmail()).isEqualTo(student.getEmail());
        assertThat(dto.getCoins()).isEqualByComparingTo(student.getCoins());
        assertThat(dto.getCourses()).hasSize(1);
        CourseDto courseDto = dto.getCourses().get(0);
        assertThat(courseDto.getTitle()).isEqualTo(course.getTitle());
        assertThat(courseDto.getDescription()).isEqualTo(course.getDescription());
        assertThat(courseDto.getPrice()).isEqualByComparingTo(course.getPrice());
        assertThat(courseDto.getCoinsPaid()).isEqualByComparingTo(course.getCoinsPaid());
    }

    @Test
    void toEntity_WhenDtoWithCourses_ShouldMapAllFieldsIncludingCourses() {
        // given
        CourseDto courseDto = TestDataGenerator.createDefaultCourseDto();
        StudentDto dto = TestDataGenerator.createStudentDtoWithCourses(List.of(courseDto));

        // when
        Student student = mapper.toEntity(dto);

        // then
        assertThat(student).isNotNull();
        assertThat(student.getId()).isEqualTo(dto.getId());
        assertThat(student.getFirstName()).isEqualTo(dto.getFirstName());
        assertThat(student.getLastName()).isEqualTo(dto.getLastName());
        assertThat(student.getEmail()).isEqualTo(dto.getEmail());
        assertThat(student.getCoins()).isEqualByComparingTo(dto.getCoins());
        assertThat(student.getCourses()).hasSize(1);
        Course mappedCourse = student.getCourses().get(0);
        assertThat(mappedCourse.getTitle()).isEqualTo(courseDto.getTitle());
        assertThat(mappedCourse.getDescription()).isEqualTo(courseDto.getDescription());
        assertThat(mappedCourse.getPrice()).isEqualByComparingTo(courseDto.getPrice());
        assertThat(mappedCourse.getCoinsPaid()).isEqualByComparingTo(courseDto.getCoinsPaid());
    }
}