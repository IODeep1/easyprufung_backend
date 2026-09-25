package com.easyprufung.backend.AI.Models;

import lombok.Data;

@Data
public class CustomChatResponse {
    private String step;
    private String content;
    private Object system_content;

}
