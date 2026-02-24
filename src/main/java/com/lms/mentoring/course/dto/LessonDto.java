package com.lms.mentoring.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDto {
    private UUID id;
    private String title;
    private Integer duration;
    private String lessonType;
    
    // ClassroomLesson fields
    private String location;
    private Integer capacity;
    
    // VideoLesson fields
    private String url;
    private String platform;
}