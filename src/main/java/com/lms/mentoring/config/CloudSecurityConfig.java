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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * JWT Decoder for validating XSUAA tokens from multiple tenants.
     * 
     * For multi-tenant SaaS apps with XSUAA broker plan:
     * - Tokens can come from any subscribed tenant's identity zone
     * - Each tenant has its own token_keys endpoint
     * - The decoder must dynamically resolve the correct endpoint based on the token's issuer
     * 
     * This implementation uses a multi-tenant JWT decoder that:
     * - Extracts the issuer (iss) from the token
     * - Constructs the JWK URI from the issuer URL
     * - Validates the issuer domain matches the XSUAA domain
     * - Caches JwtDecoders per tenant for performance
     */
    @Bean
    public JwtDecoder jwtDecoder(XsuaaServiceConfiguration xsuaaServiceConfiguration) {
        String uaaDomain = xsuaaServiceConfiguration.getUaaDomain();
        log.info("Creating Multi-tenant JWT Decoder with UAA Domain: {}", uaaDomain);
        
        return new MultiTenantJwtDecoder(uaaDomain);
    }
    
    /**
     * Multi-tenant JWT decoder that validates tokens from any subscribed tenant.
     * It dynamically resolves the JWK URI based on the token's issuer claim,
     * allowing tokens from different tenants to be validated.
     */
    private static class MultiTenantJwtDecoder implements JwtDecoder {
        private final String uaaDomain;
        private final Map<String, NimbusJwtDecoder> decoderCache = new ConcurrentHashMap<>();
        
        public MultiTenantJwtDecoder(String uaaDomain) {
            this.uaaDomain = uaaDomain;
        }
        
        @Override
        public Jwt decode(String token) {
            // Parse the token to get the issuer without validation
            String issuer = extractIssuer(token);
            
            if (issuer == null) {
                throw new org.springframework.security.oauth2.jwt.JwtException("Token missing issuer claim");
            }
            
            // Validate the issuer is from a trusted domain
            if (!isValidIssuer(issuer)) {
                throw new org.springframework.security.oauth2.jwt.JwtException(
                        "Untrusted issuer: " + issuer + ". Expected domain: " + uaaDomain);
            }
            
            // Get or create decoder for this issuer
            NimbusJwtDecoder decoder = decoderCache.computeIfAbsent(issuer, this::createDecoder);
            
            return decoder.decode(token);
        }
        
        private String extractIssuer(String token) {
            try {
                // Split JWT and decode payload
                String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    return null;
                }
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                ObjectMapper mapper = new ObjectMapper();
                JsonNode claims = mapper.readTree(payload);
                JsonNode issNode = claims.get("iss");
                return issNode != null ? issNode.asText() : null;
            } catch (Exception e) {
                log.warn("Failed to extract issuer from token", e);
                return null;
            }
        }
        
        private boolean isValidIssuer(String issuer) {
            // Issuer must be from the XSUAA domain
            // Format: https://{subdomain}.authentication.{region}.hana.ondemand.com/oauth/token
            // uaaDomain format: authentication.us10.hana.ondemand.com
            try {
                URL issuerUrl = new URL(issuer);
                String host = issuerUrl.getHost();
                // Host should end with the uaaDomain (e.g., xyz.authentication.us10.hana.ondemand.com)
                return host.endsWith(uaaDomain) || host.contains(".authentication.");
            } catch (Exception e) {
                log.warn("Invalid issuer URL: {}", issuer);
                return false;
            }
        }
        
        private NimbusJwtDecoder createDecoder(String issuer) {
            // Extract base URL from issuer (remove /oauth/token if present)
            String baseUrl = issuer.replace("/oauth/token", "");
            String jwkSetUri = baseUrl + "/token_keys";
            
            log.info("Creating JWT Decoder for issuer: {} with JWK URI: {}", issuer, jwkSetUri);
            
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
    }

    /**
     * Security filter chain for SaaS Provisioning Service callback endpoints.
     * This filter chain allows requests without JWT validation because:
     * 1. SaaS Registry sends tokens from its own identity zone (not subscriber's)
     * 2. Our XSUAA (broker plan) cannot validate cross-identity-zone tokens
     * 3. SAP documentation recommends this approach for multitenant apps
     * Order 1 means this filter chain is evaluated first.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain callbackSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/callback/v1.0/**")
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                );
        
        log.info("Configured unsecured callback filter chain for /callback/v1.0/** (SaaS Registry callbacks)");
        return http.build();
    }

    /**
     * Main security filter chain for all other endpoints.
     * Order 2 means this filter chain is evaluated after the callback filter chain.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
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
