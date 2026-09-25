package com.easyprufung.backend.AI.Models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRequest {

    private List<Message> messages = new ArrayList<>();
    private String model;
    private int top_p;
    private int max_completion_tokens;
    private boolean stream;
    private double temperature;
    private int frequency_penalty;
    private int presence_penalty;
    private String reasoning_effort;
    private String verbosity;



    public ChatRequest(){

    }
    public ChatRequest(String model, String systemprompt, String userprompt, double temperature, int maxtokens,Boolean enableStream) {
        this.model = model;
        this.temperature = temperature;
        this.top_p = 1;
        this.max_completion_tokens = maxtokens;
        this.stream = enableStream;
        List<Message.Content> usercontent = new ArrayList<>();
        usercontent.add(new Message.Content("text",userprompt));

        List<Message.Content> systemcontent = new ArrayList<>();
        systemcontent.add(new Message.Content("text",systemprompt));
        this.messages.add(new Message("system", systemcontent ));
        this.messages.add(new Message("user", usercontent ));
        this.reasoning_effort = "medium";
        this.verbosity = "medium";
        //this.prompt = prompt;
        //this.frequency_penalty = 0;
        //this.presence_penalty = 0;
    }
}
