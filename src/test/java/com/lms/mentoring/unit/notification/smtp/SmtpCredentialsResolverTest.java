package com.lms.mentoring.unit.notification.smtp;

import com.lms.mentoring.config.FeatureFlagsService;
import com.lms.mentoring.notification.smtp.DestinationSmtpCredentialsProvider;
import com.lms.mentoring.notification.smtp.SmtpCredentials;
import com.lms.mentoring.notification.smtp.SmtpCredentialsException;
import com.lms.mentoring.notification.smtp.SmtpCredentialsResolver;
import com.lms.mentoring.notification.smtp.VcapSmtpCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class SmtpCredentialsResolverTest {

    private FeatureFlagsService featureFlagsService;
    private VcapSmtpCredentialsProvider vcapProvider;
    private DestinationSmtpCredentialsProvider destinationProvider;
    private SmtpCredentialsResolver resolver;

    @BeforeEach
    void setUp() {
        featureFlagsService = mock(FeatureFlagsService.class);
        vcapProvider = mock(VcapSmtpCredentialsProvider.class);
        destinationProvider = mock(DestinationSmtpCredentialsProvider.class);
        resolver = new SmtpCredentialsResolver(featureFlagsService, vcapProvider, destinationProvider);

        when(vcapProvider.getProviderName()).thenReturn("VCAP");
        when(destinationProvider.getProviderName()).thenReturn("Destination");
    }

    @Test
    void resolveCredentials_WhenFeatureFlagDisabled_ShouldUseVcapProvider() {
        // given
        SmtpCredentials expectedCredentials = createCredentials("vcap-host.com");
        when(featureFlagsService.isUseDestinationSmtpEnabled()).thenReturn(false);
        when(vcapProvider.getCredentials()).thenReturn(expectedCredentials);

        // when
        SmtpCredentials result = resolver.resolveCredentials();

        // then
        assertEquals("vcap-host.com", result.getHost());
        verify(vcapProvider).getCredentials();
        verify(destinationProvider, never()).getCredentials();
    }

    @Test
    void resolveCredentials_WhenFeatureFlagEnabled_ShouldUseDestinationProvider() {
        // given
        SmtpCredentials expectedCredentials = createCredentials("destination-host.com");
        when(featureFlagsService.isUseDestinationSmtpEnabled()).thenReturn(true);
        when(destinationProvider.getCredentials()).thenReturn(expectedCredentials);

        // when
        SmtpCredentials result = resolver.resolveCredentials();

        // then
        assertEquals("destination-host.com", result.getHost());
        verify(destinationProvider).getCredentials();
        verify(vcapProvider, never()).getCredentials();
    }

    @Test
    void resolveCredentials_WhenPrimaryFails_ShouldFallbackToOther() {
        // given
        SmtpCredentials fallbackCredentials = createCredentials("fallback-host.com");
        when(featureFlagsService.isUseDestinationSmtpEnabled()).thenReturn(false);
        when(vcapProvider.getCredentials()).thenThrow(new SmtpCredentialsException("VCAP failed"));
        when(destinationProvider.getCredentials()).thenReturn(fallbackCredentials);

        // when
        SmtpCredentials result = resolver.resolveCredentials();

        // then
        assertEquals("fallback-host.com", result.getHost());
    }

    @Test
    void resolveCredentials_WhenBothFail_ShouldThrowOriginalException() {
        // given
        when(featureFlagsService.isUseDestinationSmtpEnabled()).thenReturn(false);
        when(vcapProvider.getCredentials()).thenThrow(new SmtpCredentialsException("Original error"));
        when(destinationProvider.getCredentials()).thenThrow(new SmtpCredentialsException("Fallback error"));

        // when
        SmtpCredentialsException exception = assertThrows(
                SmtpCredentialsException.class, () -> resolver.resolveCredentials());

        // then
        assertEquals("Original error", exception.getMessage());
    }

    private SmtpCredentials createCredentials(String host) {
        return SmtpCredentials.builder()
                .host(host)
                .port(587)
                .username("user@example.com")
                .password("secret")
                .build();
    }
}