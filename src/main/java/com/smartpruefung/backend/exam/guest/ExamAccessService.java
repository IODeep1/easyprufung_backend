package com.smartpruefung.backend.exam.guest;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Service.*;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.repository.*;
import com.smartpruefung.backend.exam.mapper.ExamViewMapper;
import com.smartpruefung.backend.exam.service.ExamGenerationService;
import javax.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamAccessService {
 @PersistenceContext private EntityManager em;
 private final UserService users;
 private final SubscriptionService subscriptions;
 private final ExamSessionRepository sessions;
 private final ExamGenerationService generation;
 private final ExamViewMapper mapper;
 public ExamAccessService(UserService users,SubscriptionService subscriptions,ExamSessionRepository sessions,ExamGenerationService generation,ExamViewMapper mapper){
  this.users=users;this.subscriptions=subscriptions;this.sessions=sessions;this.generation=generation;this.mapper=mapper;
 }
 public User lockUser(String uuid){
  User user=users.getUserByUUID(uuid);if(user==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  em.lock(user,LockModeType.PESSIMISTIC_WRITE);em.refresh(user);return user;
 }
 public boolean premium(User user){
  if(user==null) return false;
  var s=user.getSubscription();if(s==null || !Boolean.TRUE.equals(s.getIsActive()) || "expired".equalsIgnoreCase(s.getStatus())) return false;
  String plan=s.getPlan()==null?"":s.getPlan().toLowerCase(Locale.ROOT);
  return (List.of("b1","b1_unlimited","tester").contains(plan))
   && ("tester".equals(plan) || s.getEndDate()==null || s.getEndDate().toInstant().isAfter(Instant.now()));
 }
 public void requireQuota(User user){if(!subscriptions.hasAvailableQuota(user.getSubscription())) throw payment();}
 public void requireClaimAccess(User user){
  requireQuota(user);
  if(!premium(user) && sessions.existsByUserId(user.getUuid())) throw payment();
 }
 @Transactional
 public ExamSessionView start(StartExamRequest input){
  User user=lockUser(input.getUserId());
  var active=sessions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getUuid(),SessionStatus.IN_PROGRESS);
  if(active.isPresent() && (input.getExamCode()==null || input.getExamCode().equals(active.get().getExamDefinition().getCode()))) return mapper.toView(active.get());
  requireClaimAccess(user);
  return generation.start(input);
 }
 public ExamResultView result(String owner,ExamResultView result){
  if(!premium(users.getUserByUUID(owner))){result.setQuestions(List.of());result.setSections(List.of());result.setOverallFeedback(null);}
  return result;
 }
 public void requirePremium(String owner){if(!premium(users.getUserByUUID(owner))) throw payment();}
 public static ResponseStatusException payment(){return new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"Purchase required to unlock detailed results or start another exam.");}
}
