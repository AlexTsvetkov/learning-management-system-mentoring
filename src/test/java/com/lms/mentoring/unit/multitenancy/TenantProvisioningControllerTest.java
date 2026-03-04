package com.lms.mentoring.unit.multitenancy;

import com.lms.mentoring.multitenancy.TenantSchemaService;
import com.lms.mentoring.multitenancy.controller.TenantProvisioningController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantProvisioningController}.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TenantProvisioningControllerTest {

    private TenantProvisioningController controller;

    @Mock
    private TenantSchemaService tenantSchemaService;

    @BeforeEach
    void setUp() {
        controller = new TenantProvisioningController(tenantSchemaService);
        ReflectionTestUtils.setField(controller, "applicationUri", "test-app.cfapps.us10-001.hana.ondemand.com");
        ReflectionTestUtils.setField(controller, "approuterBaseUrl", "https://org-space-lms-approuter.cfapps.us10-001.hana.ondemand.com");
    }

    // ===========================================
    // GET /dependencies Tests
    // ===========================================

    @Test
    void getDependencies_WithDestinationXsappname_ShouldReturnDestinationDependency() {
        // given
        String destinationXsappname = "destination-xsappname!b12345";
        ReflectionTestUtils.setField(controller, "destinationXsappname", destinationXsappname);

        // when
        ResponseEntity<List<Map<String, Object>>> response = controller.getDependencies("test-tenant");

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        Map<String, Object> dependency = response.getBody().get(0);
        assertEquals(destinationXsappname, dependency.get("xsappname"));
    }

    @Test
    void getDependencies_WithoutDestinationXsappname_ShouldReturnEmptyList() {
        // given
        ReflectionTestUtils.setField(controller, "destinationXsappname", null);

        // when
        ResponseEntity<List<Map<String, Object>>> response = controller.getDependencies("test-tenant");

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getDependencies_WithBlankDestinationXsappname_ShouldReturnEmptyList() {
        // given
        ReflectionTestUtils.setField(controller, "destinationXsappname", "   ");

        // when
        ResponseEntity<List<Map<String, Object>>> response = controller.getDependencies(null);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // ===========================================
    // PUT /tenants/{tenantId} - Subscribe Tests
    // ===========================================

    @Test
    void onSubscribe_Success_ShouldCreateSchemaAndReturnTenantUrl() {
        // given
        String tenantId = "test-tenant-123";
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribedSubdomain", "test-subdomain");
        
        doNothing().when(tenantSchemaService).createTenantSchema(tenantId);

        // when
        ResponseEntity<String> response = controller.onSubscribe(tenantId, payload);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test-subdomain"));
        assertTrue(response.getBody().startsWith("https://"));
        verify(tenantSchemaService).createTenantSchema(tenantId);
    }

    @Test
    void onSubscribe_WithNullPayload_ShouldUseDefaultSubdomain() {
        // given
        String tenantId = "test-tenant-123";
        doNothing().when(tenantSchemaService).createTenantSchema(tenantId);

        // when
        ResponseEntity<String> response = controller.onSubscribe(tenantId, null);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("tenant"));
        verify(tenantSchemaService).createTenantSchema(tenantId);
    }

    @Test
    void onSubscribe_SchemaCreationFails_ShouldReturnInternalServerError() {
        // given
        String tenantId = "test-tenant-123";
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribedSubdomain", "test-subdomain");
        
        doThrow(new RuntimeException("Database error")).when(tenantSchemaService).createTenantSchema(tenantId);

        // when
        ResponseEntity<String> response = controller.onSubscribe(tenantId, payload);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to create tenant schema"));
    }

    // ===========================================
    // DELETE /tenants/{tenantId} - Unsubscribe Tests
    // ===========================================

    @Test
    void onUnsubscribe_Success_ShouldDropSchema() {
        // given
        String tenantId = "test-tenant-123";
        doNothing().when(tenantSchemaService).dropTenantSchema(tenantId);

        // when
        ResponseEntity<Void> response = controller.onUnsubscribe(tenantId);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantSchemaService).dropTenantSchema(tenantId);
    }

    @Test
    void onUnsubscribe_SchemaDropFails_ShouldStillReturnOk() {
        // given - unsubscription should succeed even if schema drop fails
        String tenantId = "test-tenant-123";
        doThrow(new RuntimeException("Schema does not exist")).when(tenantSchemaService).dropTenantSchema(tenantId);

        // when
        ResponseEntity<Void> response = controller.onUnsubscribe(tenantId);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantSchemaService).dropTenantSchema(tenantId);
    }

    // ===========================================
    // Tenant URL Building Tests
    // ===========================================

    @Test
    void onSubscribe_ShouldBuildCorrectTenantUrl() {
        // given
        String tenantId = "test-tenant-123";
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribedSubdomain", "my-company");
        
        doNothing().when(tenantSchemaService).createTenantSchema(tenantId);

        // when
        ResponseEntity<String> response = controller.onSubscribe(tenantId, payload);

        // then
        assertEquals("https://my-company.cfapps.us10-001.hana.ondemand.com", response.getBody());
    }

    @Test
    void onSubscribe_WithCustomApprouterUrl_ShouldExtractDomain() {
        // given
        ReflectionTestUtils.setField(controller, "approuterBaseUrl", 
                "https://custom-org-lms-approuter.cfapps.eu10.hana.ondemand.com");
        
        String tenantId = "test-tenant-123";
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribedSubdomain", "subscriber");
        
        doNothing().when(tenantSchemaService).createTenantSchema(tenantId);

        // when
        ResponseEntity<String> response = controller.onSubscribe(tenantId, payload);

        // then
        assertEquals("https://subscriber.cfapps.eu10.hana.ondemand.com", response.getBody());
    }
}