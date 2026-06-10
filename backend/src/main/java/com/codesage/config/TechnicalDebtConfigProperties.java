package com.codesage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.debt")
public class TechnicalDebtConfigProperties {
    private int largeFileThreshold = 300;
    private int deepNestingThreshold = 4;
    private int aiSamplePercentage = 10;
}
