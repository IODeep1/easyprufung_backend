package com.easyprufung.backend.exam.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiResponsesResponse {
    private String id;
    private String status;
    private List<OutputItem> output = new ArrayList<>();
    @JsonProperty("incomplete_details")
    private IncompleteDetails incompleteDetails;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutputItem {
        private String type;
        private String role;
        private String status;
        private List<ContentItem> content = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentItem {
        private String type;
        private String text;
        private String refusal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncompleteDetails {
        private String reason;
    }
}
