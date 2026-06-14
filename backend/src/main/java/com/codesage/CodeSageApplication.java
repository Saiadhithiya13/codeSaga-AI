package com.codesage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CodeSage AI — Application Entry Point.
 *
 * <p>Modular Monolith organized under {@code com.codesage.domain.*} modules.
 * Virtual threads are enabled via {@code spring.threads.virtual.enabled=true}.
 * {@code @ConfigurationPropertiesScan} auto-discovers all
 * {@code @ConfigurationProperties} beans in {@code com.codesage}.
 */
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan("com.codesage")
@EnableAsync
@EnableScheduling
public class CodeSageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeSageApplication.class, args);
    }
}
