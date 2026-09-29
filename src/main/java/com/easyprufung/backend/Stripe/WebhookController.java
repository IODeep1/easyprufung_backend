package com.easyprufung.backend.Stripe;

import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Event;
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
        String apiSecret = env.getProperty("easyprufung.stripe.apisecret");
        String webHookSecret = env.getProperty("easyprufung.stripe.webhooksecret");
        String priceIdB1 = env.getProperty("easyprufung.stripe.priceidb1");

        try {
            Stripe.apiKey = apiSecret;
            Event event = Webhook.constructEvent(
                    payload,
                    request.getHeader("Stripe-Signature"),
                    webHookSecret
            );

            switch (event.getType()) {
                case "checkout.session.completed":
                case "checkout.session.async_payment_succeeded":
                    processSuccessfulCheckout(event, priceIdB1);
                    break;
                default:
                    // No recurring subscription events are needed anymore.
                    break;
            }

            return ResponseEntity.ok("Webhook handled: " + event.getType());
        } catch (Exception e) {
            logger.error("Stripe webhook error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Webhook error: " + e.getMessage());
        }
    }

    private void processSuccessfulCheckout(Event event, String priceIdB1) throws Exception {
        String json = event.getDataObjectDeserializer().getRawJson();
        Session eventSession = ApiResource.GSON.fromJson(json, Session.class);

        SessionRetrieveParams params = SessionRetrieveParams.builder()
                .addExpand("line_items")
                .build();
        Session session = Session.retrieve(eventSession.getId(), params, null);

        // checkout.session.completed can be emitted before delayed payment methods settle.
        // Only grant access once Stripe reports the payment as paid.
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            logger.info("Ignoring unpaid Checkout Session {}", session.getId());
            return;
        }

        if (session.getLineItems() == null || session.getLineItems().getData().isEmpty()) {
            throw new IllegalStateException("Stripe Checkout Session contains no line items");
        }

        String paidPriceId = session.getLineItems().getData().get(0).getPrice().getId();
        if (priceIdB1 == null || !priceIdB1.equals(paidPriceId)) {
            logger.info("Ignoring Checkout Session {} for unrelated price {}", session.getId(), paidPriceId);
            return;
        }

        String clientReferenceId = session.getClientReferenceId();
        if (clientReferenceId == null || clientReferenceId.isEmpty()) {
            throw new IllegalStateException("Stripe client_reference_id is missing");
        }

        User user = userService.getUserByUUID(clientReferenceId);
        if (user == null) {
            throw new IllegalStateException("User not found for client_reference_id " + clientReferenceId);
        }

        String customerId = session.getCustomer();
        String customerEmail = user.getEmail();

        if (customerId != null) {
            Customer customer = Customer.retrieve(customerId);
            if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                customerEmail = customer.getEmail();
            }
        } else if (session.getCustomerDetails() != null
                && session.getCustomerDetails().getEmail() != null) {
            customerEmail = session.getCustomerDetails().getEmail();
        }

        Subscription subscription = user.getSubscription();
        if (subscription == null) {
            subscription = subscriptionService.createB1PaidSubscription(
                    customerId,
                    customerEmail,
                    paidPriceId
            );
            user.setSubscription(subscription);
            userService.updateUser(user);
        } else {
            subscriptionService.activateB1PaidSubscription(
                    subscription,
                    customerId,
                    customerEmail,
                    paidPriceId
            );
        }

        userService.sendNewSubscriptionEmail(user);
    }
}
