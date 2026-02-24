-- ======================================
-- Initial Data for Learning Management System
-- ======================================

-- Students (with locale field for i18n)
INSERT INTO students (id, first_name, last_name, email, date_of_birth, coins, locale) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Alice', 'Johnson', 'alice@example.com', '2000-05-14', 150.00, 'en'),
    ('22222222-2222-2222-2222-222222222222', 'Bob', 'Smith', 'bob@example.com', '1999-11-30', 75.50, 'de'),
    ('33333333-3333-3333-3333-333333333333', 'Carol', 'Williams', 'carol@example.com', '2001-02-20', 200.00, 'ru');

-- Course Settings
INSERT INTO course_settings (id, start_date, end_date, is_public) VALUES
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '2026-02-19 09:00:00', '2026-12-01 17:00:00', TRUE),
    ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '2026-03-01 09:00:00', '2026-12-01 17:00:00', FALSE);

-- Courses
INSERT INTO courses (id, title, description, price, coins_paid, settings_id) VALUES
    ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Intro to Java', 'Basic concepts of Java programming', 99.99, 50.00, 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Spring Boot Fundamentals', 'Learn to build REST APIs using Spring Boot', 149.99, 75.00, 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2');

-- Lessons (Single Table Inheritance with lesson_type discriminator)
-- BASIC lessons (default type)
INSERT INTO lessons (id, lesson_type, title, duration, course_id) VALUES
    ('ccccccc1-cccc-cccc-cccc-ccccccccccc1', 'BASIC', 'Java Syntax and Basics', 45, 'bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1');

-- CLASSROOM lessons (with location and capacity)
INSERT INTO lessons (id, lesson_type, title, duration, course_id, location, capacity) VALUES
    ('ccccccc2-cccc-cccc-cccc-ccccccccccc2', 'CLASSROOM', 'OOP in Java - Live Session', 90, 'bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'Room A101, Building 5', 30),
    ('ccccccc3-cccc-cccc-cccc-ccccccccccc3', 'CLASSROOM', 'Spring Boot Hands-on Workshop', 120, 'bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'Training Center, Floor 3', 25);

-- VIDEO lessons (with url and platform)
INSERT INTO lessons (id, lesson_type, title, duration, course_id, url, platform) VALUES
    ('ccccccc4-cccc-cccc-cccc-ccccccccccc4', 'VIDEO', 'Introduction to Java Collections', 35, 'bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'https://www.youtube.com/watch?v=abc123', 'YouTube'),
    ('ccccccc5-cccc-cccc-cccc-ccccccccccc5', 'VIDEO', 'Spring Boot REST API Tutorial', 45, 'bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'https://vimeo.com/video/456789', 'Vimeo'),
    ('ccccccc6-cccc-cccc-cccc-ccccccccccc6', 'VIDEO', 'Advanced Spring Data JPA', 55, 'bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'https://udemy.com/course/spring-jpa/lesson/1', 'Udemy');

-- Student-Course Enrollments
INSERT INTO student_courses (course_id, student_id) VALUES
    ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '33333333-3333-3333-3333-333333333333');