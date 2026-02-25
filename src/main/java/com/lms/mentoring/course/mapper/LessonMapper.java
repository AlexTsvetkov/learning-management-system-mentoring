package com.lms.mentoring.course.mapper;

import com.lms.mentoring.course.dto.LessonDto;
import com.lms.mentoring.course.entity.ClassroomLesson;
import com.lms.mentoring.course.entity.Lesson;
import com.lms.mentoring.course.entity.LessonType;
import com.lms.mentoring.course.entity.VideoLesson;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

    public Lesson toEntity(LessonDto dto) {
        if (dto == null) {
            return null;
        }

        LessonType lessonType = LessonType.fromString(dto.getLessonType());

        return switch (lessonType) {
            case CLASSROOM -> ClassroomLesson.builder()
                    .id(dto.getId())
                    .title(dto.getTitle())
                    .duration(dto.getDuration())
                    .location(dto.getLocation())
                    .capacity(dto.getCapacity())
                    .build();
            case VIDEO -> VideoLesson.builder()
                    .id(dto.getId())
                    .title(dto.getTitle())
                    .duration(dto.getDuration())
                    .url(dto.getUrl())
                    .platform(dto.getPlatform())
                    .build();
        };
    }

    public void updateEntity(Lesson lesson, LessonDto dto) {
        if (lesson == null || dto == null) {
            return;
        }
        
        lesson.setTitle(dto.getTitle());
        lesson.setDuration(dto.getDuration());
        
        if (lesson instanceof ClassroomLesson classroomLesson) {
            classroomLesson.setLocation(dto.getLocation());
            classroomLesson.setCapacity(dto.getCapacity());
        } else if (lesson instanceof VideoLesson videoLesson) {
            videoLesson.setUrl(dto.getUrl());
            videoLesson.setPlatform(dto.getPlatform());
        }
    }

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
            builder.lessonType(LessonType.CLASSROOM.name())
                    .location(classroomLesson.getLocation())
                    .capacity(classroomLesson.getCapacity());
        } else if (lesson instanceof VideoLesson videoLesson) {
            builder.lessonType(LessonType.VIDEO.name())
                    .url(videoLesson.getUrl())
                    .platform(videoLesson.getPlatform());
        }

        return builder.build();
    }
}