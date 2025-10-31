# Changelog

## [0.0.2] - Updated
- StudentDto now includes full CourseDto list.
- StudentMapper uses CourseMapper to map courses.
- StudentService.findById annotated with @Transactional(readOnly = true) to allow lazy-loading before mapping.
- Added unit tests for services and controllers (plain method unit tests).
