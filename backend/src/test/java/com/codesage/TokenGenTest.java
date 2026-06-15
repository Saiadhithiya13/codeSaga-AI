package com.codesage;

import com.codesage.security.jwt.JwtTokenProvider;
import com.codesage.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import java.util.UUID;

public class TokenGenTest {
    @Test
    public void generate() {
        JwtProperties props = new JwtProperties();
        props.setSecret(System.getenv("JWT_SECRET"));
        props.setAccessTokenExpiryMinutes(60L);
        JwtTokenProvider provider = new JwtTokenProvider(props);
        String token = provider.generateAccessToken(UUID.fromString("57db9884-ffe3-4515-8edb-9d2a07ed1598"), "Saiadhithiya13", "test@example.com", "USER");
        System.out.println("TEST_TOKEN: " + token);

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Cookie", "access_token=" + token);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(null, headers);

        System.out.println("Triggering indexing API...");
        org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:8081/api/v1/repositories/be230c8b-ddd1-4240-a7a5-8f078df801be/index",
                entity,
                String.class
        );
        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
    }
}
