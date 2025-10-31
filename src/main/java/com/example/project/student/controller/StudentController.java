package com.example.project.student.controller;

import com.example.project.student.dto.StudentDto;
import com.example.project.student.mapper.StudentMapper;
import com.example.project.student.model.Student;
import com.example.project.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;
    private final StudentMapper mapper;

    public StudentController(StudentService service, StudentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
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
        Student s = mapper.toEntity(dto);
        Student created = service.create(s);
        return ResponseEntity.ok(mapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDto> update(@PathVariable UUID id, @Valid @RequestBody StudentDto dto) {
        dto.setId(id);
        Student s = mapper.toEntity(dto);
        Student updated = service.update(s);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
