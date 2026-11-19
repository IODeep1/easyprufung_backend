package com.smartpruefung.backend.User.FollowUp;

import java.time.Clock;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.*;

@Component
@ConditionalOnProperty(name="smartpruefung.email.founder.enabled",havingValue="true",matchIfMissing=true)
public class FounderFollowUpScheduler {
 private static final Logger log=LoggerFactory.getLogger(FounderFollowUpScheduler.class);
 private final FounderFollowUpRepository jobs;
 private final FounderFollowUpService service;
 private final Clock clock;
 public FounderFollowUpScheduler(FounderFollowUpRepository jobs,FounderFollowUpService service,
  @Qualifier("founderFollowUpClock") Clock clock){this.jobs=jobs;this.service=service;this.clock=clock;}
 @Scheduled(fixedDelayString="${smartpruefung.email.founder.poll-ms:60000}",initialDelayString="${smartpruefung.email.founder.initial-delay-ms:60000}")
 public void run(){
  for(String id:jobs.due(clock.instant(),PageRequest.of(0,25))) {
   try {service.deliver(id);}
   catch(Exception failure){log.warn("Founder follow-up processing failed for job {} ({})",id,failure.getClass().getSimpleName());}
  }
 }
}
