package com.lms.mentoring.config;

import com.sap.cloud.security.xsuaa.XsuaaServiceConfiguration;
import com.sap.cloud.security.xsuaa.XsuaaServiceConfigurationDefault;
import com.sap.cloud.security.xsuaa.token.TokenAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for SAP BTP Cloud Foundry environment.
 * Uses XSUAA service for OAuth2/JWT token-based authentication.
 * 
 * This configuration is activated only when running with the 'cloud' profile.
 * 
 * Security roles:
 * - user: Basic access to API endpoints
 * - admin: Access to /api/v1/application-info endpoint
 * - Callback: SaaS Provisioning Service callback access
 */
@Configuration
@Profile("cloud")
@EnableMethodSecurity(prePostEnabled = true)
public class CloudSecurityConfig {

    @Value("${vcap.services.lms-xsuaa.credentials.url:}")
    private String xsuaaUrl;

    @Value("${vcap.services.lms-xsuaa.credentials.clientid:}")
    private String clientId;

    @Value("${vcap.services.lms-xsuaa.credentials.clientsecret:}")
    private String clientSecret;

    @Value("${vcap.services.lms-xsuaa.credentials.xsappname:}")
    private String xsAppName;

    /**
     * Creates the XsuaaServiceConfiguration bean from VCAP_SERVICES.
     */
    @Bean
    public XsuaaServiceConfiguration xsuaaServiceConfiguration() {
        XsuaaServiceConfigurationDefault config = new XsuaaServiceConfigurationDefault();
        // The configuration is automatically populated from VCAP_SERVICES
        // via the spring-xsuaa library
        return config;
    }

    /**
     * JWT Decoder for validating XSUAA tokens.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = xsuaaUrl + "/token_keys";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain cloudSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless session management for JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Disable CSRF for REST API
                .csrf(AbstractHttpConfigurer::disable)
                
                // Authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints - no authentication required
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        
                        // SaaS Provisioning Service callback endpoints - require Callback scope
                        // These endpoints are called by SAP BTP SaaS Provisioning Service
                        .requestMatchers("/callback/v1.0/**").hasAuthority("Callback")
                        
                        // Application info endpoint - requires admin scope (enforced via @PreAuthorize)
                        .requestMatchers("/api/v1/application-info/**").authenticated()
                        
                        // All other API endpoints require user scope
                        .requestMatchers("/api/**").hasAuthority("user")
                        
                        // Actuator endpoints require authentication
                        .requestMatchers("/actuator/**").authenticated()
                        
                        // All other requests are permitted
                        .anyRequest().permitAll()
                )
                
                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(getJwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * Creates a converter that extracts authorities from the XSUAA JWT token.
     * This allows Spring Security to use the scopes from the token for authorization.
     */
    private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter() {
        TokenAuthenticationConverter converter = new TokenAuthenticationConverter(xsuaaServiceConfiguration());
        // Extract authorities from scopes claim
        converter.setLocalScopeAsAuthorities(true);
        return converter;
    }
}