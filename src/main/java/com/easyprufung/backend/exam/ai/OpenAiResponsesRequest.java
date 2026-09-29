package com.easyprufung.backend.exam.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiResponsesRequest {
    private String model;
    private String instructions;
    private String input;
    private TextConfiguration text;
    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;
    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;
    private Boolean store;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextConfiguration {
        private JsonSchemaFormat format;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonSchemaFormat {
        private String type;
        private String name;
        private boolean strict;
        private JsonNode schema;
    }
}
