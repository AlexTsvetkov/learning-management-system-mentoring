package com.lms.mentoring.multitenancy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing XSUAA application information.
 * Only accessible to users with ADMIN role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationInfoDto {
    
    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String xsappname;
    private String identityZone;
    private String tenantId;
}