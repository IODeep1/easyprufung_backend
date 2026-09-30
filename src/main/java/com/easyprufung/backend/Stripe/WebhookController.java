package com.easyprufung.backend.Stripe;

import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.LineItem;
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

    private static final Logger logger =
            LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    UserService userService;

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    private Environment env;

    @PostMapping("/public/stripe/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request
    ) {
        String apiSecret =
                env.getProperty("easyprufung.stripe.apisecret");
        String webHookSecret =
                env.getProperty("easyprufung.stripe.webhooksecret");

        String priceIdB1 =
                env.getProperty("easyprufung.stripe.priceidb1");

        String priceIdB1Unlimited =
                env.getProperty(
                        "easyprufung.stripe.priceidb1unlimited"
                );

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
                    processSuccessfulCheckout(
                            event,
                            priceIdB1,
                            priceIdB1Unlimited
                    );
                    break;

                default:
                    // One-time payment model:
                    // no recurring subscription events are required.
                    break;
            }

            return ResponseEntity.ok(
                    "Webhook handled: " + event.getType()
            );

        } catch (Exception e) {
            logger.error("Stripe webhook error", e);

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Webhook error: " + e.getMessage());
        }
    }

    private void processSuccessfulCheckout(
            Event event,
            String priceIdB1,
            String priceIdB1Unlimited
    ) throws Exception {

        String json =
                event.getDataObjectDeserializer().getRawJson();

        Session eventSession =
                ApiResource.GSON.fromJson(json, Session.class);

        SessionRetrieveParams params =
                SessionRetrieveParams.builder()
                        .addExpand("line_items")
                        .build();

        Session session =
                Session.retrieve(
                        eventSession.getId(),
                        params,
                        null
                );

        /*
         * EasyPrufung now uses Stripe one-time payments only.
         */
        if (session.getMode() != null
                && !"payment".equalsIgnoreCase(session.getMode())) {

            logger.info(
                    "Ignoring Checkout Session {} with mode {}",
                    session.getId(),
                    session.getMode()
            );

            return;
        }

        /*
         * checkout.session.completed can happen before delayed
         * payment methods have actually settled.
         *
         * Access is granted only when Stripe reports "paid".
         */
        if (!"paid".equalsIgnoreCase(
                session.getPaymentStatus()
        )) {
            logger.info(
                    "Ignoring unpaid Checkout Session {}",
                    session.getId()
            );

            return;
        }

        if (session.getLineItems() == null
                || session.getLineItems()
                        .getData()
                        .isEmpty()) {

            throw new IllegalStateException(
                    "Stripe Checkout Session contains no line items"
            );
        }

        /*
         * Payment Links currently contain one EasyPrufung product.
         * Find the supported price rather than blindly trusting
         * the first line item.
         */
        String paidPriceId = findSupportedPriceId(
                session,
                priceIdB1,
                priceIdB1Unlimited
        );

        if (paidPriceId == null) {
            logger.info(
                    "Ignoring Checkout Session {} because it contains no supported EasyPrufung price",
                    session.getId()
            );

            return;
        }

        String purchasedPlan =
                priceIdB1Unlimited != null
                        && priceIdB1Unlimited.equals(paidPriceId)
                        ? SubscriptionService.PLAN_B1_UNLIMITED
                        : SubscriptionService.PLAN_B1;

        logger.info(
                "Processing Stripe Checkout Session {} for plan {} and price {}",
                session.getId(),
                purchasedPlan,
                paidPriceId
        );

        /*
         * client_reference_id is set by the frontend to the
         * EasyPrufung user's UUID.
         */
        String clientReferenceId =
                session.getClientReferenceId();

        if (clientReferenceId == null
                || clientReferenceId.isBlank()) {

            throw new IllegalStateException(
                    "Stripe client_reference_id is missing"
            );
        }

        User user =
                userService.getUserByUUID(
                        clientReferenceId
                );

        if (user == null) {
            throw new IllegalStateException(
                    "User not found for client_reference_id "
                            + clientReferenceId
            );
        }

        String customerId = session.getCustomer();
        String customerEmail = user.getEmail();

        /*
         * Payment Links do not always create a Stripe Customer.
         * Prefer the Stripe Customer email when available and
         * otherwise fall back to Checkout customer_details.
         */
        if (customerId != null
                && !customerId.isBlank()) {

            Customer customer =
                    Customer.retrieve(customerId);

            if (customer.getEmail() != null
                    && !customer.getEmail().isBlank()) {

                customerEmail =
                        customer.getEmail();
            }

        } else if (
                session.getCustomerDetails() != null
                        && session
                                .getCustomerDetails()
                                .getEmail() != null
                        && !session
                                .getCustomerDetails()
                                .getEmail()
                                .isBlank()
        ) {
            customerEmail =
                    session
                            .getCustomerDetails()
                            .getEmail();
        }

        Subscription subscription =
                user.getSubscription();

        /*
         * The Price ID is the source of truth:
         *
         * easyprufung.stripe.priceidb1
         *   -> b1
         *   -> 10 exams
         *
         * easyprufung.stripe.priceidb1unlimited
         *   -> b1_unlimited
         *   -> unlimited exams
         *
         * SubscriptionService also validates the Price ID,
         * so an unknown Stripe product cannot accidentally
         * grant EasyPrufung access.
         */
        if (subscription == null) {

            subscription =
                    subscriptionService
                            .createB1PaidSubscriptionByPriceId(
                                    customerId,
                                    customerEmail,
                                    paidPriceId
                            );

            user.setSubscription(subscription);
            userService.updateUser(user);

        } else {

            subscriptionService
                    .activateB1PaidSubscriptionByPriceId(
                            subscription,
                            customerId,
                            customerEmail,
                            paidPriceId
                    );
        }

        logger.info(
                "Stripe purchase fulfilled for user {} with plan {}",
                clientReferenceId,
                purchasedPlan
        );

        userService.sendNewSubscriptionEmail(user);
    }

    private String findSupportedPriceId(
            Session session,
            String priceIdB1,
            String priceIdB1Unlimited
    ) {

        for (LineItem lineItem :
                session.getLineItems().getData()) {

            if (lineItem.getPrice() == null
                    || lineItem.getPrice().getId() == null) {
                continue;
            }

            String priceId =
                    lineItem.getPrice().getId();

            if (priceIdB1 != null
                    && !priceIdB1.isBlank()
                    && priceIdB1.equals(priceId)) {

                return priceId;
            }

            if (priceIdB1Unlimited != null
                    && !priceIdB1Unlimited.isBlank()
                    && priceIdB1Unlimited.equals(priceId)) {

                return priceId;
            }
        }

        return null;
    }
}
