package com.easyprufung.backend.Scheduling;

import com.easyprufung.backend.User.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    SubscriptionService subscriptionService;

    // This will run at midnight every day
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetMonthlyQuotas() {
        subscriptionService.resetQuotas();
    }
}
