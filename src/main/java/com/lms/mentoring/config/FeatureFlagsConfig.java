package com.lms.mentoring.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for SAP BTP Feature Flags Service.
 * The Feature Flags service is accessed via REST API in cloud environment.
 * Service binding provides credentials via VCAP_SERVICES.
 */
@Configuration
@ConfigurationProperties(prefix = "sap.feature-flags")
@Profile("cloud")
@Getter
@Setter
@Slf4j
public class FeatureFlagsConfig {

    private Service service = new Service();

    @Getter
    @Setter
    public static class Service {
        private String name = "lms-feature-flags";
    }
}