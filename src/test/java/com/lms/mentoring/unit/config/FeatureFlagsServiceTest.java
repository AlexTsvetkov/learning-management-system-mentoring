package com.lms.mentoring.unit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.mentoring.config.FeatureFlagsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeatureFlagsService.
 * 
 * This service interacts with SAP BTP Feature Flags Service to control
 * application behavior at runtime. Tests cover:
 * - Flag evaluation when service is available
 * - Fallback to default value when service unavailable
 * - Handling of various error conditions
 * - Specific flag checks (use-destination-smtp)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagsService Tests")
class FeatureFlagsServiceTest {

    private static final String TEST_URI = "https://feature-flags.example.com";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-password";
    private static final String TEST_FLAG_NAME = "test-flag";

    @Mock
    private RestTemplate mockRestTemplate;

    private ObjectMapper objectMapper;
    private FeatureFlagsService featureFlagsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        featureFlagsService = new FeatureFlagsService(
                TEST_URI,
                TEST_USERNAME,
                TEST_PASSWORD,
                objectMapper
        );
        // Inject mock RestTemplate
        ReflectionTestUtils.setField(featureFlagsService, "restTemplate", mockRestTemplate);
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("should return true when flag is enabled")
        void shouldReturnTrueWhenFlagEnabled() {
            // Given
            String responseBody = "{\"variation\": true}";
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, false);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when flag is disabled")
        void shouldReturnFalseWhenFlagDisabled() {
            // Given
            String responseBody = "{\"variation\": false}";
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, true);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return default value when response is not successful")
        void shouldReturnDefaultWhenResponseNotSuccessful() {
            // Given
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, true);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return default value when response body is null")
        void shouldReturnDefaultWhenBodyNull() {
            // Given
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, false);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return default value when REST call throws exception")
        void shouldReturnDefaultOnException() {
            // Given
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, true);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return default value when JSON parsing fails")
        void shouldReturnDefaultOnInvalidJson() {
            // Given
            String invalidJson = "not-valid-json";
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(invalidJson, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, false);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return default when variation field is missing")
        void shouldReturnDefaultWhenVariationMissing() {
            // Given
            String responseBody = "{\"id\": \"test-flag\", \"value\": true}";
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isEnabled(TEST_FLAG_NAME, true);

            // Then
            // When "variation" field is missing, asBoolean returns the default
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("should return default when URI is not configured")
        void shouldReturnDefaultWhenUriNull() {
            // Given
            FeatureFlagsService serviceWithNullUri = new FeatureFlagsService(
                    null, TEST_USERNAME, TEST_PASSWORD, objectMapper);

            // When
            boolean result = serviceWithNullUri.isEnabled(TEST_FLAG_NAME, true);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return default when URI is blank")
        void shouldReturnDefaultWhenUriBlank() {
            // Given
            FeatureFlagsService serviceWithBlankUri = new FeatureFlagsService(
                    "  ", TEST_USERNAME, TEST_PASSWORD, objectMapper);

            // When
            boolean result = serviceWithBlankUri.isEnabled(TEST_FLAG_NAME, false);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return default when username is not configured")
        void shouldReturnDefaultWhenUsernameNull() {
            // Given
            FeatureFlagsService serviceWithNullUsername = new FeatureFlagsService(
                    TEST_URI, null, TEST_PASSWORD, objectMapper);

            // When
            boolean result = serviceWithNullUsername.isEnabled(TEST_FLAG_NAME, true);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return default when password is not configured")
        void shouldReturnDefaultWhenPasswordNull() {
            // Given
            FeatureFlagsService serviceWithNullPassword = new FeatureFlagsService(
                    TEST_URI, TEST_USERNAME, null, objectMapper);

            // When
            boolean result = serviceWithNullPassword.isEnabled(TEST_FLAG_NAME, false);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isUseDestinationSmtpEnabled")
    class IsUseDestinationSmtpEnabled {

        @Test
        @DisplayName("should check use-destination-smtp flag")
        void shouldCheckUseDestinationSmtpFlag() {
            // Given
            String responseBody = "{\"variation\": true}";
            when(mockRestTemplate.exchange(
                    eq(TEST_URI + "/api/v2/evaluate/" + FeatureFlagsService.FLAG_USE_DESTINATION_SMTP),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            boolean result = featureFlagsService.isUseDestinationSmtpEnabled();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false by default when flag evaluation fails")
        void shouldReturnFalseByDefaultWhenEvaluationFails() {
            // Given
            when(mockRestTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("Service unavailable"));

            // When
            boolean result = featureFlagsService.isUseDestinationSmtpEnabled();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("FLAG_USE_DESTINATION_SMTP constant should have correct value")
        void flagConstantShouldHaveCorrectValue() {
            assertThat(FeatureFlagsService.FLAG_USE_DESTINATION_SMTP).isEqualTo("use-destination-smtp");
        }
    }

    @Nested
    @DisplayName("URL Construction")
    class UrlConstruction {

        @Test
        @DisplayName("should construct correct evaluation URL")
        void shouldConstructCorrectEvaluationUrl() {
            // Given
            String responseBody = "{\"variation\": true}";
            String expectedUrl = TEST_URI + "/api/v2/evaluate/" + TEST_FLAG_NAME;
            
            when(mockRestTemplate.exchange(
                    eq(expectedUrl),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            featureFlagsService.isEnabled(TEST_FLAG_NAME, false);

            // Then - verification that the correct URL was called is implicit in the mock setup
        }
    }
}