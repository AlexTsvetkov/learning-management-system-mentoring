package com.lms.mentoring.notification.smtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class holding SMTP server credentials.
 * Used for dynamically configuring the mail sender.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpCredentials {
    
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    
    /**
     * Validates that all required fields are present.
     * @return true if credentials are valid
     */
    public boolean isValid() {
        return host != null && !host.isBlank()
                && port > 0
                && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}