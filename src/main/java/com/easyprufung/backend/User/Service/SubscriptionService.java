package com.easyprufung.backend.User.Service;

import com.easyprufung.backend.User.DTO.SubscriptionDTO;
import com.easyprufung.backend.User.Repository.SubscriptionRepository;
import com.easyprufung.backend.User.Repository.UsersRepository;
import com.easyprufung.backend.User.Subscription;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    Environment env;

    public static final String PLAN_FREE = "free";
    /** 10-exam TELC B1 pass. */
    public static final String PLAN_B1 = "b1";
    /** Unlimited TELC B1 pass. */
    public static final String PLAN_B1_UNLIMITED = "b1_unlimited";
    public static final String PLAN_TESTER = "tester";

    public static final String TYPE_FREE = "free";
    public static final String TYPE_ONE_TIME = "one_time";
    public static final String TYPE_TESTER = "tester";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_EXPIRED = "expired";

    public static final int FREE_QUOTA = 1;
    public static final int B1_PAID_QUOTA = 10;
    public static final int B1_ACCESS_DAYS = 60;
    public static final int TESTER_ACCESS_DAYS = 7;

    /**
     * Creates the one and only free access record for a new user.
     * The free quota does not renew. There is intentionally no end date.
     */
    public Subscription createFreeSubscription(String customerEmail) {
        Date now = new Date();
        SubscriptionDTO dto = baseSubscription(customerEmail, customerEmail, PLAN_FREE, TYPE_FREE, PLAN_FREE, now);
        dto.setQuota(FREE_QUOTA);
        dto.setIteration(0); // legacy field kept for DB compatibility
        dto.setEndDate(null);
        return save(dto);
    }

    /**
     * Backwards-compatible helper for the 10-exam paid pass.
     */
    public Subscription createB1PaidSubscription(String customerId, String customerEmail, String priceId) {
        return createPaidB1Subscription(customerId, customerEmail, priceId, false);
    }

    /**
     * Creates the unlimited TELC B1 pass.
     */
    public Subscription createB1UnlimitedSubscription(String customerId, String customerEmail, String priceId) {
        return createPaidB1Subscription(customerId, customerEmail, priceId, true);
    }

    /**
     * Preferred Stripe fulfillment entry point. The Stripe Price ID determines
     * whether the purchase grants 10 exams or unlimited exams.
     *
     * Required properties:
     * easyprufung.stripe.priceidb1=<10-exam price id>
     * easyprufung.stripe.priceidb1unlimited=<unlimited price id>
     */
    public Subscription createB1PaidSubscriptionByPriceId(
            String customerId,
            String customerEmail,
            String priceId
    ) {
        return createPaidB1Subscription(
                customerId,
                customerEmail,
                priceId,
                isUnlimitedPriceId(priceId)
        );
    }

    /**
     * Backwards-compatible activation for the 10-exam paid pass.
     */
    public Subscription activateB1PaidSubscription(
            Subscription subscription,
            String customerId,
            String customerEmail,
            String priceId
    ) {
        return activatePaidB1Subscription(subscription, customerId, customerEmail, priceId, false);
    }

    /**
     * Activates the unlimited TELC B1 pass.
     */
    public Subscription activateB1UnlimitedSubscription(
            Subscription subscription,
            String customerId,
            String customerEmail,
            String priceId
    ) {
        return activatePaidB1Subscription(subscription, customerId, customerEmail, priceId, true);
    }

    /**
     * Preferred Stripe fulfillment entry point for an existing user's single
     * subscription record. The Price ID selects the purchased tier.
     */
    public Subscription activateB1PaidSubscriptionByPriceId(
            Subscription subscription,
            String customerId,
            String customerEmail,
            String priceId
    ) {
        return activatePaidB1Subscription(
                subscription,
                customerId,
                customerEmail,
                priceId,
                isUnlimitedPriceId(priceId)
        );
    }

    /**
     * Tester accounts are intentionally kept. They use the same single-record
     * model but can be created with a custom quota and expire after 7 days.
     */
    public Subscription createTesterSubscription(String userUUID, String customerEmail, int quota) {
        Date now = new Date();
        SubscriptionDTO dto = baseSubscription(customerEmail, customerEmail, PLAN_TESTER, TYPE_TESTER, PLAN_TESTER, now);
        dto.setQuota(quota);
        dto.setIteration(0);
        dto.setEndDate(addDays(now, TESTER_ACCESS_DAYS));
        return save(dto);
    }

    /**
     * Returns true when access is active and not expired. Limited/free/tester
     * plans additionally require quota; b1_unlimited does not.
     */
    public boolean hasAvailableQuota(Subscription subscription) {
        if (subscription == null || !Boolean.TRUE.equals(subscription.getIsActive())) {
            return false;
        }

        if (isExpired(subscription)) {
            expireSubscription(subscription);
            return false;
        }

        if (isUnlimitedAccess(subscription)) {
            return true;
        }

        return subscription.getQuota() > 0;
    }

    /**
     * Consumes one exam for quota-based plans. Unlimited access is validated but
     * never decremented.
     */
    @Transactional
    public boolean consumeQuota(Subscription subscription) {
        if (!hasAvailableQuota(subscription)) {
            return false;
        }

        if (isUnlimitedAccess(subscription)) {
            return true;
        }

        subscription.setQuota(subscription.getQuota() - 1);
        subscription.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        subscriptionRepository.save(subscription);
        return true;
    }

    public boolean isUnlimitedAccess(Subscription subscription) {
        return subscription != null && PLAN_B1_UNLIMITED.equalsIgnoreCase(subscription.getPlan());
    }

    /**
     * Kept under the old method name so an existing scheduler does not break.
     * Quotas are never replenished here; only time-limited access is expired.
     */
    public void resetQuotas() {
        try {
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            for (Subscription subscription : subscriptions) {
                if (isExpired(subscription)) {
                    expireSubscription(subscription);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public Subscription expireSubscription(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        subscription.setIsActive(false);
        subscription.setStatus(STATUS_EXPIRED);
        subscription.setQuota(0);
        subscription.setIteration(0);
        subscription.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        return subscriptionRepository.save(subscription);
    }

    public Subscription updateSubscription(Subscription subscription) {
        subscription.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        return subscriptionRepository.save(subscription);
    }

    public Subscription findSubscriptionByCustomerId(String customerId) {
        return subscriptionRepository.findByCustomerId(customerId);
    }

    public Subscription findSubscriptionByCustomerEmail(String customerEmail) {
        return subscriptionRepository.findByCustomerEmail(customerEmail);
    }

    public Page<Subscription> getSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    private Subscription createPaidB1Subscription(
            String customerId,
            String customerEmail,
            String priceId,
            boolean unlimited
    ) {
        Date now = new Date();
        String plan = unlimited ? PLAN_B1_UNLIMITED : PLAN_B1;
        SubscriptionDTO dto = baseSubscription(customerId, customerEmail, plan, TYPE_ONE_TIME, priceId, now);
        dto.setQuota(unlimited ? 0 : B1_PAID_QUOTA);
        dto.setIteration(0);
        dto.setEndDate(addDays(now, B1_ACCESS_DAYS));
        return save(dto);
    }

    private Subscription activatePaidB1Subscription(
            Subscription subscription,
            String customerId,
            String customerEmail,
            String priceId,
            boolean unlimited
    ) {
        Date now = new Date();
        subscription.setCustomerId(customerId);
        subscription.setCustomerEmail(customerEmail);
        subscription.setPlan(unlimited ? PLAN_B1_UNLIMITED : PLAN_B1);
        subscription.setType(TYPE_ONE_TIME);
        subscription.setPriceId(priceId);
        subscription.setIsActive(true);
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setQuota(unlimited ? 0 : B1_PAID_QUOTA);
        subscription.setIteration(0);
        subscription.setStartDate(new Timestamp(now.getTime()));
        subscription.setEndDate(addDays(now, B1_ACCESS_DAYS));
        subscription.setUpdatedDate(new Timestamp(now.getTime()));
        return subscriptionRepository.save(subscription);
    }

    private boolean isUnlimitedPriceId(String priceId) {
        String limitedPriceId = env.getProperty("easyprufung.stripe.priceidb1");
        String unlimitedPriceId = env.getProperty("easyprufung.stripe.priceidb1unlimited");

        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException("STRIPE_PRICE_ID_MISSING");
        }

        if (unlimitedPriceId != null && !unlimitedPriceId.isBlank() && unlimitedPriceId.equals(priceId)) {
            return true;
        }

        if (limitedPriceId != null && !limitedPriceId.isBlank() && limitedPriceId.equals(priceId)) {
            return false;
        }

        throw new IllegalArgumentException("UNKNOWN_STRIPE_PRICE_ID");
    }

    private boolean isExpired(Subscription subscription) {
        if (subscription.getEndDate() == null) {
            return false;
        }
        return new Timestamp(System.currentTimeMillis()).after(subscription.getEndDate());
    }

    private SubscriptionDTO baseSubscription(
            String customerId,
            String customerEmail,
            String plan,
            String type,
            String priceId,
            Date now
    ) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setUuid(UUID.randomUUID().toString());
        dto.setCustomerId(customerId);
        dto.setCustomerEmail(customerEmail);
        dto.setPlan(plan);
        dto.setType(type);
        dto.setPriceId(priceId);
        dto.setIsActive(true);
        dto.setStatus(STATUS_ACTIVE);
        dto.setStartDate(new Timestamp(now.getTime()));
        dto.setCreatedDate(new Timestamp(now.getTime()));
        dto.setUpdatedDate(new Timestamp(now.getTime()));
        return dto;
    }

    private Timestamp addDays(Date from, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.DATE, days);
        return new Timestamp(calendar.getTimeInMillis());
    }

    private Subscription save(SubscriptionDTO dto) {
        ModelMapper modelMapper = new ModelMapper();
        Subscription subscription = modelMapper.map(dto, Subscription.class);
        return subscriptionRepository.save(subscription);
    }
}
