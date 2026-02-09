package com.lms.mentoring.student.mapper;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        Course course = Course.builder()
                .id(UUID.randomUUID())
                .title("Java Masterclass")
                .description("Java course")
                .price(BigDecimal.valueOf(150))
                .coinsPaid(BigDecimal.valueOf(50))
                .build();
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .coins(BigDecimal.valueOf(100))
                .courses(List.of(course))
                .build();

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
        CourseDto courseDto = dto.getCourses().getFirst();
        assertThat(courseDto.getTitle()).isEqualTo(course.getTitle());
        assertThat(courseDto.getDescription()).isEqualTo(course.getDescription());
        assertThat(courseDto.getPrice()).isEqualByComparingTo(course.getPrice());
        assertThat(courseDto.getCoinsPaid()).isEqualByComparingTo(course.getCoinsPaid());
    }

    @Test
    void toEntity_WhenDtoWithCourses_ShouldMapAllFieldsIncludingCourses() {
        // given
        CourseDto courseDto = CourseDto.builder()
                .id(UUID.randomUUID())
                .title("Spring Boot")
                .description("Spring Boot course")
                .price(BigDecimal.valueOf(200))
                .coinsPaid(BigDecimal.valueOf(80))
                .build();
        StudentDto dto = StudentDto.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .coins(BigDecimal.valueOf(300))
                .courses(List.of(courseDto))
                .build();

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
        Course mappedCourse = student.getCourses().getFirst();
        assertThat(mappedCourse.getTitle()).isEqualTo(courseDto.getTitle());
        assertThat(mappedCourse.getDescription()).isEqualTo(courseDto.getDescription());
        assertThat(mappedCourse.getPrice()).isEqualByComparingTo(courseDto.getPrice());
        assertThat(mappedCourse.getCoinsPaid()).isEqualByComparingTo(courseDto.getCoinsPaid());
    }
}