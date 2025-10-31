package com.example.project.course.controller;

import com.example.project.course.dto.CourseDto;
import com.example.project.course.mapper.CourseMapper;
import com.example.project.course.model.Course;
import com.example.project.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    private final CourseMapper mapper;

    public CourseController(CourseService service, CourseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CourseDto> all() {
        return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> get(@PathVariable UUID id) {
        return service.findById(id).map(mapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CourseDto dto) {
        Course c = mapper.toEntity(dto);
        Course created = service.create(c);
        return ResponseEntity.ok(mapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> update(@PathVariable UUID id, @Valid @RequestBody CourseDto dto) {
        dto.setId(id);
        Course c = mapper.toEntity(dto);
        Course updated = service.update(c);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
