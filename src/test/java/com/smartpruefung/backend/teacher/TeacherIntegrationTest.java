package com.smartpruefung.backend.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.User.Service.SubscriptionService;
import com.smartpruefung.backend.Utils.JwtUtils;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.repository.*;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@org.springframework.test.context.TestPropertySource(locations="classpath:integration-fixtures.properties")
@SpringBootTest(properties={"smartpruefung.teacher.checkout-enabled=false","smartpruefung.teacher.approval-required=false","smartpruefung.guest.exam-code=GUEST_TEST","smartpruefung.stripe.webhooksecret=whsec_teacher_test","smartpruefung.stripe.apisecret=sk_test_not_real"})
@AutoConfigureMockMvc @ActiveProfiles("admin-test")
public class TeacherIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired TeacherService teachers;
 @Autowired com.smartpruefung.backend.exam.guest.ExamAccessService access;
 @Autowired TeacherPayments payments; @Autowired UsersRepository users; @Autowired SubscriptionService subscriptions;
 @Autowired PlatformTransactionManager manager; @PersistenceContext EntityManager em;
 @Autowired ExamDefinitionRepository definitions; @Autowired ExamSessionRepository sessions; @Autowired ExamResultRepository results;
 @MockBean com.smartpruefung.backend.Shared.Service.EmailSenderService emails;
 @MockBean org.springframework.web.client.RestTemplate remote;
 @MockBean com.smartpruefung.backend.exam.service.ExamGenerationService generation;
 String teacher,other,student,classId,studentId;
 TransactionTemplate tx(){return new TransactionTemplate(manager);}
 String token(User u){return JwtUtils.getJWTToken(u.getEmail(),"ROLE_USER");}
 User user(){User u=new User();u.setUuid(UUID.randomUUID().toString());u.setEmail(u.getUuid()+"@example.test");u.setFirstname("Test");u.setLastname("User");u=users.save(u);u.setSubscription(subscriptions.createFreeSubscription(u.getEmail()));return users.save(u);}
 @BeforeEach void setup() throws Exception {
  User t=user(),o=user(),s=user();teacher=token(t);other=token(o);student=token(s);studentId=s.getUuid();
  call("POST","/api/teacher/apply",null,teacher,200);call("POST","/api/teacher/apply",null,other,200);
  classId=call("POST","/api/teacher/classes",Map.of("name","B1 Evening"),teacher,200).get("id").asText();
 }
 JsonNode call(String method,String path,Object body,String token,int code)throws Exception{
  var req=request(org.springframework.http.HttpMethod.valueOf(method),path).contentType(MediaType.APPLICATION_JSON);
  if(token!=null)req.header("Authorization","Bearer "+token);if(body!=null)req.content(json.writeValueAsBytes(body));
  var response=mvc.perform(req).andExpect(status().is(code)).andReturn().getResponse();
  return response.getContentAsString().isBlank()?json.nullNode():json.readTree(response.getContentAsString());
 }
 void join()throws Exception{String invite=call("POST","/api/teacher/classes/"+classId+"/invites",null,teacher,200).get("token").asText();call("POST","/api/classes/invites/"+invite+"/join",Map.of("consent",true),student,200);}
 TeacherSeatOrder seedOrder(int count){return tx().execute(status->{TeacherSeatOrder o=new TeacherSeatOrder();o.teacherId=teachers.actor(teacher).getUuid();o.quantity=count;o.unitPriceCents=SeatPricing.unit(count);o.totalCents=count*o.unitPriceCents;o.currency="eur";o.status="PENDING";o.stripeSessionId="cs_"+o.id;em.persist(o);return o;});}
 Session paid(TeacherSeatOrder order){String data="{\"id\":\""+order.stripeSessionId+"\",\"mode\":\"payment\",\"payment_status\":\"paid\",\"client_reference_id\":\"teacher-seat:"+order.id+"\",\"metadata\":{\"teacher_seat_order_id\":\""+order.id+"\"},\"currency\":\"eur\",\"amount_total\":"+order.totalCents+",\"line_items\":{\"data\":[{\"quantity\":"+order.quantity+",\"price\":{\"unit_amount\":"+order.unitPriceCents+"}}]}}";return ApiResource.GSON.fromJson(data,Session.class);}
 Event event(){Event e=new Event();e.setId("evt_"+UUID.randomUUID());e.setType("checkout.session.completed");return e;}
 String offer()throws Exception{return call("POST","/api/teacher/seats/offer",Map.of("classId",classId,"studentId",studentId),teacher,200).get("id").asText();}
 @Test void quoteBoundaries(){assertEquals(9995,SeatPricing.quote(5).get("grossTotalCents"));assertEquals(23985,SeatPricing.quote(15).get("grossTotalCents"));assertEquals(34975,SeatPricing.quote(25).get("grossTotalCents"));assertEquals(59950,SeatPricing.quote(50).get("grossTotalCents"));assertEquals(1999,SeatPricing.unit(9));assertEquals(1599,SeatPricing.unit(10));assertEquals(1599,SeatPricing.unit(24));assertEquals(1399,SeatPricing.unit(49));assertThrows(IllegalArgumentException.class,()->SeatPricing.unit(0));assertThrows(IllegalArgumentException.class,()->SeatPricing.unit(1001));}
 @Test void authOwnershipConsentAndRotation()throws Exception{
  call("GET","/api/teacher/classes",null,null,403);
  call("GET","/api/teacher/classes/"+classId,null,other,404);
  call("POST","/api/teacher/classes",Map.of("name"," "),teacher,400);
  String old=call("POST","/api/teacher/classes/"+classId+"/invites",null,teacher,200).get("token").asText();
  call("POST","/api/classes/invites/"+old+"/join",Map.of("consent",false),student,400);
  String fresh=call("POST","/api/teacher/classes/"+classId+"/invites/rotate",null,teacher,200).get("token").asText();
  call("GET","/api/classes/invites/"+old+"/preview",null,student,404);
  for(int i=0;i<2;i++)call("POST","/api/classes/invites/"+fresh+"/join",Map.of("consent",true),student,200);
  assertEquals(1,call("GET","/api/teacher/classes/"+classId+"/students",null,teacher,200).size());
  assertEquals("free",users.findByUUID(studentId).getSubscription().getPlan());
  call("POST","/api/student/classes/"+classId+"/leave",null,student,200);
  assertEquals(0,call("GET","/api/teacher/classes/"+classId+"/students",null,teacher,200).size());
 }
 @Test void invitationRetrievalIsPersistentOwnerOnlyAndRespectsLifecycle()throws Exception{
  String path="/api/teacher/classes/"+classId+"/invites";
  assertFalse(call("GET",path,null,teacher,200).get("active").asBoolean());
  String token=call("POST",path,null,teacher,200).get("token").asText();
  // Each HTTP request uses a fresh persistence context, as after a page reload.
  assertEquals(token,call("GET",path,null,teacher,200).get("token").asText());
  call("GET",path,null,other,404);call("GET",path,null,student,403);call("GET",path,null,null,403);
  assertFalse(call("GET","/api/teacher/classes/"+classId,null,teacher,200).has("inviteToken"));
  assertFalse(call("GET","/api/teacher/classes",null,teacher,200).get(0).has("inviteToken"));
  tx().execute(status->{em.find(TeacherClass.class,classId).inviteToken=null;return null;});
  JsonNode legacy=call("GET",path,null,teacher,200);assertTrue(legacy.get("active").asBoolean());assertEquals("",legacy.get("token").asText());
  call("GET","/api/classes/invites/"+token+"/preview",null,student,200);
  String fresh=call("POST",path+"/rotate",null,teacher,200).get("token").asText();
  assertEquals(fresh,call("GET",path,null,teacher,200).get("token").asText());
  call("GET","/api/classes/invites/"+token+"/preview",null,student,404);
  call("DELETE",path,null,teacher,200);assertFalse(call("GET",path,null,teacher,200).get("active").asBoolean());
  call("GET","/api/classes/invites/"+fresh+"/preview",null,student,404);
  call("POST",path,null,teacher,200);
  tx().execute(status->{em.find(TeacherClass.class,classId).inviteExpiresAt=Instant.now().minusSeconds(1);return null;});
  assertFalse(call("GET",path,null,teacher,200).get("active").asBoolean());
  call("POST",path+"/rotate",null,teacher,200);
  call("POST","/api/teacher/classes/"+classId+"/archive",null,teacher,200);
  assertFalse(call("GET",path,null,teacher,200).get("active").asBoolean());
 }
 @Test void fulfillmentIdempotencyAndAcceptance()throws Exception{
  join();TeacherSeatOrder order=seedOrder(2);Event e=event();Session session=paid(order);
  payments.fulfill(e,session);payments.fulfill(e,session);payments.fulfill(event(),session);
  assertEquals(2L,teachers.wallet(teacher).get("available"));
  String id=offer();assertEquals(1L,teachers.wallet(teacher).get("available"));
  call("POST","/api/student/seat-offers/"+id+"/accept",null,other,404);
  Instant before=Instant.now();call("POST","/api/student/seat-offers/"+id+"/accept",null,student,200);
  var subscription=users.findByUUID(studentId).getSubscription();assertEquals(10,subscription.getQuota());assertEquals("teacher_seat",subscription.getType());assertTrue(subscription.getStartDate().toInstant().compareTo(before)>=0);assertEquals(Duration.ofDays(60),Duration.between(subscription.getStartDate().toInstant(),subscription.getEndDate().toInstant()));
  call("POST","/api/student/seat-offers/"+id+"/accept",null,student,200);assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());
  call("POST","/api/student/classes/"+classId+"/leave",null,student,200);assertTrue(access.premium(users.findByUUID(studentId)));
  payments.reverseEvent(event(),null,"REFUNDED");
 }
 @Test void unpaidAndWrongAmountCreditNothing()throws Exception{
  TeacherSeatOrder o=seedOrder(3);Session s=paid(o);s.setPaymentStatus("unpaid");payments.fulfill(event(),s);assertEquals(0L,teachers.wallet(teacher).get("available"));s.setPaymentStatus("paid");s.setAmountTotal(1L);assertThrows(RuntimeException.class,()->payments.fulfill(event(),s));assertEquals(0L,teachers.wallet(teacher).get("available"));
  call("POST","/api/teacher/seats/checkout",Map.of("quantity",5),teacher,409);
 }
 @Test void pendingOfferReleasesAndPaidAccessIsNeverReplaced()throws Exception{
  join();TeacherSeatOrder o=seedOrder(1);payments.fulfill(event(),paid(o));String id=offer();
  call("POST","/api/student/seat-offers/"+id+"/decline",null,student,200);assertEquals(1L,teachers.wallet(teacher).get("available"));id=offer();
  tx().execute(status->{User u=access.lockUser(studentId);subscriptions.activateB1UnlimitedSubscription(u.getSubscription(),u.getUuid(),u.getEmail(),"test_unlimited");return null;});
  assertEquals("REVOKED",call("POST","/api/student/seat-offers/"+id+"/accept",null,student,200).get("state").asText());assertEquals("b1_unlimited",users.findByUUID(studentId).getSubscription().getPlan());assertEquals(1L,teachers.wallet(teacher).get("available"));
  call("POST","/api/teacher/seats/offer",Map.of("classId",classId,"studentId",studentId),teacher,409);
 }
 @Test void offerExpiryAndRefundPreserveActivatedAccess()throws Exception{
  join();TeacherSeatOrder order=seedOrder(2);Session s=paid(order);s.setPaymentIntent(null);payments.fulfill(event(),s);String id=offer();
  tx().execute(status->{em.find(SeatOffer.class,id).expiresAt=Instant.now().minusSeconds(1);return null;});
  teachers.studentOffers(student);assertEquals(2L,teachers.wallet(teacher).get("available"));
  String active=offer();call("POST","/api/student/seat-offers/"+active+"/accept",null,student,200);
  tx().execute(status->{em.find(TeacherSeatOrder.class,order.id).paymentIntentId="pi_"+order.id;return null;});
  payments.reverseEvent(event(),"pi_"+order.id,"REFUNDED");assertEquals(0L,teachers.wallet(teacher).get("available"));assertTrue(access.premium(users.findByUUID(studentId)));assertTrue(teachers.orders(teacher).get(0).supportReview);
 }
 @Test void concurrentAcceptanceActivatesOnce()throws Exception{
  join();TeacherSeatOrder o=seedOrder(1);payments.fulfill(event(),paid(o));String id=offer();ExecutorService pool=Executors.newFixedThreadPool(2);
  try{Future<?> a=pool.submit(()->teachers.resolveOffer(student,id,"ACCEPTED")),b=pool.submit(()->teachers.resolveOffer(student,id,"ACCEPTED"));a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());assertEquals(1L,teachers.wallet(teacher).get("activated"));
 }
 @Test void assignmentResultsArePrivateAfterWithdrawal()throws Exception{
  join();ExamDefinition d=definitions.findByActiveTrueOrderByProviderAscLevelAscCodeAsc().stream().filter(x->x.getProvider()==ExamProvider.TELC&&x.getLevel()==CefrLevel.B1).findFirst().orElseThrow();
  String assignment=call("POST","/api/teacher/classes/"+classId+"/assignments",Map.of("examCode",d.getCode()),teacher,200).get("id").asText();
  call("POST","/api/student/assignments/"+assignment+"/start",null,other,403);
  tx().execute(status->{ExamSession s=new ExamSession();s.setUserId(studentId);s.setExamDefinition(d);s.setDefinitionVersion(d.getDefinitionVersion());s.setStatus(SessionStatus.EVALUATED);s.setCreatedAt(Instant.now());s.setSubmittedAt(Instant.now());sessions.save(s);AssignmentStudent target=em.createQuery("from AssignmentStudent where assignmentId=:id",AssignmentStudent.class).setParameter("id",assignment).getSingleResult();target.attemptId=s.getId().toString();ExamResult r=new ExamResult();r.setExamSession(s);r.setScore(java.math.BigDecimal.TEN);r.setMaximumScore(java.math.BigDecimal.TEN);r.setPercentage(java.math.BigDecimal.valueOf(100));r.setPassed(true);r.setSectionResultsJson("[{\"sectionKey\":\"READING\",\"title\":\"Reading\",\"score\":10,\"maximumScore\":10}]");r.setQuestionResultsJson("[{\"privateWriting\":\"SECRET ANSWER\"}]");r.setOverallFeedback("PRIVATE FEEDBACK");r.setEvaluatedAt(Instant.now());results.save(r);return null;});
  String progress=call("GET","/api/teacher/classes/"+classId+"/performance",null,teacher,200).toString();assertTrue(progress.contains("Reading"));assertFalse(progress.contains("SECRET"));assertFalse(progress.contains("PRIVATE"));
  call("POST","/api/student/classes/"+classId+"/leave",null,student,200);assertEquals(0,call("GET","/api/teacher/classes/"+classId+"/performance",null,teacher,200).size());assertEquals(1L,sessions.findByUserId(studentId,org.springframework.data.domain.PageRequest.of(0,20)).getTotalElements());
 }
 @Test void personalPurchaseDoesNotConsumeTeacherWallet()throws Exception{
  join();TeacherSeatOrder o=seedOrder(1);payments.fulfill(event(),paid(o));
  Session session=new Session();session.setId("cs_student_"+UUID.randomUUID());session.setClientReferenceId(studentId);
  String price=org.springframework.test.util.ReflectionTestUtils.getField(subscriptions,"env") instanceof org.springframework.core.env.Environment ? ((org.springframework.core.env.Environment)org.springframework.test.util.ReflectionTestUtils.getField(subscriptions,"env")).getProperty("smartpruefung.stripe.priceidb1") : null;
  Event event=event();payments.fulfillStudent(event,session,price);assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());assertEquals(1L,teachers.wallet(teacher).get("available"));
  tx().execute(status->{assertTrue(subscriptions.consumeQuota(users.findByUUID(studentId).getSubscription()));return null;});
  payments.fulfillStudent(event,session,price);payments.fulfillStudent(event(),session,price);assertEquals(9,users.findByUUID(studentId).getSubscription().getQuota());
 }
 @Test void concurrentOffersCannotOverspendWallet()throws Exception{
  join();User second=user();String secondToken=token(second);String invite=call("POST","/api/teacher/classes/"+classId+"/invites",null,teacher,200).get("token").asText();call("POST","/api/classes/invites/"+invite+"/join",Map.of("consent",true),secondToken,200);
  TeacherSeatOrder order=seedOrder(1);payments.fulfill(event(),paid(order));ExecutorService pool=Executors.newFixedThreadPool(2);
  java.util.concurrent.atomic.AtomicInteger success=new java.util.concurrent.atomic.AtomicInteger();
  try{Future<?> a=pool.submit(()->{try{teachers.offer(teacher,classId,studentId);success.incrementAndGet();}catch(org.springframework.web.server.ResponseStatusException expected){assertEquals(409,expected.getRawStatusCode());}}),b=pool.submit(()->{try{teachers.offer(teacher,classId,second.getUuid());success.incrementAndGet();}catch(org.springframework.web.server.ResponseStatusException expected){assertEquals(409,expected.getRawStatusCode());}});a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  assertEquals(1,success.get());assertEquals(0L,teachers.wallet(teacher).get("available"));assertEquals(1L,teachers.wallet(teacher).get("reserved"));
 }
 @Test void signedWebhookIsRequiredAndRetriesCreditOnce()throws Exception{
  TeacherSeatOrder order=seedOrder(2);Session session=paid(order);
  var getterMethod=ApiResource.class.getDeclaredMethod("getGlobalResponseGetter");getterMethod.setAccessible(true);
  var original=(com.stripe.net.StripeResponseGetter)getterMethod.invoke(null);
  var getter=org.mockito.Mockito.mock(com.stripe.net.StripeResponseGetter.class, invocation->invocation.getMethod().getName().equals("request")?session:null);
  ApiResource.setStripeResponseGetter(getter);
  try{
   String payload="{\"id\":\"evt_signed_"+order.id+"\",\"object\":\"event\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\""+order.stripeSessionId+"\",\"object\":\"checkout.session\"}}}";
   mvc.perform(post("/public/stripe/webhook").contentType(MediaType.APPLICATION_JSON).header("Stripe-Signature","invalid").content(payload)).andExpect(status().isBadRequest());assertEquals(0L,teachers.wallet(teacher).get("available"));
   long now=Instant.now().getEpochSecond();
   com.fasterxml.jackson.databind.node.ObjectNode envelope=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(payload);
   envelope.put("created",now);envelope.put("livemode",false);payload=json.writeValueAsString(envelope);
   javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("whsec_teacher_test".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));StringBuilder signature=new StringBuilder();for(byte b:mac.doFinal((now+"."+payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)))signature.append(String.format("%02x",b));
   for(int i=0;i<2;i++)mvc.perform(post("/public/stripe/webhook").contentType(MediaType.APPLICATION_JSON).header("Stripe-Signature","t="+now+",v1="+signature).content(payload)).andExpect(status().isOk());
   assertEquals(2L,teachers.wallet(teacher).get("available"));
  }finally{ApiResource.setStripeResponseGetter(original);}
 }
 @Test void assignedStartResumeAndSubmitUseExistingQuotaExactlyOnce()throws Exception{
  join();TeacherSeatOrder order=seedOrder(1);payments.fulfill(event(),paid(order));String offered=offer();call("POST","/api/student/seat-offers/"+offered+"/accept",null,student,200);
  ExamDefinition d=definitions.findByActiveTrueOrderByProviderAscLevelAscCodeAsc().stream().filter(x->x.getProvider()==ExamProvider.TELC&&x.getLevel()==CefrLevel.B1).findFirst().orElseThrow();
  org.mockito.Mockito.when(generation.start(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation->{
   var input=(com.smartpruefung.backend.exam.dto.StartExamRequest)invocation.getArgument(0);ExamSession attempt=new ExamSession();attempt.setUserId(input.getUserId());attempt.setExamDefinition(d);attempt.setDefinitionVersion(d.getDefinitionVersion());attempt.setStatus(SessionStatus.IN_PROGRESS);attempt.setCreatedAt(Instant.now());attempt.setStartedAt(Instant.now());attempt.setExpiresAt(Instant.now().plusSeconds(3600));sessions.save(attempt);
   return new com.smartpruefung.backend.exam.dto.ExamSessionView(attempt.getId(),d.getCode(),d.getTitle(),d.getProvider(),d.getLevel(),d.getDefinitionVersion(),SessionStatus.IN_PROGRESS,attempt.getStartedAt(),attempt.getExpiresAt(),List.of());
  });
  String assignment=call("POST","/api/teacher/classes/"+classId+"/assignments",Map.of("examCode",d.getCode()),teacher,200).get("id").asText();
  String attempt=call("POST","/api/student/assignments/"+assignment+"/start",null,student,200).get("sessionId").asText();assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());
  assertEquals(attempt,call("GET","/api/exams/sessions/active",null,student,200).get("sessionId").asText());
  call("POST","/api/student/assignments/"+assignment+"/start",null,student,409);
  call("POST","/api/exams/sessions/"+attempt+"/submit",Map.of("answers",List.of()),student,200);assertEquals(9,users.findByUUID(studentId).getSubscription().getQuota());
  call("POST","/api/exams/sessions/"+attempt+"/submit",Map.of("answers",List.of()),student,200);assertEquals(9,users.findByUUID(studentId).getSubscription().getQuota());
  call("GET","/api/exams/sessions/"+attempt+"/result",null,student,200);assertEquals(1,call("GET","/api/exams/sessions?userId="+studentId,null,student,200).get("content").size());assertEquals("EVALUATED",call("GET","/api/teacher/classes/"+classId+"/performance",null,teacher,200).get(0).get("status").asText());
 }
 /** Offline runner for environments with cached JUnit engines but no Gradle daemon access. */
 public static void main(String[] args)throws Exception{
  var context=new org.springframework.test.context.TestContextManager(TeacherIntegrationTest.class);int count=0;
  try{for(var method:TeacherIntegrationTest.class.getDeclaredMethods())if(method.isAnnotationPresent(Test.class)){
   TeacherIntegrationTest test=new TeacherIntegrationTest();context.prepareTestInstance(test);test.setup();try{method.invoke(test);System.out.println("PASS "+method.getName());count++;}catch(java.lang.reflect.InvocationTargetException ex){throw new AssertionError(method.getName(),ex.getCause());}
  }System.out.println("Teacher integration tests passed: "+count);}finally{context.getTestContext().markApplicationContextDirty(org.springframework.test.annotation.DirtiesContext.HierarchyMode.EXHAUSTIVE);}
 }
}
