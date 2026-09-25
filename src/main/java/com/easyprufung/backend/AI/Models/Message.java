package com.easyprufung.backend.AI.Models;

import lombok.Data;

import java.util.List;

@Data
public class Message {

    private String role;
    private List<Content> content;

    public Message(){

    }
    public Message(String user, List<Content> content) {
        this.role = user;
        this.content = content;
    }

    @Data
    public static class Content {
        private String type;
        private String text;

        public Content(){

        }
        public Content(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}