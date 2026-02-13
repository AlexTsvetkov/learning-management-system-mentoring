package com.lms.mentoring.notification.smtp;

/**
 * Strategy interface for retrieving SMTP credentials.
 * Implementations can fetch credentials from different sources:
 * - User-provided service (VCAP_SERVICES)
 * - Destination Service API
 * - Local configuration (application.yml)
 */
public interface SmtpCredentialsProvider {
    
    /**
     * Retrieves SMTP credentials from the underlying source.
     * @return SmtpCredentials containing host, port, username, password, and from address
     * @throws SmtpCredentialsException if credentials cannot be retrieved
     */
    SmtpCredentials getCredentials();
    
    /**
     * Returns the name of this provider for logging purposes.
     * @return provider name
     */
    String getProviderName();
}