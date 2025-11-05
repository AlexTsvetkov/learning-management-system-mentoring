package com.lms.mentoring.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSettingsDto {
    private UUID id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isPublic;
}
