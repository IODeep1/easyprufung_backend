package com.easyprufung.backend.exam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "easyprufung.ai")
public class AiProperties {
    private boolean enabled;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4o-mini";
    private String responsesPath = "/responses";
    private String organization;
    private String project;
    private int maxOutputTokens = 4000;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(120);
}
