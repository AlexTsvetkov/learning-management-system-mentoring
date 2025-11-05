package com.lms.mentoring.student.repository;

import com.lms.mentoring.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
}
