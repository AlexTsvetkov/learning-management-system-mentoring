package com.example.project.student.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    private UUID id;

    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfBirth;

    @Column(precision = 19, scale = 2)
    private BigDecimal coins;

    @ManyToMany(mappedBy = "students")
    private Set<com.example.project.course.model.Course> courses;
}
