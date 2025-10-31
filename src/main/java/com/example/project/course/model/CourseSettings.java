package com.example.project.course.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSettings {
    @Id
    private UUID id;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isPublic;
}
