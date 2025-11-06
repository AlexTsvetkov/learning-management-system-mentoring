package com.lms.mentoring.enrollment.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.enrollment.service.CoursePurchaseService;
import com.lms.mentoring.student.dto.StudentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoursePurchaseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CoursePurchaseService purchaseService;

    @InjectMocks
    private CoursePurchaseController controller;

    private UUID studentId;
    private UUID courseId;
    private StudentDto studentDto;
    private CourseDto courseDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        studentDto = StudentDto.builder()
                .id(studentId)
                .firstName("John")
                .lastName("Doe")
                .build();

        courseDto = CourseDto.builder()
                .id(courseId)
                .title("Java Basics")
                .build();
    }

    @Test
    void shouldPurchaseCourseSuccessfully() throws Exception {
        // given
        PurchaseResult result = new PurchaseResult(studentDto, courseDto);
        when(purchaseService.purchaseCourse(any(UUID.class), any(UUID.class))).thenReturn(result);

        // when + then
        mockMvc.perform(post("/api/enrollments/purchase")
                        .param("studentId", studentId.toString())
                        .param("courseId", courseId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Course 'Java Basics' purchased successfully by student John Doe"));
    }
}
