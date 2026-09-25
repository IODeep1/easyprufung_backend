package com.easyprufung.backend.Project.Models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
    private String sender;
    private String step;
    private String content;
    private String system_content;

    public ChatMessage() {
    }
}
