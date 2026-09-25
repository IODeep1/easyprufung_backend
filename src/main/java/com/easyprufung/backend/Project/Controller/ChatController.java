package com.easyprufung.backend.Project.Controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Project.Models.ChatMessage;
import com.easyprufung.backend.Project.Service.ChatService;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private SubscriptionService subscriptionService;


    @GetMapping(path = EndPoints.CHAT_PROJECT_HISTORY)
    public ResponseEntity<?> getProjectChatHistory(@RequestParam String uuid, @RequestParam String location, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var chatRequest = chatService.getProjectChatHistory(uuid, location);
                return ResponseEntity.ok(chatRequest);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException | JsonProcessingException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.CHAT_PROJECT_VALIDATION)
    public ResponseEntity<?> updateProjectValidation(@RequestBody ChatMessage chatMessage, @RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getIteration() == 0)
            {
                logger.error("INSUFFICIENT_ITERATION for "+ user.getEmail() +"Invalid try");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INSUFFICIENT_ITERATION");
            }
            if(subscription.getIteration() != -1){
                subscription.setIteration(subscription.getIteration()-1);
                subscriptionService.updateSubscription(subscription);
            }
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = chatService.updateProjectValidation(chatMessage, uuid);
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_UPDATE_PROJECT_VALIDATION");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_PROJECT_VALIDATION");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (IOException exception) {
            logger.error(exception.getMessage());
            exception.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping(path = EndPoints.CHAT_PROJECT_NAME)
    public ResponseEntity<?> updateProjectName(@RequestBody ChatMessage chatMessage, @RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getIteration() == 0)
            {
                logger.error("INSUFFICIENT_ITERATION for "+ user.getEmail() +"Invalid try");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INSUFFICIENT_ITERATION");
            }
            if(subscription.getIteration() != -1){
                subscription.setIteration(subscription.getIteration()-1);
                subscriptionService.updateSubscription(subscription);
            }
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = chatService.updateProjectName(chatMessage, uuid);
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_UPDATE_PROJECT_NAME");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_PROJECT_NAME");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (IOException exception) {
            logger.error(exception.getMessage());
            exception.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }


    /*@PostMapping(path = EndPoints.CHAT_PROJECT_ICON)
    public ResponseEntity<?> updateProjectIcon(@RequestBody ChatMessage chatMessage, @RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getIteration() == 0)
            {
                logger.error("INSUFFICIENT_ITERATION for "+ user.getEmail() +"Invalid try");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INSUFFICIENT_ITERATION");
            }
            if(subscription.getIteration() != -1){
                subscription.setIteration(subscription.getIteration()-1);
                subscriptionService.updateSubscription(subscription);
            }
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = chatService.updateProjectIcon(chatMessage, uuid);
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_UPDATE_PROJECT_ICON");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_PROJECT_ICON");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (IOException exception) {
            logger.error(exception.getMessage());
            exception.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }*/

    @PostMapping(path = EndPoints.CHAT_PROJECT_LANDINGPAGE)
    public ResponseEntity<?> updateProjectLandingPage(@RequestBody ChatMessage chatMessage, @RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getIteration() == 0)
            {
                logger.error("INSUFFICIENT_ITERATION for "+ user.getEmail() +"Invalid try");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INSUFFICIENT_ITERATION");
            }
            if(subscription.getIteration() != -1){
                subscription.setIteration(subscription.getIteration()-1);
                subscriptionService.updateSubscription(subscription);
            }
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = chatService.updateProjectLandingPage(chatMessage, uuid);
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_UPDATE_PROJECT_LANDINGPAGE");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_PROJECT_LANDINGPAGE");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (IOException exception) {
            logger.error(exception.getMessage());
            exception.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}


