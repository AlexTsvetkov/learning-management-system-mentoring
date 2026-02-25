package com.lms.mentoring.multitenancy.controller;

import com.lms.mentoring.multitenancy.dto.ApplicationInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing XSUAA application information.
 * Only accessible to users with ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/application-info")
@Profile("cloud")
@Tag(name = "Application Info", description = "XSUAA application information (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationInfoController {

    @Value("${vcap.services.lms-xsuaa.credentials.url:}")
    private String xsuaaUrl;

    @Value("${vcap.services.lms-xsuaa.credentials.clientid:}")
    private String clientId;

    @Value("${vcap.services.lms-xsuaa.credentials.clientsecret:}")
    private String clientSecret;

    @Value("${vcap.services.lms-xsuaa.credentials.xsappname:}")
    private String xsappname;

    @Value("${vcap.services.lms-xsuaa.credentials.identityzone:}")
    private String identityZone;

    @Value("${vcap.services.lms-xsuaa.credentials.tenantid:}")
    private String tenantId;

    @Operation(
        summary = "Get application info",
        description = "Returns XSUAA token URL, client ID, and client secret. Requires ADMIN role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application info retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<ApplicationInfoDto> getApplicationInfo() {
        log.info("Application info requested by admin user");
        
        String tokenUrl = xsuaaUrl + "/oauth/token";
        
        ApplicationInfoDto info = ApplicationInfoDto.builder()
                .tokenUrl(tokenUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .xsappname(xsappname)
                .identityZone(identityZone)
                .tenantId(tenantId)
                .build();
        
        log.debug("Returning application info for xsappname: {}", xsappname);
        return ResponseEntity.ok(info);
    }
}