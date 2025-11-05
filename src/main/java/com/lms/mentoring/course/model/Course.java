package com.lms.mentoring.course.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.lms.mentoring.student.model.Student;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@ToString(exclude = {"lessons", "students", "settings"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @DecimalMin("0.00")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    @DecimalMin("0.00")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal coinsPaid;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "settings_id")
    @JsonManagedReference
    private CourseSettings settings;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Lesson> lessons = new HashSet<>();

    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    private Set<Student> students = new HashSet<>();

    @PrePersist
    private void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }
        if (this.coinsPaid == null) {
            this.coinsPaid = BigDecimal.ZERO;
        }
    }

    // Helper methods to manage bidirectional relationships

    public void addLesson(Lesson lesson) {
        if (lesson == null) return;
        lessons.add(lesson);
        lesson.setCourse(this);
    }

    public void removeLesson(Lesson lesson) {
        if (lesson == null) return;
        lessons.remove(lesson);
        if (lesson.getCourse() == this) {
            lesson.setCourse(null);
        }
    }

    public void addStudent(Student student) {
        if (student == null) return;
        students.add(student);
        student.getCourses().add(this);
    }

    public void removeStudent(Student student) {
        if (student == null) return;
        students.remove(student);
        student.getCourses().remove(this);
    }
}