package com.lms.mentoring.enrollment.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.enrollment.service.CoursePurchaseService;
import com.lms.mentoring.student.dto.StudentDto;
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

    @PostMapping("/purchase")
    public ResponseEntity<String> purchaseCourse(@RequestParam UUID studentId, @RequestParam UUID courseId) {
        PurchaseResult purchaseResult = purchaseService.purchaseCourse(studentId, courseId);
        StudentDto student = purchaseResult.student();
        CourseDto course = purchaseResult.course();
        return ResponseEntity.ok("Course '" + course.getTitle() + "' purchased successfully by student " + student.getFirstName() + " " + student.getLastName());
    }
}
