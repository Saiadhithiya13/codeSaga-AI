package com.codesage.security;

import com.codesage.common.constants.AppConstants;
import com.codesage.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.DispatcherType;

/**
 * Spring Security Configuration for CodeSage AI.
 *
 * <p>Sprint 2 Architecture Spec:
 * <ul>
 *   <li>Stateless session policy (JWTs only)</li>
 *   <li>CSRF disabled (we use HttpOnly cookies with SameSite=Lax)</li>
 *   <li>CORS explicitly enabled and configured via {@link com.codesage.config.WebConfig}</li>
 *   <li>{@link JwtAuthenticationFilter} intercepts all requests</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ─── Basic Config ──────────────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ─── Authorization Rules ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Allow async and error dispatches for SseEmitter (SSE streaming)
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                // Public endpoints
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/api/v1/auth/github/callback").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers("/webhooks/github").permitAll()
                
                // OpenAPI / Swagger
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Actuator
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"" + authException.getMessage() + "\"}");
                })
            )

            // ─── Filters ──────────────────────────────────────────────────────
            // Insert JWT filter before the standard auth filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
