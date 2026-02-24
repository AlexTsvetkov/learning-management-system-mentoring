package com.lms.mentoring.unit.notification.smtp;

import com.lms.mentoring.notification.smtp.SmtpCredentials;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class SmtpCredentialsTest {

    @Test
    void isValid_WhenAllFieldsPresent_ShouldReturnTrue() {
        // given
        SmtpCredentials credentials = SmtpCredentials.builder()
                .host("smtp.example.com")
                .port(587)
                .username("user@example.com")
                .password("secret")
                .build();

        // when
        boolean result = credentials.isValid();

        // then
        assertTrue(result);
    }

    @Test
    void isValid_WhenHostMissing_ShouldReturnFalse() {
        // given
        SmtpCredentials credentials = SmtpCredentials.builder()
                .port(587)
                .username("user")
                .password("pass")
                .build();

        // when
        boolean result = credentials.isValid();

        // then
        assertFalse(result);
    }

    @Test
    void isValid_WhenPortInvalid_ShouldReturnFalse() {
        // given
        SmtpCredentials credentials = SmtpCredentials.builder()
                .host("host")
                .port(0)
                .username("user")
                .password("pass")
                .build();

        // when
        boolean result = credentials.isValid();

        // then
        assertFalse(result);
    }

    @Test
    void isValid_WhenPasswordMissing_ShouldReturnFalse() {
        // given
        SmtpCredentials credentials = SmtpCredentials.builder()
                .host("host")
                .port(587)
                .username("user")
                .build();

        // when
        boolean result = credentials.isValid();

        // then
        assertFalse(result);
    }
}