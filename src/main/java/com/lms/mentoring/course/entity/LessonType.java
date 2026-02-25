package com.lms.mentoring.course.entity;

/**
 * Enum for lesson types used in JPA single-table inheritance.
 */
public enum LessonType {
    CLASSROOM,
    VIDEO;

    /**
     * Get the discriminator value for JPA inheritance.
     * Returns the enum name as the discriminator value.
     */
    public String getValue() {
        return this.name();
    }

    /**
     * Parse a string to LessonType enum.
     * @param value the string value (case-insensitive)
     * @return the matching LessonType
     * @throws IllegalArgumentException if no matching type found
     */
    public static LessonType fromString(String value) {
        if (value == null) {
            return VIDEO; // Default to VIDEO
        }
        return LessonType.valueOf(value.toUpperCase());
    }
}