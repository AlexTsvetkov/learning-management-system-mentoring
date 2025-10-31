package com.example.project.course.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @OneToOne
    @JoinColumn(name = "course_id")
    private Course course;
}
