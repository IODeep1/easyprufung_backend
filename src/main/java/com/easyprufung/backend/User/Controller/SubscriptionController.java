package com.easyprufung.backend.User.Controller;

import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.User.DTO.SubscriptionDTO;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final UserService userService;


    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping(path = EndPoints.SUBSCRIPTION_CREATE)
    public ResponseEntity<?> createSubscription(@RequestBody SubscriptionDTO subscriptionDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            User user = userService.getUserByEmail(subscriptionDTO.getCustomerEmail());
            if(user != null)
            {
                if(user.getSubscriptions().stream().count() == 0)
                {
                    Subscription newSubscription = subscriptionService.createSubscription(user.getUuid(), subscriptionDTO.getCustomerId(),subscriptionDTO.getCustomerEmail(),subscriptionDTO.getPlan(),subscriptionDTO.getType(),subscriptionDTO.getPriceId());
                    user.addSubscription(newSubscription);
                    userService.updateUser(user);
                }
                else {
                    var storedSubscription = user.getSubscriptions().stream().findFirst().get();
                    subscriptionService.updateSubscription(storedSubscription, subscriptionDTO.getCustomerId(),subscriptionDTO.getPlan(),subscriptionDTO.getType(),subscriptionDTO.getPriceId());
                }
            }
            return ResponseEntity.ok("success");
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.SUBSCRIPTION_LIST)
    public ResponseEntity<?> getSubscriptionList(Pageable pageable) {
        try
        {
            Page<Subscription> events = subscriptionService.getSubscriptions(pageable);
            return ResponseEntity.ok(events.getContent());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }
}
