package com.codesage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger documentation configuration.
 *
 * <p>Architecture Spec §API Standards: OpenAPI documentation is required.
 * Accessible at {@code /swagger-ui.html} and raw spec at {@code /v3/api-docs}.
 *
 * <p>A Bearer token security scheme is declared here so it shows in the UI
 * immediately, ready for use when JWT auth is added in Sprint 2.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .servers(buildServers())
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info buildInfo() {
        return new Info()
                .title("CodeSage AI API")
                .description("""
                        AI-powered Developer Intelligence Platform.
                        
                        Provides APIs for repository analytics, AI code chat (RAG),
                        pull request review, technical debt analysis, and security scanning.
                        
                        **Architecture**: Modular Monolith | **Base Path**: /api/v1
                        """)
                .version(appVersion)
                .contact(new Contact()
                        .name("CodeSage AI Team")
                        .email("api@codesage.ai"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://codesage.ai/license"));
    }

    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.codesage.ai")
                        .description("Production Server")
        );
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained via GitHub OAuth. Added in Sprint 2.")
                );
    }
}
