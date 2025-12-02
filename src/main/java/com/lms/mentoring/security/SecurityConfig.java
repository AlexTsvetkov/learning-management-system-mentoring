package com.lms.mentoring.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Define an in-memory user store for Basic Auth
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
                // Disable CSRF for simpler API testing (common practice)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // 1. Actuator endpoints restricted to MANAGER role
                        // Use EndpointRequest.toAnyEndpoint() for Actuator paths
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("MANAGER")

                        // 2. All main API endpoints secured for any authenticated user
                        .requestMatchers("/api/**").authenticated()

                        // 3. All other requests are permitted (e.g., static content, root path)
                        .anyRequest().permitAll()
                )

                // Apply Basic Authentication for all secured paths
                .httpBasic(httpBasic -> httpBasic.init(http));

        return http.build();
    }
}