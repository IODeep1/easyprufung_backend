package com.easyprufung.backend.AI.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {

    private List<Choice> choices;
    @Data
    public static class Choice {

        private int index;

        @JsonProperty("message")
        private ResultMessage message;
    }
}