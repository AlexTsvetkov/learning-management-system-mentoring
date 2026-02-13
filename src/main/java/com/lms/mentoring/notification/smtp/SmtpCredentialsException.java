package com.lms.mentoring.notification.smtp;

/**
 * Exception thrown when SMTP credentials cannot be retrieved or are invalid.
 */
public class SmtpCredentialsException extends RuntimeException {
    
    public SmtpCredentialsException(String message) {
        super(message);
    }
    
    public SmtpCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}