package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.LessonDto;
import com.lms.mentoring.course.entity.ClassroomLesson;
import com.lms.mentoring.course.entity.Lesson;
import com.lms.mentoring.course.entity.VideoLesson;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

    public LessonDto toDto(Lesson lesson) {
        if (lesson == null) {
            return null;
        }

        LessonDto.LessonDtoBuilder builder = LessonDto.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .duration(lesson.getDuration());

        // Set lesson type and type-specific fields
        if (lesson instanceof ClassroomLesson classroomLesson) {
            builder.lessonType("CLASSROOM")
                    .location(classroomLesson.getLocation())
                    .capacity(classroomLesson.getCapacity());
        } else if (lesson instanceof VideoLesson videoLesson) {
            builder.lessonType("VIDEO")
                    .url(videoLesson.getUrl())
                    .platform(videoLesson.getPlatform());
        } else {
            builder.lessonType("BASIC");
        }

        return builder.build();
    }
}