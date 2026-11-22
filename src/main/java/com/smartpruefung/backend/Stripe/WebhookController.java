package com.smartpruefung.backend.Stripe;

import com.smartpruefung.backend.User.Service.SubscriptionService;
import com.smartpruefung.backend.User.Service.UserService;
import com.smartpruefung.backend.User.Subscription;
import com.smartpruefung.backend.User.User;
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
    private com.smartpruefung.backend.teacher.TeacherPayments teacherPayments;

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
                env.getProperty("smartpruefung.stripe.apisecret");
        String webHookSecret =
                env.getProperty("smartpruefung.stripe.webhooksecret");

        String priceIdB1 =
                env.getProperty("smartpruefung.stripe.priceidb1");

        String priceIdB1Unlimited =
                env.getProperty(
                        "smartpruefung.stripe.priceidb1unlimited"
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

                case "checkout.session.expired":
                case "checkout.session.async_payment_failed": {
                    Session failed = ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(), Session.class);
                    teacherPayments.failedCheckout(event, failed, "checkout.session.expired".equals(event.getType()) ? "EXPIRED" : "FAILED");
                    break;
                }
                case "charge.refunded": {
                    com.stripe.model.Charge charge = ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(), com.stripe.model.Charge.class);
                    teacherPayments.reverseEvent(event, charge.getPaymentIntent(), "REFUNDED");
                    break;
                }
                case "charge.dispute.created": {
                    com.stripe.model.Dispute dispute = ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(), com.stripe.model.Dispute.class);
                    teacherPayments.reverseEvent(event, dispute.getPaymentIntent(), "DISPUTED");
                    break;
                }
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

        if (teacherPayments.isTeacher(session)) {
            teacherPayments.fulfill(event, session);
            return;
        }

        /*
         * SmartPrüfung now uses Stripe one-time payments only.
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
         * Payment Links currently contain one SmartPrüfung product.
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
                    "Ignoring Checkout Session {} because it contains no supported SmartPrüfung price",
                    session.getId()
            );

            return;
        }

        teacherPayments.fulfillStudent(event, session, paidPriceId);
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
