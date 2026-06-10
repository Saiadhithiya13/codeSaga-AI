package com.codesage.domain.repos.controller;

import com.codesage.config.properties.GitHubProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Tag(name = "Webhooks", description = "GitHub Webhook Receiver")
@Log4j2
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
public class WebhookController {

    private final GitHubProperties gitHubProperties;

    @Operation(summary = "GitHub Webhook Receiver", description = "Receives webhook events from GitHub, validates HMAC signature, and returns 200 OK")
    @PostMapping
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String payload,
            HttpServletRequest request) {

        log.debug("Received GitHub Webhook Event: {}", eventType);

        if (signature == null || signature.isEmpty()) {
            log.warn("Missing X-Hub-Signature-256 header in webhook request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }

        if (!isValidSignature(payload, signature)) {
            log.error("Invalid webhook signature detected. Possible tampering or incorrect secret.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        log.info("Successfully validated GitHub webhook event: {}", eventType);
        
        // Sprint 3: Return 200 OK immediately. Business logic will be added in later sprints.
        return ResponseEntity.ok("Event received");
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            // signatureHeader format: "sha256=..."
            if (!signatureHeader.startsWith("sha256=")) {
                return false;
            }

            String expectedSignature = "sha256=" + calculateHmacSha256(payload, gitHubProperties.getWebhookSecret());
            
            // Use constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
            
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    private String calculateHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
