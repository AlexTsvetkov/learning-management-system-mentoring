package com.lms.mentoring.unit.notification.smtp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.mentoring.notification.smtp.SmtpCredentials;
import com.lms.mentoring.notification.smtp.SmtpCredentialsException;
import com.lms.mentoring.notification.smtp.VcapSmtpCredentialsProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class VcapSmtpCredentialsProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SERVICE_NAME = "lms-smtp-credentials";

    @Test
    void getCredentials_WhenValidVcapServices_ShouldReturnCredentials() {
        // given
        String vcapServices = """
            {
                "user-provided": [
                    {
                        "name": "lms-smtp-credentials",
                        "credentials": {
                            "host": "smtp.example.com",
                            "port": "587",
                            "username": "user@example.com",
                            "password": "secret123",
                            "from": "no-reply@example.com"
                        }
                    }
                ]
            }
            """;
        VcapSmtpCredentialsProvider provider = new VcapSmtpCredentialsProvider(
                vcapServices, SERVICE_NAME, objectMapper);

        // when
        SmtpCredentials credentials = provider.getCredentials();

        // then
        assertNotNull(credentials);
        assertEquals("smtp.example.com", credentials.getHost());
        assertEquals(587, credentials.getPort());
        assertEquals("user@example.com", credentials.getUsername());
        assertTrue(credentials.isValid());
    }

    @Test
    void getCredentials_WhenVcapServicesIsNull_ShouldThrowException() {
        // given
        VcapSmtpCredentialsProvider provider = new VcapSmtpCredentialsProvider(
                null, SERVICE_NAME, objectMapper);

        // when & then
        assertThrows(SmtpCredentialsException.class, provider::getCredentials);
    }

    @Test
    void getCredentials_WhenServiceNotFound_ShouldThrowException() {
        // given
        String vcapServices = """
            {
                "user-provided": [
                    {
                        "name": "other-service",
                        "credentials": { "key": "value" }
                    }
                ]
            }
            """;
        VcapSmtpCredentialsProvider provider = new VcapSmtpCredentialsProvider(
                vcapServices, SERVICE_NAME, objectMapper);

        // when & then
        assertThrows(SmtpCredentialsException.class, provider::getCredentials);
    }

    @Test
    void getCredentials_WhenHostMissing_ShouldThrowException() {
        // given
        String vcapServices = """
            {
                "user-provided": [
                    {
                        "name": "lms-smtp-credentials",
                        "credentials": {
                            "port": "587",
                            "username": "user@example.com",
                            "password": "secret123"
                        }
                    }
                ]
            }
            """;
        VcapSmtpCredentialsProvider provider = new VcapSmtpCredentialsProvider(
                vcapServices, SERVICE_NAME, objectMapper);

        // when & then
        assertThrows(SmtpCredentialsException.class, provider::getCredentials);
    }

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        // given
        VcapSmtpCredentialsProvider provider = new VcapSmtpCredentialsProvider(
                null, SERVICE_NAME, objectMapper);

        // when
        String name = provider.getProviderName();

        // then
        assertEquals("VCAP_SERVICES (User-Provided)", name);
    }
}