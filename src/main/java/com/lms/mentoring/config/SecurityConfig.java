package com.lms.mentoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for local development and non-cloud environments.
 * Uses Basic Authentication with in-memory users.
 * 
 * This configuration is NOT activated when running with the 'cloud' profile.
 * For cloud deployment, see {@link CloudSecurityConfig} which uses XSUAA OAuth2.
 */
@Configuration
@Profile("!cloud")
public class SecurityConfig {

    /**
     * Define an in-memory user store for Basic Auth (local development only).
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        // Create a standard user
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

        // Create a user with the MANAGER role
        UserDetails manager = User.withDefaultPasswordEncoder()
                .username("manager")
                .password("secret")
                .roles("USER", "MANAGER") // MANAGER role needed for Actuator
                .build();

        return new InMemoryUserDetailsManager(user, manager);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1. Crucial for API/Basic Auth: tell Spring not to create HTTP sessions
                // This prevents redirection/session state issues that cause hangs.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 2. Disable CSRF (good for API)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Configure authorization rules (most specific first)
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // All main API endpoints secured for any authenticated user
                        .requestMatchers("/api/**").authenticated()

                        // Actuator endpoints restricted to MANAGER role
                        .requestMatchers("/actuator/**").hasRole("MANAGER")

                        // All other requests are permitted
                        .anyRequest().permitAll()
                )

                // 4. Apply Basic Authentication
                .httpBasic(httpBasic -> {})
                
                // 5. Allow H2 console frames (for local development)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}