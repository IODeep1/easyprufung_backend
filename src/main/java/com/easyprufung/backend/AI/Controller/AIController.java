package com.easyprufung.backend.AI.Controller;

import com.easyprufung.backend.AI.Models.ChatRequest;
import com.easyprufung.backend.AI.Service.OpenAIService;
import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Project.Controller.ProjectController;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping
public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private OpenAIService openAIService;

    /*@PostMapping(path = EndPoints.AI_CHAT)
    public SseEmitter liveChat(@RequestBody InternalChatRequest request) {
        try {
            SseEmitter emitter = new SseEmitter();
            String userMessage = request.getUserMessage();
            ChatRequest body = null;
            switch (request.getStep()){
                case  "initial_prompt":
                    body = openAIService.getInitialProjectFeedBack(userMessage);
            }
            openAIService.webClient.post()
                    .uri("")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(openAIService.aiHeader, openAIService.aiKey)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 400, clientResponse -> {
                        // Handle 400 status
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    // Log the error or handle it accordingly
                                    System.out.println("Bad Request: " + clientResponse.statusCode() + " " + errorBody);
                                    return Mono.error(new RuntimeException("Bad Request: " + errorBody)); // Throw error
                                });
                    })
                    .bodyToFlux(String.class)
                    .doOnNext(chunk -> {
                        try {
                            emitter.send(chunk); // Streaming each chunk to the client
                        } catch (Exception e) {
                            emitter.completeWithError(e); // Handle error if streaming fails
                        }
                    })
                    .doOnTerminate(() -> emitter.complete()) // When the stream finishes, complete the emitter
                    .doOnError(throwable -> emitter.completeWithError(throwable)) // Handle errors
                    .subscribe(); // Start streaming asynchronously
            return emitter;
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }*/

}
