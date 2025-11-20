package com.lms.mentoring.enrollment.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.enrollment.service.CoursePurchaseService;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class CoursePurchaseController {

    private final CoursePurchaseService purchaseService;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;

    @PostMapping("/purchase")
    public ResponseEntity<String> purchaseCourse(@RequestParam UUID studentId, @RequestParam UUID courseId) {
        PurchaseResult<Student, Course> purchaseResult = purchaseService.purchaseCourse(studentId, courseId);
        StudentDto student = studentMapper.toDto(purchaseResult.student());
        CourseDto course = courseMapper.toDto(purchaseResult.course());
        return ResponseEntity.ok("Course '" + course.getTitle() + "' purchased successfully by student " + student.getFirstName() + " " + student.getLastName());
    }
}
