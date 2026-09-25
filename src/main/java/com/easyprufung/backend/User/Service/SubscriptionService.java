package com.easyprufung.backend.User.Service;


import com.easyprufung.backend.User.DTO.SubscriptionDTO;
import com.easyprufung.backend.User.Repository.SubscriptionRepository;
import com.easyprufung.backend.User.Repository.UsersRepository;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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


    final String free = "free";
    final String lifetime = "lifetime";
    final String monthly = "monthly";
    final String tester = "tester";
    final String starter = "starter";
    final String autopilot = "autopilot";
    final int freeIteration = 4;
    final int starterIteration = 50;
    final int autopilotIteration = -1;
    final int freeQuota = 1;
    final int starterQuota = 3;
    final int autopilotQuota = -1;
    final String activeStatus = "active";
    final String cancelledStatus = "cancelled";
    final String cancellationRequestedStatus = "cancellation_requested";

    //Methods
    public Subscription createSubscription(String userUUID, String customerId, String customerEmail, String plan, String type, String priceId) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setUuid(UUID.randomUUID().toString());
        subscriptionDTO.setCustomerId(customerId);
        subscriptionDTO.setCustomerEmail(customerEmail);
        subscriptionDTO.setPlan(plan);
        subscriptionDTO.setType(type);
        subscriptionDTO.setPriceId(priceId);
        subscriptionDTO.setIsActive(true);
        subscriptionDTO.setStatus(activeStatus);
        if(plan.equals(starter)){
            subscriptionDTO.setIteration(starterIteration);
            subscriptionDTO.setQuota(starterQuota);
        }
        else if (plan.equals(autopilot)){
            subscriptionDTO.setIteration(autopilotIteration);
            subscriptionDTO.setQuota(autopilotQuota);
        }
        else {
            subscriptionDTO.setIteration(0);
            subscriptionDTO.setQuota(0);
        }
        subscriptionDTO.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(type.equals(monthly)){
            calendar.add(Calendar.MONTH, 1);
        }
        else if(type.equals(lifetime)){
            calendar.add(Calendar.YEAR, 99);
        }
        Date endDate = calendar.getTime();
        subscriptionDTO.setEndDate(new Timestamp(endDate.getTime()));
        subscriptionDTO.setCreatedDate(new Timestamp(date.getTime()));
        subscriptionDTO.setUpdatedDate(new Timestamp(date.getTime()));
        Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
        return subscriptionRepository.save(subscription);
    }

    public Subscription createAppSumoSubscription(String userUUID, String customerId, String customerEmail) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setUuid(UUID.randomUUID().toString());
        subscriptionDTO.setCustomerId(customerId);
        subscriptionDTO.setCustomerEmail(customerEmail);
        subscriptionDTO.setPlan(autopilot);
        subscriptionDTO.setType(lifetime);
        subscriptionDTO.setPriceId("promo_code");
        subscriptionDTO.setIsActive(true);
        subscriptionDTO.setStatus(activeStatus);
        subscriptionDTO.setQuota(autopilotQuota);
        subscriptionDTO.setIteration(autopilotIteration);
        subscriptionDTO.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, 99);
        Date endDate = calendar.getTime();
        subscriptionDTO.setEndDate(new Timestamp(endDate.getTime()));
        subscriptionDTO.setCreatedDate(new Timestamp(date.getTime()));
        subscriptionDTO.setUpdatedDate(new Timestamp(date.getTime()));
        Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
        return subscriptionRepository.save(subscription);
    }

    public Subscription createTesterSubscription(String userUUID, String customerEmail, int quota) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setUuid(UUID.randomUUID().toString());
        subscriptionDTO.setCustomerId(customerEmail);
        subscriptionDTO.setCustomerEmail(customerEmail);
        subscriptionDTO.setPlan(tester);
        subscriptionDTO.setType("monthly");
        subscriptionDTO.setPriceId(tester);
        subscriptionDTO.setIsActive(true);
        subscriptionDTO.setStatus(activeStatus);
        subscriptionDTO.setQuota(quota);
        subscriptionDTO.setIteration(quota*20);
        subscriptionDTO.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 7);
        Date endDate = calendar.getTime();
        subscriptionDTO.setEndDate(new Timestamp(endDate.getTime()));
        subscriptionDTO.setCreatedDate(new Timestamp(date.getTime()));
        subscriptionDTO.setUpdatedDate(new Timestamp(date.getTime()));
        Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
        return subscriptionRepository.save(subscription);
    }

    public Subscription createFreeSubscription(String customerEmail) {
        ModelMapper modelMapper = new ModelMapper();
        Date date = new Date();
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setUuid(UUID.randomUUID().toString());
        subscriptionDTO.setCustomerId(customerEmail);
        subscriptionDTO.setCustomerEmail(customerEmail);
        subscriptionDTO.setPlan(free);
        subscriptionDTO.setType(monthly);
        subscriptionDTO.setPriceId(free);
        subscriptionDTO.setIsActive(true);
        subscriptionDTO.setStatus(activeStatus);
        subscriptionDTO.setQuota(freeQuota);
        subscriptionDTO.setIteration(freeIteration);
        subscriptionDTO.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        Date endDate = calendar.getTime();
        subscriptionDTO.setEndDate(new Timestamp(endDate.getTime()));
        subscriptionDTO.setCreatedDate(new Timestamp(date.getTime()));
        subscriptionDTO.setUpdatedDate(new Timestamp(date.getTime()));
        Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
        return subscriptionRepository.save(subscription);
    }


    public Subscription updateSubscription(Subscription subscription, String customerId, String plan, String type, String priceId) {
        Date date = new Date();
        subscription.setCustomerId(customerId);
        subscription.setPlan(plan);
        subscription.setType(type);
        subscription.setPriceId(priceId);
        subscription.setIsActive(true);
        subscription.setStatus(activeStatus);
        if(plan.equals(starter)){
            subscription.setQuota(starterQuota);
            subscription.setIteration(starterIteration);
        }
        else if(plan.equals(autopilot)){
            subscription.setQuota(autopilotQuota);
            subscription.setIteration(autopilotIteration);
        }
        else {
            subscription.setIteration(0);
            subscription.setQuota(0);
        }

        subscription.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(type.equals(monthly)){
            calendar.add(Calendar.MONTH, 1);
        }
        else if(type.equals(lifetime)){
            calendar.add(Calendar.YEAR, 99);
        }
        Date endDate = calendar.getTime();
        subscription.setEndDate(new Timestamp(endDate.getTime()));
        subscription.setUpdatedDate(new Timestamp(date.getTime()));
        return subscriptionRepository.save(subscription);
    }

    public void resetQuotas(){
        try {
            Date date = new Date();
            Timestamp currentDateTime = new Timestamp(date.getTime());
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            for (Subscription subscription : subscriptions) {
                if(currentDateTime.after(subscription.getEndDate())){
                    if(subscription.getStatus().equals(activeStatus)) {
                        resetSubscription(subscription);
                    }
                    else  if(subscription.getStatus().equals(cancellationRequestedStatus)){
                        cancelSubscription(subscription);
                    }
                }
            }
        }
        catch (Exception ex){
            logger.error(ex.getMessage());
        }
    }

    private Subscription resetSubscription(Subscription subscription) {
        Date date = new Date();
        if(subscription.getPlan().equals(starter)){
            subscription.setIteration(starterIteration);
            subscription.setQuota(starterQuota);
        }
        else if (subscription.getPlan().equals(autopilot)){
           return subscription;
        }
        else {
            subscription.setIteration(0);
            subscription.setQuota(0);
        }
        subscription.setStartDate(new Timestamp(date.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String type = subscription.getType();
        if(type.equals(monthly)){
            calendar.add(Calendar.MONTH, 1);
        }
        Date endDate = calendar.getTime();
        subscription.setEndDate(new Timestamp(endDate.getTime()));
        subscription.setUpdatedDate(new Timestamp(date.getTime()));
        return subscriptionRepository.save(subscription);
    }

    public Subscription requestCancellationSubscription(Subscription subscription) {
        Date date = new Date();
        subscription.setStatus(cancellationRequestedStatus);
        subscription.setUpdatedDate(new Timestamp(date.getTime()));
        return subscriptionRepository.save(subscription);
    }

    public Subscription cancelSubscription(Subscription subscription) {
        Date date = new Date();
        subscription.setIsActive(false);
        subscription.setStatus(cancelledStatus);
        subscription.setUpdatedDate(new Timestamp(date.getTime()));
        User user = usersRepository.findByEmail(subscription.getCustomerEmail());
        return subscriptionRepository.save(subscription);
    }

    public Subscription updateSubscription(Subscription subscription) {
        Date date = new Date();
        subscription.setUpdatedDate(new Timestamp(date.getTime()));
        return subscriptionRepository.save(subscription);
    }

    public Subscription findSubscriptionByCustomerId(String customerId) {;
        return subscriptionRepository.findByCustomerId(customerId);
    }

    public Subscription findSubscriptionByCustomerEmail(String customerEmail) {;
        return subscriptionRepository.findByCustomerEmail(customerEmail);
    }

    public Page<Subscription> getSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }
}
