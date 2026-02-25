package com.lms.mentoring.course.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.dto.LessonDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.mapper.LessonMapper;
import com.lms.mentoring.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseService service;
    private final CourseMapper mapper;
    private final LessonMapper lessonMapper;

    public CourseController(CourseService service, CourseMapper mapper, LessonMapper lessonMapper) {
        this.service = service;
        this.mapper = mapper;
        this.lessonMapper = lessonMapper;
    }

    /**
     * Get all courses with pagination support.
     * 
     * @param pageable pagination parameters (page, size, sort)
     *                 Example: /api/v1/courses?page=0&size=10&sort=title,asc
     * @return paginated list of courses
     */
    @GetMapping
    public Page<CourseDto> all(@PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return service.findAll(pageable).map(mapper::toDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> get(@PathVariable UUID id) {
        return service.findById(id).map(mapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CourseDto dto) {
        Course c = mapper.toEntity(dto);
        Course created = service.create(c);
        CourseDto body = mapper.toDto(created);
        return ResponseEntity.created(java.net.URI.create("/api/v1/courses/" + body.getId())).body(body);
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

    /**
     * Get all lessons for a specific course.
     * 
     * @param id the course ID
     * @return list of lessons (CLASSROOM or VIDEO types)
     */
    @GetMapping("/{id}/lessons")
    public ResponseEntity<List<LessonDto>> getLessons(@PathVariable UUID id) {
        return service.findByIdWithLessons(id)
                .map(course -> course.getLessons().stream()
                        .map(lessonMapper::toDto)
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new lesson for a course.
     * 
     * @param courseId the course ID
     * @param dto the lesson data (lessonType must be "CLASSROOM" or "VIDEO")
     * @return the created lesson
     */
    @PostMapping("/{courseId}/lessons")
    public ResponseEntity<LessonDto> createLesson(@PathVariable UUID courseId, @Valid @RequestBody LessonDto dto) {
        var lesson = lessonMapper.toEntity(dto);
        var created = service.createLesson(courseId, lesson);
        var body = lessonMapper.toDto(created);
        return ResponseEntity.created(java.net.URI.create("/api/v1/courses/" + courseId + "/lessons/" + body.getId())).body(body);
    }

    /**
     * Get a specific lesson by ID.
     * 
     * @param courseId the course ID
     * @param lessonId the lesson ID
     * @return the lesson
     */
    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<LessonDto> getLesson(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
        return service.findLessonById(lessonId)
                .filter(lesson -> lesson.getCourse().getId().equals(courseId))
                .map(lessonMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing lesson.
     * 
     * @param courseId the course ID
     * @param lessonId the lesson ID
     * @param dto the updated lesson data
     * @return the updated lesson
     */
    @PutMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<LessonDto> updateLesson(@PathVariable UUID courseId, @PathVariable UUID lessonId, @Valid @RequestBody LessonDto dto) {
        return service.findLessonById(lessonId)
                .filter(lesson -> lesson.getCourse().getId().equals(courseId))
                .map(existing -> {
                    lessonMapper.updateEntity(existing, dto);
                    var updated = service.updateLesson(courseId, lessonId, existing);
                    return ResponseEntity.ok(lessonMapper.toDto(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a lesson.
     * 
     * @param courseId the course ID
     * @param lessonId the lesson ID
     * @return no content
     */
    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
        service.deleteLesson(courseId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
