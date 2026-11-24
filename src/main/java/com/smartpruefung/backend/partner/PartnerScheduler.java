package com.smartpruefung.backend.partner;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="smartpruefung.partner.scheduling-enabled",havingValue="true",matchIfMissing=true)
@Component @EnableScheduling
public class PartnerScheduler {
 private final PartnerStripeService stripe;
 public PartnerScheduler(PartnerStripeService stripe){this.stripe=stripe;}
 @Scheduled(fixedDelayString="${smartpruefung.partner.retry-ms:60000}") public void retry(){stripe.drain();}
 @Scheduled(fixedDelayString="${smartpruefung.partner.code-check-ms:3600000}") public void codes(){stripe.reconcileCodes();}
}
