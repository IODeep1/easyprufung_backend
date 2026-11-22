package com.smartpruefung.backend.teacher;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Subscription;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.User.Service.SubscriptionService;
import com.smartpruefung.backend.Utils.JwtUtils;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.guest.ExamAccessService;
import com.smartpruefung.backend.exam.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** All teacher data leaves this service through explicit projections. Never serialize User or ExamResult. */
@Service @Transactional
public class TeacherService {
 @PersistenceContext EntityManager em;
 private final Map<String,long[]> inviteWindows=new LinkedHashMap<>();
 private synchronized void inviteRate(String user) {
  long minute=System.currentTimeMillis()/60000;long[] window=inviteWindows.get(user);
  if(window==null || window[0]!=minute){if(inviteWindows.size()>=10000)inviteWindows.entrySet().removeIf(e->e.getValue()[0]!=minute);if(inviteWindows.size()>=10000)throw error(429,"Please try again in a minute.");window=new long[]{minute,0};inviteWindows.put(user,window);}
  if(++window[1]>30)throw error(429,"Too many invitation requests. Try again in a minute.");
 }
 final UsersRepository users;
 final SubscriptionService subscriptions;
 final ExamAccessService access;
 final ExamDefinitionRepository definitions;
 final ExamSessionRepository sessions;
 final ExamResultRepository results;
 final ObjectMapper json;
 final Environment env;
 public TeacherService(UsersRepository users, SubscriptionService subscriptions, ExamAccessService access,
   ExamDefinitionRepository definitions, ExamSessionRepository sessions, ExamResultRepository results, ObjectMapper json, Environment env) {
  this.users=users; this.subscriptions=subscriptions; this.access=access; this.definitions=definitions;
  this.sessions=sessions; this.results=results; this.json=json; this.env=env;
 }
 User actor(String authorization) {
  if(authorization==null || !JwtUtils.validateToken(authorization)) throw error(401,"Sign in to continue.");
  User u=users.findByEmail(JwtUtils.getHttpClientConsumer(authorization).email);
  if(u==null) throw error(401,"Sign in to continue.");
  return u;
 }
 public User teacher(String authorization) {
  User u=actor(authorization);
  if(profile(u)==null || !"ACTIVE".equals(profile(u).status)) throw error(403,"Teacher Mode is not active.");
  return u;
 }
 TeacherProfile profile(User u) { return first(TeacherProfile.class,"userId",u.getUuid()); }
 public Map<String,Object> me(String auth) {
  User u=actor(auth); TeacherProfile p=profile(u);
  return Map.of("status",p==null?"NOT_REGISTERED":p.status,"checkoutEnabled",env.getProperty("smartpruefung.teacher.checkout-enabled",Boolean.class,false));
 }
 public Map<String,Object> apply(String auth) {
  User u=access.lockUser(actor(auth).getUuid());
  if(profile(u)==null) {
   TeacherProfile p=new TeacherProfile(); p.userId=u.getUuid();
   p.status=env.getProperty("smartpruefung.teacher.approval-required",Boolean.class,false)?"PENDING":"ACTIVE";
   em.persist(p); audit(u.getUuid(),"TEACHER_REGISTERED",p.id);
  }
  return me(auth);
 }
 public Map<String,Object> summary(String auth) {
  List<TeacherClass> owned=classes(auth);Set<String> studentIds=new HashSet<>();long assignmentCount=0;
  for(TeacherClass c:owned)if(!c.archived){for(ClassMembership m:em.createQuery("from ClassMembership where classId=:c and active=true",ClassMembership.class).setParameter("c",c.id).getResultList())studentIds.add(m.studentId);assignmentCount+=em.createQuery("select count(a) from ClassAssignment a where a.classId=:c",Long.class).setParameter("c",c.id).getSingleResult();}
  return map("students",studentIds.size(),"assignments",assignmentCount);
 }
 public List<TeacherClass> classes(String auth) {return em.createQuery("from TeacherClass where teacherId=:u order by createdAt desc",TeacherClass.class).setParameter("u",teacher(auth).getUuid()).getResultList();}
 public TeacherClass createClass(String auth,String name) {
  User u=teacher(auth); TeacherClass c=new TeacherClass(); c.teacherId=u.getUuid(); c.name=name(name);em.persist(c);audit(u.getUuid(),"CLASS_CREATED",c.id);return c;
 }
 public TeacherClass owned(String auth,String id,boolean active) {
  TeacherClass c=require(TeacherClass.class,id);
  if(!c.teacherId.equals(teacher(auth).getUuid())) throw error(404,"Class not found.");
  if(active && c.archived) throw error(409,"Class is archived.");return c;
 }
 public TeacherClass updateClass(String auth,String id,String name,boolean archive) {
  TeacherClass c=owned(auth,id,false); em.lock(c,LockModeType.PESSIMISTIC_WRITE);em.refresh(c);
  if(name!=null)c.name=name(name);
  if(archive){c.archived=true;c.inviteHash=null;c.inviteToken=null;c.inviteExpiresAt=null;revokeClassOffers(c.id,null);}
  audit(c.teacherId,archive?"CLASS_ARCHIVED":"CLASS_RENAMED",id);return c;
 }
 public Map<String,Object> currentInvite(String auth,String id) {
  TeacherClass c=owned(auth,id,false);
  boolean active=!c.archived && c.inviteHash!=null && c.inviteExpiresAt!=null && c.inviteExpiresAt.isAfter(Instant.now());
  if(!active)return Map.of("active",false);
  // Older invitations kept only a one-way hash; retrieving them must not silently rotate them.
  return map("active",true,"token",c.inviteToken==null?"":c.inviteToken,"expiresAt",c.inviteExpiresAt);
 }
 public Map<String,Object> invite(String auth,String id,boolean revoke) {
  TeacherClass c=owned(auth,id,true);em.lock(c,LockModeType.PESSIMISTIC_WRITE);em.refresh(c);
  if(revoke){c.inviteHash=null;c.inviteToken=null;c.inviteExpiresAt=null; audit(c.teacherId,"INVITE_REVOKED",id);return Map.of("revoked",true);}
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
  String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  c.inviteToken=token;c.inviteHash=hash(token);c.inviteExpiresAt=Instant.now().plus(30,ChronoUnit.DAYS);
  audit(c.teacherId,"INVITE_ROTATED",id);return Map.of("token",token,"expiresAt",c.inviteExpiresAt);
 }
 private TeacherClass invited(String token) {
  if(token==null || token.length()!=43)throw error(404,"Invitation is invalid or expired.");
  TeacherClass c=first(TeacherClass.class,"inviteHash",hash(token));
  if(c==null || c.archived || c.inviteExpiresAt==null || !c.inviteExpiresAt.isAfter(Instant.now()))throw error(404,"Invitation is invalid or expired.");
  TeacherProfile p=first(TeacherProfile.class,"userId",c.teacherId);
  if(p==null || !"ACTIVE".equals(p.status))throw error(404,"Invitation is unavailable.");return c;
 }
 public Map<String,Object> preview(String auth,String token) {
  inviteRate(actor(auth).getUuid()); TeacherClass c=invited(token);User t=users.findByUUID(c.teacherId);
  return Map.of("id",c.id,"name",c.name,"teacher",t.getFirstname()+" "+t.getLastname(),"consentVersion","class-results-v1",
   "sharing","Your teacher can see completion, scores and section results for exams you start from this class's assignments. Past personal exams, raw answers and writing content are not shared. Leave the class at any time to stop sharing.");
 }
 public Map<String,Object> join(String auth,String token,boolean consent) {
  User u=actor(auth);inviteRate(u.getUuid());TeacherClass c=invited(token);em.lock(c,LockModeType.PESSIMISTIC_WRITE);em.refresh(c);
  if(c.archived || !hash(token).equals(c.inviteHash) || !c.inviteExpiresAt.isAfter(Instant.now()))throw error(404,"Invitation is invalid or expired.");
  access.lockUser(u.getUuid());
  if(!consent)throw error(400,"Explicit sharing consent is required.");
  ClassMembership m=membership(c.id,u.getUuid());
  boolean newMember=m==null;
  if(newMember){m=new ClassMembership();m.classId=c.id;m.studentId=u.getUuid();}
  if(!m.active){m.active=true;m.consentedAt=Instant.now();m.consentVersion="class-results-v1";m.leftAt=null;}
  if(newMember)em.persist(m);
  audit(u.getUuid(),"SHARING_CONSENTED",c.id);return Map.of("joined",true,"classId",c.id);
 }
 ClassMembership membership(String c,String u) {return em.createQuery("from ClassMembership where classId=:c and studentId=:u",ClassMembership.class).setParameter("c",c).setParameter("u",u).getResultStream().findFirst().orElse(null);}
 ClassMembership enrolled(String c,String u) {
  TeacherClass clazz=require(TeacherClass.class,c);ClassMembership m=membership(c,u);
  TeacherProfile p=first(TeacherProfile.class,"userId",clazz.teacherId);
  if(clazz.archived || p==null || !"ACTIVE".equals(p.status) || m==null || !m.active)throw error(403,"Active class membership and sharing consent are required.");return m;
 }
 public void leave(String auth,String classId,String studentId) {
  User u=actor(auth);if(studentId==null)studentId=u.getUuid();else owned(auth,classId,false);
  access.lockUser(studentId);ClassMembership m=membership(classId,studentId);
  if(m!=null){m.active=false;m.leftAt=Instant.now();revokeClassOffers(classId,studentId);audit(u.getUuid(),"SHARING_WITHDRAWN",classId);}
 }
 public List<Map<String,Object>> roster(String auth,String id) {
  owned(auth,id,false);
  return em.createQuery("from ClassMembership where classId=:c and active=true",ClassMembership.class).setParameter("c",id).getResultList().stream().map(m->{
   User u=users.findByUUID(m.studentId);String label=accessLabel(u);
   if(!access.premium(u))for(SeatOffer offer:em.createQuery("from SeatOffer where studentId=:u and state='PENDING'",SeatOffer.class).setParameter("u",m.studentId).getResultList()){expire(offer);if("PENDING".equals(offer.state))label="Teacher seat pending";}
   return map("studentId",m.studentId,"name",u==null?"Former user":u.getFirstname()+" "+u.getLastname(),"access",label,"consentedAt",m.consentedAt);
  }).collect(Collectors.toList());
 }
 String accessLabel(User u) {
  if(u==null || u.getSubscription()==null)return "Free";
  Subscription s=u.getSubscription();
  if(!access.premium(u))return "free".equalsIgnoreCase(s.getPlan())?"Free":"Expired";
  return "teacher_seat".equals(s.getType())?"Teacher seat active":"Paid individually";
 }
 public List<Map<String,Object>> examChoices(String auth) {
  teacher(auth);return definitions.findByActiveTrueOrderByProviderAscLevelAscCodeAsc().stream()
   .filter(d->d.getProvider()==ExamProvider.TELC && d.getLevel()==CefrLevel.B1)
   .map(d->map("code",d.getCode(),"title",d.getTitle())).collect(Collectors.toList());
 }
 public ClassAssignment assign(String auth,String classId,String examCode,Instant dueAt,List<String> studentIds) {
  TeacherClass c=owned(auth,classId,true);em.lock(c,LockModeType.PESSIMISTIC_WRITE);
  if(definitions.findFirstByProviderAndLevelAndCodeAndActiveTrueOrderByDefinitionVersionDesc(ExamProvider.TELC,CefrLevel.B1,examCode).isEmpty())throw error(400,"Select an existing TELC B1 exam.");
  if(dueAt!=null && !dueAt.isAfter(Instant.now()))throw error(400,"Due date must be in the future.");
  List<ClassMembership> members=em.createQuery("from ClassMembership where classId=:c and active=true",ClassMembership.class).setParameter("c",classId).getResultList();
  Set<String> selected=studentIds==null || studentIds.isEmpty()?members.stream().map(m->m.studentId).collect(Collectors.toSet()):new HashSet<>(studentIds);
  if(selected.isEmpty())throw error(400,"Invite students before assigning an exam.");
  for(String id:selected)enrolled(classId,id);
  ClassAssignment a=new ClassAssignment();a.classId=classId;a.examCode=examCode;a.dueAt=dueAt;em.persist(a);
  for(String id:selected){AssignmentStudent s=new AssignmentStudent();s.assignmentId=a.id;s.studentId=id;em.persist(s);}
  audit(c.teacherId,"EXAM_ASSIGNED",a.id);return a;
 }
 public List<ClassAssignment> assignments(String auth,String classId) {
  owned(auth,classId,false);return em.createQuery("from ClassAssignment where classId=:c order by createdAt desc",ClassAssignment.class).setParameter("c",classId).getResultList();
 }
 public List<Map<String,Object>> performance(String auth,String classId) {
  TeacherClass c=owned(auth,classId,false);List<Map<String,Object>> out=new ArrayList<>();if(c.archived)return out;
  for(ClassAssignment a:assignments(auth,classId))for(AssignmentStudent s:em.createQuery("from AssignmentStudent where assignmentId=:a",AssignmentStudent.class).setParameter("a",a.id).getResultList()){
   ClassMembership m=membership(classId,s.studentId);if(m==null || !m.active)continue;
   Map<String,Object> row=map("assignmentId",a.id,"studentId",s.studentId,"examCode",a.examCode,"dueAt",a.dueAt,"status","ASSIGNED");
   if(s.attemptId!=null){
    ExamSession attempt=sessions.findById(UUID.fromString(s.attemptId)).orElse(null);
    // Rejoining never resurrects sharing from an earlier consent period.
    if(attempt!=null && attempt.getCreatedAt().isBefore(m.consentedAt))continue;
    if(attempt!=null){row.put("status",attempt.getStatus().name());
     results.findByExamSessionId(attempt.getId()).ifPresent(r->{
      row.put("score",r.getScore());row.put("maximumScore",r.getMaximumScore());row.put("passed",r.isPassed());
      try{row.put("sections",json.readValue(r.getSectionResultsJson(),SectionResultView[].class));}
      catch(Exception ex){throw new IllegalStateException("Could not read section scores",ex);}
     });
    }
   }
   out.add(row);
  }return out;
 }
 public List<Map<String,Object>> studentClasses(String auth) {
  String u=actor(auth).getUuid();List<Map<String,Object>> out=new ArrayList<>();
  for(ClassMembership m:em.createQuery("from ClassMembership where studentId=:u and active=true",ClassMembership.class).setParameter("u",u).getResultList()){
   TeacherClass c=require(TeacherClass.class,m.classId);out.add(map("id",c.id,"name",c.name,"archived",c.archived,"consentedAt",m.consentedAt));
  }return out;
 }
 public List<Map<String,Object>> studentAssignments(String auth) {
  String u=actor(auth).getUuid();List<Map<String,Object>> out=new ArrayList<>();
  for(AssignmentStudent s:em.createQuery("from AssignmentStudent where studentId=:u order by createdAt desc",AssignmentStudent.class).setParameter("u",u).getResultList()){
   ClassAssignment a=require(ClassAssignment.class,s.assignmentId);ClassMembership m=membership(a.classId,u);TeacherClass c=require(TeacherClass.class,a.classId);
   if(m==null || !m.active || c.archived)continue;
   ExamSession attempt=s.attemptId==null?null:sessions.findById(UUID.fromString(s.attemptId)).orElse(null);
   if(attempt!=null && attempt.getCreatedAt().isBefore(m.consentedAt))continue;
   out.add(map("id",a.id,"className",c.name,"examCode",a.examCode,"dueAt",a.dueAt,"attemptId",s.attemptId,"status",attempt==null?"ASSIGNED":attempt.getStatus().name()));
  }return out;
 }
 /** Uses normal start/quota logic. An unrelated active exam must be finished first, never attached retroactively. */
 public ExamSessionView startAssignment(String auth,String id) {
  String u=actor(auth).getUuid();access.lockUser(u);
  ClassAssignment a=require(ClassAssignment.class,id);ClassMembership m=enrolled(a.classId,u);
  AssignmentStudent s=em.createQuery("from AssignmentStudent where assignmentId=:a and studentId=:u",AssignmentStudent.class).setParameter("a",id).setParameter("u",u).getResultStream().findFirst().orElseThrow(()->error(404,"Assignment not found."));
  if(s.attemptId!=null)throw error(409,"This assignment already has an attempt. Resume it from Exams.");
  if(sessions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(u,SessionStatus.IN_PROGRESS).isPresent())throw error(409,"Finish your current exam before starting this assignment.");
  ExamSessionView view=access.start(new StartExamRequest(u,ExamProvider.TELC,CefrLevel.B1,a.examCode));
  s.attemptId=view.getSessionId().toString();audit(u,"ASSIGNMENT_STARTED",id);return view;
 }
 public Map<String,Object> wallet(String auth) {
  String u=teacher(auth).getUuid();List<TeacherSeat> seats=em.createQuery("from TeacherSeat where teacherId=:u order by createdAt desc",TeacherSeat.class).setParameter("u",u).getResultList();
  List<Map<String,Object>> offers=new ArrayList<>();
  for(TeacherSeat seat:seats)for(SeatOffer o:em.createQuery("from SeatOffer where seatId=:s",SeatOffer.class).setParameter("s",seat.id).getResultList()){
   expire(o);offers.add(map("id",o.id,"studentId",o.studentId,"classId",o.classId,"state",o.state,"expiresAt",o.expiresAt));
  }
  return map("purchased",seats.size(),"available",seats.stream().filter(s->"AVAILABLE".equals(s.state)).count(),"reserved",seats.stream().filter(s->"RESERVED".equals(s.state)).count(),"activated",seats.stream().filter(s->"ACTIVATED".equals(s.state)).count(),"offers",offers);
 }
 public SeatOffer offer(String auth,String classId,String studentId) {
  TeacherClass c=owned(auth,classId,true);access.lockUser(c.teacherId);User student=access.lockUser(studentId);enrolled(classId,studentId);
  if(access.premium(student))throw error(409,"Student already has active paid access.");
  // One pending offer per student across all classes/teachers. Expired offers are released first.
  for(SeatOffer o:em.createQuery("from SeatOffer where studentId=:u and state='PENDING'",SeatOffer.class).setParameter("u",studentId).getResultList()){
   expire(o);if("PENDING".equals(o.state))throw error(409,"Student already has a pending seat offer.");
  }
  TeacherSeat seat=em.createQuery("from TeacherSeat where teacherId=:u and state='AVAILABLE' order by createdAt",TeacherSeat.class).setParameter("u",c.teacherId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultStream().findFirst().orElseThrow(()->error(409,"No available seats. Refresh your wallet or buy seats."));
  seat.state="RESERVED";seat.studentId=studentId;
  SeatOffer o=new SeatOffer();o.seatId=seat.id;o.studentId=studentId;o.classId=classId;o.state="PENDING";o.expiresAt=Instant.now().plus(14,ChronoUnit.DAYS);em.persist(o);audit(c.teacherId,"SEAT_OFFERED",o.id);return o;
 }
 public List<Map<String,Object>> studentOffers(String auth) {
  User u=actor(auth);List<Map<String,Object>> out=new ArrayList<>();
  for(SeatOffer o:em.createQuery("from SeatOffer where studentId=:u order by createdAt desc",SeatOffer.class).setParameter("u",u.getUuid()).getResultList()){
   expire(o);TeacherClass c=require(TeacherClass.class,o.classId);
   TeacherSeat seat=require(TeacherSeat.class,o.seatId);
   String state="ACCEPTED".equals(o.state) && seat.expiresAt!=null && !seat.expiresAt.isAfter(Instant.now())?"EXPIRED":o.state;
   out.add(map("id",o.id,"className",c.name,"state",state,"expiresAt","ACCEPTED".equals(o.state)?seat.expiresAt:o.expiresAt,"canAccept",!access.premium(u)));
  }return out;
 }
 public Map<String,Object> resolveOffer(String auth,String id,String action) {
  User actor=actor(auth);SeatOffer o=require(SeatOffer.class,id);TeacherSeat seat=lockSeat(o.seatId);
  em.refresh(o);if("REVOKED".equals(action)){if(!seat.teacherId.equals(teacher(auth).getUuid()))throw error(404,"Offer not found.");}
  else if(!o.studentId.equals(actor.getUuid()))throw error(404,"Offer not found.");
  User student=access.lockUser(o.studentId);expire(o);
  if(!"PENDING".equals(o.state))return map("state",o.state);
  if("ACCEPTED".equals(action)){
   enrolled(o.classId,o.studentId);
   if(access.premium(student)){release(o,"REVOKED");return map("state",o.state,"message","You already have active paid access. The seat was returned to your teacher.");}
   if(!"RESERVED".equals(seat.state) || !o.studentId.equals(seat.studentId))throw error(409,"Seat is unavailable.");
   Subscription s=student.getSubscription();
   if(s==null){s=subscriptions.createB1PaidSubscription(student.getUuid(),student.getEmail(),"teacher-seat:"+seat.id);student.setSubscription(s);users.save(student);}
   else s=subscriptions.activateB1PaidSubscription(s,student.getUuid(),student.getEmail(),"teacher-seat:"+seat.id);
   Instant now=Instant.now();s.setType("teacher_seat");s.setStartDate(java.sql.Timestamp.from(now));s.setEndDate(java.sql.Timestamp.from(now.plus(60,ChronoUnit.DAYS)));subscriptions.updateSubscription(s);
   seat.state="ACTIVATED";seat.activatedAt=now;seat.expiresAt=s.getEndDate().toInstant();o.state="ACCEPTED";o.resolvedAt=now;
  }else release(o,action);
  audit(actor.getUuid(),"SEAT_"+o.state,id);return map("state",o.state);
 }
 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000,initialDelay=60000)
 public void releaseExpiredOffers() {
  for(SeatOffer o:em.createQuery("from SeatOffer where state='PENDING' and expiresAt<=:now order by id",SeatOffer.class).setParameter("now",Instant.now()).setMaxResults(100).getResultList())expire(o);
 }
 void expire(SeatOffer o) {if("PENDING".equals(o.state) && !o.expiresAt.isAfter(Instant.now())){lockSeat(o.seatId);em.refresh(o);if("PENDING".equals(o.state))release(o,"EXPIRED");}}
 void release(SeatOffer o,String state) {TeacherSeat s=lockSeat(o.seatId);if("RESERVED".equals(s.state)){s.state="AVAILABLE";s.studentId=null;}o.state=state;o.resolvedAt=Instant.now();}
 void revokeClassOffers(String classId,String student) {
  for(SeatOffer o:em.createQuery("from SeatOffer where classId=:c and state='PENDING'",SeatOffer.class).setParameter("c",classId).getResultList())if(student==null || student.equals(o.studentId)){lockSeat(o.seatId);em.refresh(o);if("PENDING".equals(o.state))release(o,"REVOKED");}
 }
 TeacherSeat lockSeat(String id) {TeacherSeat s=em.find(TeacherSeat.class,id,LockModeType.PESSIMISTIC_WRITE);if(s==null)throw error(404,"Seat not found.");em.refresh(s);return s;}
 public Map<String,Object> orderState(String auth,String id) {
  String owner=teacher(auth).getUuid();TeacherSeatOrder order=require(TeacherSeatOrder.class,id);
  if(!owner.equals(order.teacherId))throw error(404,"Order not found.");
  List<TeacherSeat> seats=em.createQuery("from TeacherSeat where orderId=:id",TeacherSeat.class).setParameter("id",id).getResultList();
  long available=seats.stream().filter(s->"AVAILABLE".equals(s.state)).count(),reserved=seats.stream().filter(s->"RESERVED".equals(s.state)).count(),activated=seats.stream().filter(s->"ACTIVATED".equals(s.state)).count();
  return map("order",order,"seats",map("purchased",seats.size(),"available",available,"reserved",reserved,"activated",activated,"assigned",reserved+activated));
 }
 public List<TeacherSeatOrder> orders(String auth) {return em.createQuery("from TeacherSeatOrder where teacherId=:u order by createdAt desc",TeacherSeatOrder.class).setParameter("u",teacher(auth).getUuid()).getResultList();}
 void audit(String actor,String action,String id) {TeacherAuditEvent e=new TeacherAuditEvent();e.actorId=actor;e.action=action;e.resourceId=id;em.persist(e);}
 <T> T require(Class<T> type,String id) {T e=em.find(type,id);if(e==null)throw error(404,"Record not found.");return e;}
 <T> T first(Class<T> type,String field,Object value) {return em.createQuery("from "+type.getSimpleName()+" where "+field+"=:v",type).setParameter("v",value).getResultStream().findFirst().orElse(null);}
 static String name(String name){if(name==null || name.isBlank() || name.trim().length()>120)throw error(400,"Class name must be 1–120 characters.");return name.trim();}
 static String hash(String token){try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
 static ResponseStatusException error(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
 static Map<String,Object> map(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
}
