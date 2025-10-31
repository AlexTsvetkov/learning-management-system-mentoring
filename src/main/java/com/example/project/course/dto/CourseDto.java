package com.example.project.course.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {
    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal coinsPaid;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CourseSettingsDto settings;
}
