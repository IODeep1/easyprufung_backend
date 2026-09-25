package com.easyprufung.backend.Stripe;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.User;
import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionRetrieveParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor

public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    UserService userService;
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    private Environment env;

    @PostMapping("/public/stripe/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, HttpServletRequest request) {
        String api_secret = env.getProperty("easyprufung.stripe.apisecret");
        String webHookSecret = env.getProperty("easyprufung.stripe.webhooksecret");
        String priceIdStarter = env.getProperty("easyprufung.stripe.priceidstarter");
        String priceIdAutopilot = env.getProperty("easyprufung.stripe.priceidautopilot");
        String creditIdStarter = env.getProperty("easyprufung.stripe.creditidstarter");
        String creditIdBuilder = env.getProperty("easyprufung.stripe.creditidbuilder");
        String creditIdPro = env.getProperty("easyprufung.stripe.creditidpro");

        String customerId = "";
        String customerEmail = "";
        try {
            Stripe.apiKey = api_secret;
            Event event = Webhook.constructEvent(payload, request.getHeader("Stripe-Signature"), webHookSecret);
            try {
                // Handle the event
                switch (event.getType()) {
                    case "checkout.session.completed":
                        // Retrieve the session ID from the event data
                        String json = event.getDataObjectDeserializer().getRawJson();
                        Session preSession = ApiResource.GSON.fromJson(json, Session.class);
                        String sessionId = preSession.getId();

                        // Retrieve the session details including line items
                        SessionRetrieveParams params = SessionRetrieveParams.builder()
                                .addExpand("line_items")
                                .build();
                        Session session = Session.retrieve(sessionId, params, null);

                        String clientReferenceId = session.getClientReferenceId();

                        // Retrieve the customer ID from the session
                        customerId = session.getCustomer();
                        if(customerId != null) {
                            // Retrieve the customer object
                            Customer customer = Customer.retrieve(customerId);
                            customerEmail = customer.getEmail();
                        }
                        else {
                            var csutomrtDetails= session.getCustomerDetails();
                            customerEmail = csutomrtDetails.getEmail();
                        }

                        // Retrieve the price ID from the session's line items
                        String priceId = session.getLineItems().getData().get(0).getPrice().getId();

                        User user = userService.getUserByUUID(clientReferenceId);
                        if(user != null)
                        {
                            String plan = "free";
                            String type = "monthly";
                            if(priceId.equals(priceIdStarter) || priceId.equals(priceIdAutopilot)) {
                                plan = "starter";
                                if(priceId.equals(priceIdAutopilot)){
                                    plan = "autopilot";
                                    type = "lifetime";
                                }
                                if(user.getSubscriptions().stream().count() == 0)
                                {
                                    com.easyprufung.backend.User.Subscription newSubscription = subscriptionService.createSubscription(user.getUuid(), customerId,customerEmail,plan, type,priceId);
                                    user.addSubscription(newSubscription);
                                    userService.updateUser(user);
                                    userService.sendNewSubscriptionEmail(user);
                                }
                                else {
                                    var storedSubscription = user.getSubscriptions().stream().findFirst().get();
                                    subscriptionService.updateSubscription(storedSubscription,customerId,plan, type,priceId);
                                }
                                break;
                            }
                            var storedSubscription = user.getSubscriptions().stream().findFirst().get();
                            var currentIteration = storedSubscription.getIteration();
                            if(priceId.equals(creditIdStarter)){
                                currentIteration += 50;
                            }
                            else if(priceId.equals(creditIdBuilder)){
                                currentIteration += 100;
                            }
                            else if(priceId.equals(creditIdPro)){
                                currentIteration += 200;
                            }
                            storedSubscription.setIteration(currentIteration);
                            subscriptionService.updateSubscription(storedSubscription);
                            break;
                        }
                        break;

                    case "customer.subscription.deleted":
                        String jsonResult = event.getDataObjectDeserializer().getRawJson();
                        Session sessionResult = ApiResource.GSON.fromJson(jsonResult, Session.class);
                        String subscriptionId = sessionResult.getId();
                        Subscription subscription = Subscription.retrieve(
                                subscriptionId
                        );
                        customerId= subscription.getCustomer();
                        com.easyprufung.backend.User.Subscription storedSubscription = subscriptionService.findSubscriptionByCustomerId(customerId);
                        if(storedSubscription != null)
                        {
                            subscriptionService.cancelSubscription(storedSubscription);
                        }
                        break;
                    default:
                }
                return ResponseEntity.ok("Webhook handled: " + event.getType());
            }
            catch (Exception e) {
                logger.error("Event data" + event.getData().getObject().toString() + "Error" +e.getMessage() );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: " + e.getMessage());
        }
    }
}
