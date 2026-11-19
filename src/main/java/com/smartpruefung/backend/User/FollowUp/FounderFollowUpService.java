package com.smartpruefung.backend.User.FollowUp;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Admin.Console.Repository.AdminUserStateRepository;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class FounderFollowUpService {
 private static final Logger log=LoggerFactory.getLogger(FounderFollowUpService.class);
 private final FounderFollowUpRepository jobs;
 private final UsersRepository users;
 private final AdminUserStateRepository states;
 private final FounderFollowUpMail mail;
 private final Clock clock;
 private final int maxAttempts;
 private final long retrySeconds;
 public FounderFollowUpService(FounderFollowUpRepository jobs,UsersRepository users,AdminUserStateRepository states,
  FounderFollowUpMail mail,@Qualifier("founderFollowUpClock") Clock clock,
  @Value("${smartpruefung.email.founder.max-attempts:5}") int maxAttempts,
  @Value("${smartpruefung.email.founder.retry-seconds:3600}") long retrySeconds){
  this.jobs=jobs;this.users=users;this.states=states;this.mail=mail;this.clock=clock;
  this.maxAttempts=Math.max(1,maxAttempts);this.retrySeconds=Math.max(60,retrySeconds);
 }
 @Transactional
 public void schedule(User user){
  // Enrol only newly completed registrations, never scan/backfill all existing accounts.
  if(jobs.existsByUserUuid(user.getUuid())) return;
  Instant created=user.getCreatedDate()==null?clock.instant():user.getCreatedDate().toInstant();
  FounderFollowUp job=new FounderFollowUp();job.setId(UUID.randomUUID().toString());job.setUserUuid(user.getUuid());
  job.setDueAt(created.plus(Duration.ofDays(2)));job.setNextAttemptAt(job.getDueAt());jobs.save(job);
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void deliver(String id){
  // Serializes delivery for this job across application instances. Recheck state after locking.
  FounderFollowUp job=jobs.lock(id).orElse(null);
  Instant now=clock.instant();
  if(job==null || !"PENDING".equals(job.getStatus()) || job.getDueAt().isAfter(now) || job.getNextAttemptAt().isAfter(now)) return;
  User user=users.findByUUID(job.getUserUuid());
  if(user==null || states.findById(user.getId()).map(s -> !s.isEnabled()).orElse(false)){
   job.setStatus("CANCELLED");return;
  }
  job.setAttempts(job.getAttempts()+1);
  try {
   mail.send(user); // current name, email and preferred language; returns only after SMTP accepts
   job.setStatus("SENT");job.setSentAt(clock.instant());job.setLastError(null);
  } catch(Exception failure){
   job.setLastError(failure.getClass().getSimpleName());
   if(job.getAttempts()>=maxAttempts) job.setStatus("FAILED");
   else job.setNextAttemptAt(now.plusSeconds(retrySeconds));
   log.warn("Founder follow-up job {} failed on attempt {} ({})",id,job.getAttempts(),job.getLastError());
  }
 }
}
