package com.easyprufung.backend.User.Service;

import com.easyprufung.backend.User.DTO.SubscriptionDTO;
import com.easyprufung.backend.User.Repository.SubscriptionRepository;
import com.easyprufung.backend.User.Repository.UsersRepository;
import com.easyprufung.backend.User.Subscription;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static final String PLAN_FREE = "free";
    public static final String PLAN_B1 = "b1";
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
     * Creates paid TELC B1 access. Normally the user already has a free record,
     * so activateB1PaidSubscription(...) will be used instead.
     */
    public Subscription createB1PaidSubscription(String customerId, String customerEmail, String priceId) {
        Date now = new Date();
        SubscriptionDTO dto = baseSubscription(customerId, customerEmail, PLAN_B1, TYPE_ONE_TIME, priceId, now);
        dto.setQuota(B1_PAID_QUOTA);
        dto.setIteration(0); // legacy field kept for DB compatibility
        dto.setEndDate(addDays(now, B1_ACCESS_DAYS));
        return save(dto);
    }

    /**
     * Converts the user's existing single access record (usually free) into the
     * paid B1 product. A repurchase resets quota to 10 and starts a fresh 60-day
     * access window from the new successful payment.
     */
    public Subscription activateB1PaidSubscription(
            Subscription subscription,
            String customerId,
            String customerEmail,
            String priceId
    ) {
        Date now = new Date();
        subscription.setCustomerId(customerId);
        subscription.setCustomerEmail(customerEmail);
        subscription.setPlan(PLAN_B1);
        subscription.setType(TYPE_ONE_TIME);
        subscription.setPriceId(priceId);
        subscription.setIsActive(true);
        subscription.setStatus(STATUS_ACTIVE);
        subscription.setQuota(B1_PAID_QUOTA);
        subscription.setIteration(0);
        subscription.setStartDate(new Timestamp(now.getTime()));
        subscription.setEndDate(addDays(now, B1_ACCESS_DAYS));
        subscription.setUpdatedDate(new Timestamp(now.getTime()));
        return subscriptionRepository.save(subscription);
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
     * Returns true only when access is active, not expired and at least one quota remains.
     */
    public boolean hasAvailableQuota(Subscription subscription) {
        if (subscription == null || !Boolean.TRUE.equals(subscription.getIsActive())) {
            return false;
        }

        if (isExpired(subscription)) {
            expireSubscription(subscription);
            return false;
        }

        return subscription.getQuota() > 0;
    }

    /**
     * Call this when the user starts/commits a mock exam that should consume one quota.
     * Returns false when the app should block the action and show the payment wall.
     */
    @Transactional
    public boolean consumeQuota(Subscription subscription) {
        if (!hasAvailableQuota(subscription)) {
            return false;
        }

        subscription.setQuota(subscription.getQuota() - 1);
        subscription.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        subscriptionRepository.save(subscription);
        return true;
    }

    /**
     * Kept under the old method name so an existing scheduler does not break.
     * Unlike the old subscription model, quotas are never replenished here.
     * Only time-limited paid/tester access is expired.
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
