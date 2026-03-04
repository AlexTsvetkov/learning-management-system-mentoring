package com.lms.mentoring.unit.notification.smtp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.mentoring.multitenancy.TenantContext;
import com.lms.mentoring.notification.smtp.SmtpCredentialsException;
import com.lms.mentoring.notification.smtp.TenantAwareDestinationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantAwareDestinationService}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TenantAwareDestinationServiceTest {

    private TenantAwareDestinationService service;
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    private static final String DESTINATION_URI = "https://destination.cfapps.us10.hana.ondemand.com";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String TOKEN_URL = "https://test.authentication.us10.hana.ondemand.com";
    private static final String PROVIDER_TENANT_ID = "provider-tenant-123";
    private static final String SUBSCRIBER_TENANT_ID = "subscriber-tenant-456";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TenantAwareDestinationService(
                DESTINATION_URI,
                CLIENT_ID,
                CLIENT_SECRET,
                TOKEN_URL,
                PROVIDER_TENANT_ID,
                objectMapper
        );
        // Inject mocked RestTemplate
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getDestination_ProviderTenant_ShouldFetchFromProvider() throws Exception {
        // given
        TenantContext.setCurrentTenant(PROVIDER_TENANT_ID);
        String destinationName = "lms-smtp";
        
        // Mock token response
        String tokenResponse = "{\"access_token\": \"test-token\", \"expires_in\": 3600}";
        when(restTemplate.exchange(
                eq(TOKEN_URL + "/oauth/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        
        // Mock destination response
        String destinationResponse = """
            {
                "destinationConfiguration": {
                    "Name": "lms-smtp",
                    "mail.smtp.host": "smtp.example.com",
                    "mail.smtp.port": "587"
                }
            }
            """;
        when(restTemplate.exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(destinationResponse, HttpStatus.OK));

        // when
        JsonNode result = service.getDestination(destinationName);

        // then
        assertNotNull(result);
        assertEquals("lms-smtp", result.path("destinationConfiguration").path("Name").asText());
    }

    @Test
    void getDestination_SubscriberTenant_ShouldTrySubscriberFirst() throws Exception {
        // given
        TenantContext.setCurrentTenant(SUBSCRIBER_TENANT_ID);
        String destinationName = "lms-smtp";
        
        // Mock token response
        String tokenResponse = "{\"access_token\": \"test-token\", \"expires_in\": 3600}";
        when(restTemplate.exchange(
                eq(TOKEN_URL + "/oauth/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        
        // Mock destination response for subscriber
        String destinationResponse = """
            {
                "destinationConfiguration": {
                    "Name": "lms-smtp",
                    "mail.smtp.host": "subscriber-smtp.example.com"
                }
            }
            """;
        when(restTemplate.exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(destinationResponse, HttpStatus.OK));

        // when
        JsonNode result = service.getDestination(destinationName);

        // then
        assertNotNull(result);
        assertEquals("subscriber-smtp.example.com", 
                result.path("destinationConfiguration").path("mail.smtp.host").asText());
    }

    @Test
    void getDestination_SubscriberNotFound_ShouldFallbackToProvider() throws Exception {
        // given
        TenantContext.setCurrentTenant(SUBSCRIBER_TENANT_ID);
        String destinationName = "lms-smtp";
        
        // Mock token response
        String tokenResponse = "{\"access_token\": \"test-token\", \"expires_in\": 3600}";
        when(restTemplate.exchange(
                eq(TOKEN_URL + "/oauth/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        
        // First call (subscriber) - returns 404
        // Second call (provider) - returns destination
        String providerDestinationResponse = """
            {
                "destinationConfiguration": {
                    "Name": "lms-smtp",
                    "mail.smtp.host": "provider-smtp.example.com"
                }
            }
            """;
        
        when(restTemplate.exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        ))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null))
        .thenReturn(new ResponseEntity<>(providerDestinationResponse, HttpStatus.OK));

        // when
        JsonNode result = service.getDestination(destinationName);

        // then
        assertNotNull(result);
        assertEquals("provider-smtp.example.com", 
                result.path("destinationConfiguration").path("mail.smtp.host").asText());
        
        // Verify it was called twice (subscriber then provider)
        verify(restTemplate, times(2)).exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void getDestination_DefaultTenant_ShouldFetchFromProvider() throws Exception {
        // given - DEFAULT_TENANT (PROVIDER)
        TenantContext.clear(); // Will use DEFAULT_TENANT
        String destinationName = "lms-smtp";
        
        // Mock token response
        String tokenResponse = "{\"access_token\": \"test-token\", \"expires_in\": 3600}";
        when(restTemplate.exchange(
                eq(TOKEN_URL + "/oauth/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        
        // Mock destination response
        String destinationResponse = """
            {
                "destinationConfiguration": {
                    "Name": "lms-smtp",
                    "mail.smtp.host": "smtp.example.com"
                }
            }
            """;
        when(restTemplate.exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(destinationResponse, HttpStatus.OK));

        // when
        JsonNode result = service.getDestination(destinationName);

        // then
        assertNotNull(result);
        
        // Should only be called once (provider only)
        verify(restTemplate, times(1)).exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void getDestination_NotFoundInBoth_ShouldReturnNull() throws Exception {
        // given
        TenantContext.setCurrentTenant(SUBSCRIBER_TENANT_ID);
        String destinationName = "non-existent";
        
        // Mock token response
        String tokenResponse = "{\"access_token\": \"test-token\", \"expires_in\": 3600}";
        when(restTemplate.exchange(
                eq(TOKEN_URL + "/oauth/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        
        // Both subscriber and provider return 404
        when(restTemplate.exchange(
                eq(DESTINATION_URI + "/destination-configuration/v1/destinations/" + destinationName),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // when
        JsonNode result = service.getDestination(destinationName);

        // then
        assertNull(result);
    }

    @Test
    void getDestination_MissingConfiguration_ShouldThrowException() {
        // given
        TenantAwareDestinationService serviceWithoutUri = new TenantAwareDestinationService(
                null, // Missing URI
                CLIENT_ID,
                CLIENT_SECRET,
                TOKEN_URL,
                PROVIDER_TENANT_ID,
                objectMapper
        );

        // when/then
        assertThrows(SmtpCredentialsException.class, 
                () -> serviceWithoutUri.getDestination("lms-smtp"));
    }
}