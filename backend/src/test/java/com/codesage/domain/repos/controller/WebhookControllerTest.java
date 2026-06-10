package com.codesage.domain.repos.controller;

import com.codesage.config.properties.GitHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private GitHubProperties gitHubProperties;

    @InjectMocks
    private WebhookController webhookController;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Test
    void handleGitHubWebhook_MissingSignature_ReturnsUnauthorized() {
        ResponseEntity<String> response = webhookController.handleGitHubWebhook(null, "push", "{}", request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Missing signature", response.getBody());
    }

    @Test
    void handleGitHubWebhook_InvalidSignature_ReturnsUnauthorized() {
        when(gitHubProperties.getWebhookSecret()).thenReturn("my-secret");

        ResponseEntity<String> response = webhookController.handleGitHubWebhook("sha256=invalid", "push", "{}", request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid signature", response.getBody());
    }

    @Test
    void handleGitHubWebhook_ValidSignature_ReturnsOk() throws Exception {
        when(gitHubProperties.getWebhookSecret()).thenReturn("test-secret");
        
        String payload = "{\"action\":\"opened\"}";
        // Calculated HMAC-SHA256 of payload with "test-secret"
        // Echo -n '{"action":"opened"}' | openssl dgst -sha256 -hmac "test-secret"
        String validSignature = "sha256=b7df3c961e050cebebc32cc62ab117bfdcd5dd9a28cebc1ff35148386de85e49";

        ResponseEntity<String> response = webhookController.handleGitHubWebhook(validSignature, "push", payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Event received", response.getBody());
    }
}
