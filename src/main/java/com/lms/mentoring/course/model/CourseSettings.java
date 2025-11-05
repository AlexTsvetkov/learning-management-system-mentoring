package com.lms.mentoring.course.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_settings")
@Getter
@Setter
@ToString(exclude = {"course"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSettings {
    @Id
    private UUID id;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isPublic;

    @OneToOne(mappedBy = "settings")
    private Course course;
}
