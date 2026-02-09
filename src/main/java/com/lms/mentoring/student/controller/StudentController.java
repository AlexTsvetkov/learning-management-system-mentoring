package com.lms.mentoring.student.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
    private final StudentService service;
    private final StudentMapper mapper;
    private final CourseMapper courseMapper;

    public StudentController(StudentService service, StudentMapper mapper, CourseMapper courseMapper) {
        this.service = service;
        this.mapper = mapper;
        this.courseMapper = courseMapper;
    }

    @GetMapping
    public List<StudentDto> all() {
        return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> get(@PathVariable UUID id) {
        return service.findById(id).map(mapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StudentDto> create(@Valid @RequestBody StudentDto dto) {
        Student student = mapper.toEntity(dto);
        Student createdStudent = service.create(student);
        StudentDto body = mapper.toDto(createdStudent);
        return ResponseEntity.created(java.net.URI.create("/api/v1/students/" + body.getId())).body(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDto> update(@PathVariable UUID id, @Valid @RequestBody StudentDto dto) {
        dto.setId(id);
        Student student = mapper.toEntity(dto);
        Student updatedStudent = service.update(student);
        return ResponseEntity.ok(mapper.toDto(updatedStudent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    public List<CourseDto> getCourses(@PathVariable UUID id) {
        return service.findCoursesByStudentId(id).stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }
}
