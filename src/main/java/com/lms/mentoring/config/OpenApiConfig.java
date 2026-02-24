package com.lms.mentoring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 * 
 * Configures:
 * - API documentation metadata
 * - Basic Authentication security scheme for local development
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {
    
    private static final String BASIC_AUTH_SCHEME = "basicAuth";
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS Mentoring API")
                        .version("v1.0")
                        .description("Learning Management System API - Students & Courses\n\n" +
                                "## Authentication\n\n" +
                                "**Local Development:** Use Basic Auth\n" +
                                "- User: `user` / `password` (API access)\n" +
                                "- Manager: `manager` / `secret` (API + Actuator access)\n\n" +
                                "**Cloud (SAP BTP):** Use OAuth2 Bearer Token from XSUAA"))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Basic Authentication for local development.\n\n" +
                                        "**Credentials:**\n" +
                                        "- Username: `user`, Password: `password` (USER role)\n" +
                                        "- Username: `manager`, Password: `secret` (MANAGER role)")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
    }
}
