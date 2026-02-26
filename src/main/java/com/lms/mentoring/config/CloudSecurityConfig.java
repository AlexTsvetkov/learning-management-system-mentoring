package com.lms.mentoring.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.security.config.CredentialType;
import com.sap.cloud.security.xsuaa.XsuaaServiceConfiguration;
import com.sap.cloud.security.xsuaa.token.TokenAuthenticationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 
 * For SaaS multitenancy:
 * - XSUAA must use 'broker' plan
 * - Callback scope is granted to SaaS Provisioning Service via grant-as-authority-to-apps
 */
@Configuration
@Profile("cloud")
@EnableMethodSecurity(prePostEnabled = true)
public class CloudSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudSecurityConfig.class);

    /**
     * Creates XsuaaServiceConfiguration bean that reads XSUAA credentials from VCAP_SERVICES.
     * This implementation manually parses VCAP_SERVICES to ensure proper reading of credentials.
     */
    @Bean
    public XsuaaServiceConfiguration xsuaaServiceConfiguration() {
        return new VcapXsuaaServiceConfiguration();
    }

    /**
     * JWT Decoder for validating XSUAA tokens.
     * Uses the XSUAA service configuration to get the JWK set URI.
     */
    @Bean
    public JwtDecoder jwtDecoder(XsuaaServiceConfiguration xsuaaServiceConfiguration) {
        String uaaUrl = xsuaaServiceConfiguration.getUaaUrl();
        log.info("Creating JWT Decoder with UAA URL: {}", uaaUrl);
        
        if (uaaUrl == null || uaaUrl.isBlank()) {
            throw new IllegalStateException("XSUAA URL is not configured. Check VCAP_SERVICES binding.");
        }
        
        String jwkSetUri = uaaUrl + "/token_keys";
        log.info("JWK Set URI: {}", jwkSetUri);
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain cloudSecurityFilterChain(HttpSecurity http, 
                                                         XsuaaServiceConfiguration xsuaaServiceConfiguration) throws Exception {
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
                                .decoder(jwtDecoder(xsuaaServiceConfiguration))
                                .jwtAuthenticationConverter(getJwtAuthenticationConverter(xsuaaServiceConfiguration))
                        )
                );

        return http.build();
    }

    /**
     * Creates a converter that extracts authorities from the XSUAA JWT token.
     * This allows Spring Security to use the scopes from the token for authorization.
     */
    private Converter<Jwt, AbstractAuthenticationToken> getJwtAuthenticationConverter(
            XsuaaServiceConfiguration xsuaaServiceConfiguration) {
        TokenAuthenticationConverter converter = new TokenAuthenticationConverter(xsuaaServiceConfiguration);
        // Extract authorities from scopes claim
        converter.setLocalScopeAsAuthorities(true);
        return converter;
    }

    /**
     * Custom XsuaaServiceConfiguration that reads from VCAP_SERVICES environment variable.
     * This ensures proper parsing of XSUAA credentials on Cloud Foundry.
     */
    private static class VcapXsuaaServiceConfiguration implements XsuaaServiceConfiguration {
        private final String url;
        private final String clientId;
        private final String clientSecret;
        private final String xsAppName;
        private final String uaaDomain;
        private final String verificationKey;

        public VcapXsuaaServiceConfiguration() {
            String vcapServices = System.getenv("VCAP_SERVICES");
            log.info("Reading XSUAA configuration from VCAP_SERVICES");
            
            if (vcapServices == null || vcapServices.isBlank()) {
                log.warn("VCAP_SERVICES environment variable is not set");
                this.url = null;
                this.clientId = null;
                this.clientSecret = null;
                this.xsAppName = null;
                this.uaaDomain = null;
                this.verificationKey = null;
                return;
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode vcapNode = mapper.readTree(vcapServices);
                JsonNode xsuaaArray = vcapNode.get("xsuaa");
                
                if (xsuaaArray == null || !xsuaaArray.isArray() || xsuaaArray.isEmpty()) {
                    log.warn("No XSUAA service found in VCAP_SERVICES");
                    this.url = null;
                    this.clientId = null;
                    this.clientSecret = null;
                    this.xsAppName = null;
                    this.uaaDomain = null;
                    this.verificationKey = null;
                    return;
                }

                JsonNode xsuaaService = xsuaaArray.get(0);
                JsonNode credentials = xsuaaService.get("credentials");
                
                if (credentials == null) {
                    log.warn("No credentials found in XSUAA service");
                    this.url = null;
                    this.clientId = null;
                    this.clientSecret = null;
                    this.xsAppName = null;
                    this.uaaDomain = null;
                    this.verificationKey = null;
                    return;
                }

                this.url = getTextValue(credentials, "url");
                this.clientId = getTextValue(credentials, "clientid");
                this.clientSecret = getTextValue(credentials, "clientsecret");
                this.xsAppName = getTextValue(credentials, "xsappname");
                this.uaaDomain = getTextValue(credentials, "uaadomain");
                this.verificationKey = getTextValue(credentials, "verificationkey");
                
                log.info("XSUAA Configuration loaded - URL: {}, ClientID: {}, AppName: {}", 
                        this.url, this.clientId, this.xsAppName);
                
            } catch (Exception e) {
                log.error("Failed to parse VCAP_SERVICES for XSUAA configuration", e);
                throw new IllegalStateException("Failed to parse XSUAA configuration from VCAP_SERVICES", e);
            }
        }

        private String getTextValue(JsonNode node, String field) {
            JsonNode fieldNode = node.get(field);
            return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }

        @Override
        public String getUaaUrl() {
            return url;
        }

        @Override
        public String getAppId() {
            return xsAppName;
        }

        @Override
        public String getUaaDomain() {
            return uaaDomain;
        }

        @Override
        public String getVerificationKey() {
            return verificationKey;
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.BINDING_SECRET;
        }
    }
}
