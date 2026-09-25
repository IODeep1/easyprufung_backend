package com.easyprufung.backend.PromoCode.Controller;

import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.PromoCode.PromoCode;
import com.easyprufung.backend.PromoCode.Service.PromoCodeService;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class PromoCodeController {

    private final UserService userService;
    private final PromoCodeService promoCodeService;
    private final SubscriptionService subscriptionService;

    @PostMapping(path = EndPoints.PROMO_CODE_ACTIVATE)
    public ResponseEntity<?> activateCode(@RequestBody String code, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            String response = "INVALID";
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            if(user != null)
            {
                if(user.getSubscriptions().stream().count() != 0) {
                    var currentSubscription = user.getSubscriptions().stream().findFirst().get();
                    if(currentSubscription.getPlan().equals("free"))
                        user.getSubscriptions().remove(currentSubscription);
                    else{
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "FAILED_TO_ACTIVATE_CODE");
                    }
                }
                if(code.equalsIgnoreCase("starter4next") || code.equalsIgnoreCase("aico2025"))
                {
                    Subscription newSubscription = subscriptionService.createTesterSubscription(user.getUuid(), user.getEmail(), 2);
                    user.addSubscription(newSubscription);
                    userService.updateUser(user);
                    userService.sendNewSubscriptionEmail(user);
                    response = "ACTIVATED";
                }
                else
                {
                    PromoCode promoCode = promoCodeService.activateAppSumoPromoCode(code);
                    if(promoCode != null)
                    {
                        Subscription newSubscription = subscriptionService.createAppSumoSubscription(user.getUuid(), String.valueOf(user.getId()),user.getEmail());
                        user.addSubscription(newSubscription);
                        userService.updateUser(user);
                        userService.sendNewSubscriptionEmail(user);
                        response = "ACTIVATED";
                    }
                }
            }
            return ResponseEntity.ok(response);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }
}
