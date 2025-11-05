-- ======================================
-- Initial Data for Learning Management System
-- ======================================

-- Students
INSERT INTO students (id, first_name, last_name, email, date_of_birth, coins) VALUES
                                                                                  ('11111111-1111-1111-1111-111111111111', 'Alice', 'Johnson', 'alice@example.com', '2000-05-14', 150.00),
                                                                                  ('22222222-2222-2222-2222-222222222222', 'Bob', 'Smith', 'bob@example.com', '1999-11-30', 75.50),
                                                                                  ('33333333-3333-3333-3333-333333333333', 'Carol', 'Williams', 'carol@example.com', '2001-02-20', 200.00);

-- Course Settings
INSERT INTO course_settings (id, start_date, end_date, is_public) VALUES
                                                                      ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '2025-11-06 09:00:00', '2025-12-01 17:00:00', TRUE),
                                                                      ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '2025-11-06 09:00:00', '2025-12-01 17:00:00', FALSE);

-- Courses
INSERT INTO courses (id, title, description, price, coins_paid, settings_id) VALUES
                                                                                 ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Intro to Java', 'Basic concepts of Java programming', 99.99, 50.00, 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
                                                                                 ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Spring Boot Fundamentals', 'Learn to build REST APIs using Spring Boot', 149.99, 75.00, 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2');

-- Lessons
INSERT INTO lessons (id, title, duration, course_id) VALUES
                                                         ('ccccccc1-cccc-cccc-cccc-ccccccccccc1', 'Java Syntax and Basics', 45, 'bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1'),
                                                         ('ccccccc2-cccc-cccc-cccc-ccccccccccc2', 'OOP in Java', 60, 'bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1'),
                                                         ('ccccccc3-cccc-cccc-cccc-ccccccccccc3', 'Spring Boot Setup', 40, 'bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2');

-- Student-Course Enrollments
INSERT INTO student_courses (course_id, student_id) VALUES
                                                        ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '11111111-1111-1111-1111-111111111111'),
                                                        ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '22222222-2222-2222-2222-222222222222'),
                                                        ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '33333333-3333-3333-3333-333333333333');
