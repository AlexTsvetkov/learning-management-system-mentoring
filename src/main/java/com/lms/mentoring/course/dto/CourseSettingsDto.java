package com.lms.mentoring.course.dto;

import jakarta.validation.constraints.*;
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

    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDateTime startDate;

    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "isPublic flag must be specified")
    private Boolean isPublic;
}
