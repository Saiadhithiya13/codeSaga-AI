package com.codesage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.host:localhost}")
    private String host;

    @Value("${chroma.port:8000}")
    private String port;

    public String getChromaUrl() {
        return "http://" + host + ":" + port;
    }
}
