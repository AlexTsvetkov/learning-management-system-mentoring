package com.lms.mentoring.enrollment.dto;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.student.dto.StudentDto;

public record PurchaseResult(StudentDto student, CourseDto course) {
}