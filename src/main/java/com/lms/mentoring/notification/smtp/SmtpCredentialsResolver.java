package com.lms.mentoring.notification.smtp;

import com.lms.mentoring.config.FeatureFlagsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Resolver that selects the appropriate SMTP credentials provider based on feature flag.
 * 
 * Strategy:
 * - When feature flag 'use-destination-smtp' is DISABLED (default): Use VCAP (User-Provided Service)
 * - When feature flag 'use-destination-smtp' is ENABLED: Use Destination Service
 * 
 * This allows runtime switching between credential sources without code changes or redeployment.
 */
@Component
@Profile("cloud")
@Slf4j
public class SmtpCredentialsResolver {
    
    private final FeatureFlagsService featureFlagsService;
    private final VcapSmtpCredentialsProvider vcapProvider;
    private final DestinationSmtpCredentialsProvider destinationProvider;
    
    @Autowired
    public SmtpCredentialsResolver(
            FeatureFlagsService featureFlagsService,
            VcapSmtpCredentialsProvider vcapProvider,
            DestinationSmtpCredentialsProvider destinationProvider) {
        this.featureFlagsService = featureFlagsService;
        this.vcapProvider = vcapProvider;
        this.destinationProvider = destinationProvider;
    }
    
    /**
     * Resolves SMTP credentials using the appropriate provider based on feature flag.
     * 
     * @return SMTP credentials from the selected provider
     * @throws SmtpCredentialsException if credentials cannot be retrieved
     */
    public SmtpCredentials resolveCredentials() {
        boolean useDestination = featureFlagsService.isUseDestinationSmtpEnabled();
        
        SmtpCredentialsProvider provider = useDestination ? destinationProvider : vcapProvider;
        
        log.info("Using SMTP credentials provider: {} (feature flag use-destination-smtp={})", 
                provider.getProviderName(), useDestination);
        
        try {
            return provider.getCredentials();
        } catch (SmtpCredentialsException e) {
            log.error("Failed to get credentials from {}: {}", provider.getProviderName(), e.getMessage());
            
            // Try fallback to other provider
            SmtpCredentialsProvider fallbackProvider = useDestination ? vcapProvider : destinationProvider;
            log.warn("Attempting fallback to {}", fallbackProvider.getProviderName());
            
            try {
                SmtpCredentials fallbackCredentials = fallbackProvider.getCredentials();
                log.info("Successfully retrieved credentials from fallback provider: {}", 
                        fallbackProvider.getProviderName());
                return fallbackCredentials;
            } catch (SmtpCredentialsException fallbackEx) {
                log.error("Fallback provider also failed: {}", fallbackEx.getMessage());
                throw e; // Throw original exception
            }
        }
    }
    
    /**
     * Gets the currently active provider name for diagnostics.
     * 
     * @return the name of the provider that would be used
     */
    public String getActiveProviderName() {
        boolean useDestination = featureFlagsService.isUseDestinationSmtpEnabled();
        return useDestination ? destinationProvider.getProviderName() : vcapProvider.getProviderName();
    }
}