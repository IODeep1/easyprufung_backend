package com.smartpruefung.backend.Admin.Console;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.smartpruefung.backend.Admin.*;
import com.smartpruefung.backend.Admin.Repository.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.AdminPasswords;
import com.smartpruefung.backend.Security.AESEncryption;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import com.smartpruefung.backend.teacher.*;
import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.Utils.JwtUtils;
import javax.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties={"smartpruefung.guest.exam-code=GUEST_TEST"})
@AutoConfigureMockMvc @ActiveProfiles("admin-test")
public class AdminTeacherIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 @Autowired AdminsRepository admins; @Autowired RolesRepository roles;
 @Autowired AdminCredentialRepository credentials; @Autowired UsersRepository users;
 @Autowired com.smartpruefung.backend.User.Service.SubscriptionService customerAccess;
 @MockBean org.springframework.web.client.RestTemplate google;
 @MockBean com.smartpruefung.backend.Shared.Service.EmailSenderService emailSender;
 String root;
 @BeforeEach void login()throws Exception{root=staff("SuperAdmin");}

  String staff(String role) throws Exception {
    String email = UUID.randomUUID() + "@staff.example.com";
    Admin a = new Admin();
    a.setEmail(email);
    a.setFirstname("Test");
    a.setLastname(role);
    a.setPassword(AESEncryption.encrypt("Legacy-Account-493!"));
    a.addRole(roles.findByName(role));
    a = admins.save(a);
    AdminCredential c = new AdminCredential();
    c.setId(a.getId());
    c.setPasswordHash(AdminPasswords.encode("Dashboard-Password-493!"));
    credentials.save(c);
    return call(
            "POST",
            "/public/admin/session",
            Map.of("email", email, "password", "Dashboard-Password-493!"),
            null,
            200)
        .get("token")
        .asText();
  }

  JsonNode body(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  JsonNode call(String method, String path, Object value, String token, int expected)
      throws Exception {
    var req =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), path)
            .contentType(MediaType.APPLICATION_JSON);
    if (token != null) req.header("Authorization", "Bearer " + token);
    if (value != null) req.content(json.writeValueAsBytes(value));
    MvcResult r = mvc.perform(req).andExpect(status().is(expected)).andReturn();
    return r.getResponse().getContentAsString().isBlank() ? NullNode.instance : body(r);
  }

  ObjectNode newUser() {
    ObjectNode u = json.createObjectNode();
    u.put("email", UUID.randomUUID() + "@example.com");
    u.put("firstname", "Learner");
    u.put("lastname", "OriginalStack");
    u.put("password", "Learner-Password-493!");
    u.put("preferredLanguage", "de");
    u.put("enabled", true);
    u.put("reason", "Admin integration test");
    return u;
  }

  JsonNode createUser() throws Exception {
    return call("POST", "/api/admin/v1/users", newUser(), root, 201);
  }


 @PersistenceContext EntityManager em;
 @Autowired PlatformTransactionManager manager;
 String profileId,classId,orderId,teacherId,studentId,email,customerToken;
 final String base="/api/admin/v1/teacher-mode";
 TransactionTemplate tx(){return new TransactionTemplate(manager);}
 @BeforeEach void seed()throws Exception{
  JsonNode t=createUser(),s=createUser();teacherId=t.get("uuid").asText();studentId=s.get("uuid").asText();email=t.get("email").asText();customerToken=JwtUtils.getJWTToken(email,"ROLE_USER");
  tx().execute(status->{
   TeacherProfile p=new TeacherProfile();p.userId=teacherId;p.status="PENDING";em.persist(p);profileId=p.id;
   TeacherClass c=new TeacherClass();c.teacherId=teacherId;c.name="Admin Test "+UUID.randomUUID();c.inviteHash="private-hash-"+UUID.randomUUID();c.inviteToken="x".repeat(43);c.inviteExpiresAt=Instant.now().plusSeconds(86400);em.persist(c);classId=c.id;
   ClassMembership m=new ClassMembership();m.classId=c.id;m.studentId=studentId;m.active=true;m.consentVersion="class-results-v1";m.consentedAt=Instant.now();em.persist(m);
   ClassAssignment a=new ClassAssignment();a.classId=c.id;a.examCode="TELC_B1_ADMIN_TEST";em.persist(a);
   TeacherSeatOrder o=new TeacherSeatOrder();o.teacherId=teacherId;o.quantity=3;o.unitPriceCents=1999;o.totalCents=5997;o.currency="eur";o.status="PAID";o.supportReview=true;o.stripeSessionId="cs_"+UUID.randomUUID();em.persist(o);orderId=o.id;
   for(int i=1;i<=3;i++){TeacherSeat seat=new TeacherSeat();seat.orderId=o.id;seat.teacherId=teacherId;seat.seatNumber=i;seat.state=i==1?"AVAILABLE":i==2?"RESERVED":"ACTIVATED";if(i>1)seat.studentId=studentId;if(i==3){seat.activatedAt=Instant.now();seat.expiresAt=Instant.now().plusSeconds(86400);}em.persist(seat);if(i==2){SeatOffer offer=new SeatOffer();offer.seatId=seat.id;offer.classId=c.id;offer.studentId=studentId;offer.state="PENDING";offer.expiresAt=Instant.now().plusSeconds(86400);em.persist(offer);}}
   TeacherAuditEvent e=new TeacherAuditEvent();e.actorId=teacherId;e.action="TEACHER_REGISTERED";e.resourceId=p.id;em.persist(e);
   User learner=users.findByUUID(studentId);customerAccess.activateB1PaidSubscription(learner.getSubscription(),studentId,learner.getEmail(),"teacher-seat:test");learner.getSubscription().setType("teacher_seat");return null;
  });
 }
 JsonNode change(String method,String path,Object input,String token,Long version,int expected)throws Exception{
  var request=request(org.springframework.http.HttpMethod.valueOf(method),path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(input));if(token!=null)request.header("Authorization","Bearer "+token);if(version!=null)request.header("If-Match",version);
  var response=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();return response.getContentAsString().isBlank()?json.nullNode():json.readTree(response.getContentAsString());
 }
 @Test void staffReadRolesAndCustomerIsolation()throws Exception{
  String viewer=staff("Viewer");
  for(String path:List.of("/summary","/teachers","/teachers/"+profileId,"/classes","/classes/"+classId+"/members","/classes/"+classId+"/assignments","/orders","/orders/"+orderId+"/seats","/orders/"+orderId+"/offers","/activity")){
   call("GET",base+path,null,viewer,200);call("GET",base+path,null,null,401);call("GET",base+path,null,customerToken,401);
  }
  change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","ACTIVE","reason","Permission test"),viewer,0L,403);
  change("POST",base+"/classes/"+classId+"/archive",Map.of("reason","Permission test"),viewer,0L,403);
  change("PUT",base+"/orders/"+orderId+"/review",Map.of("supportReview",false,"reason","Permission test"),viewer,0L,403);
 }
 @Test void pagedFiltersCountsAndSafeDetails()throws Exception{
  JsonNode teachers=call("GET",base+"/teachers?q="+email+"&status=PENDING",null,root,200);assertEquals(1,teachers.get("totalElements").asInt());JsonNode p=teachers.get("content").get(0);assertEquals(1,p.get("classes").asInt());assertEquals(1,p.get("learners").asInt());assertEquals(1,p.get("available").asInt());assertEquals(1,p.get("reserved").asInt());assertEquals(1,p.get("activated").asInt());
  assertEquals(0,call("GET",base+"/teachers?q="+email+"&status=ACTIVE",null,root,200).get("totalElements").asInt());
  JsonNode classes=call("GET",base+"/classes?teacherId="+teacherId,null,root,200);assertEquals(1,classes.get("totalElements").asInt());JsonNode c=classes.get("content").get(0);assertTrue(c.get("invitationActive").asBoolean());assertEquals(1,c.get("members").asInt());assertEquals(1,c.get("assignments").asInt());assertFalse(classes.toString().contains("inviteHash"));assertFalse(classes.toString().contains("inviteToken"));assertFalse(classes.toString().contains("x".repeat(43)));
  assertEquals(1,call("GET",base+"/classes/"+classId+"/members",null,root,200).get("content").size());assertEquals("TELC_B1_ADMIN_TEST",call("GET",base+"/classes/"+classId+"/assignments",null,root,200).get("content").get(0).get("examCode").asText());
  JsonNode orders=call("GET",base+"/orders?teacherId="+teacherId+"&review=true&status=PAID",null,root,200);assertEquals(1,orders.get("totalElements").asInt());assertEquals(5997,orders.get("content").get(0).get("totalCents").asInt());
  JsonNode seats=call("GET",base+"/orders/"+orderId+"/seats?size=2",null,root,200);assertEquals(3,seats.get("totalElements").asInt());assertEquals(2,seats.get("content").size());assertEquals(1,call("GET",base+"/orders/"+orderId+"/seats?size=2&page=1",null,root,200).get("content").size());assertEquals(1,call("GET",base+"/orders/"+orderId+"/offers",null,root,200).get("content").size());
  assertEquals(1,call("GET",base+"/activity?q="+profileId,null,root,200).get("content").size());JsonNode summary=call("GET",base+"/summary",null,root,200);assertTrue(summary.get("teachers").asInt()>0);assertTrue(summary.get("paidOrderTotalCents").asInt()>=5997);
  for(String path:List.of("/teachers?status=WRONG","/classes?archived=WRONG","/orders?review=WRONG","/orders?status=WRONG","/teachers?size=101","/orders?page=-1"))call("GET",base+path,null,root,400);
  call("GET",base+"/teachers/missing",null,root,404);call("GET",base+"/classes/missing/members",null,root,404);
 }
 @Test void statusRequiresReasonVersionAndPreservesLearnerAccess()throws Exception{
  change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","ACTIVE","reason",""),root,0L,400);
  change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","OTHER","reason","Test"),root,0L,400);
  change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","ACTIVE","reason","Test"),root,null,400);
  JsonNode active=change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","ACTIVE","reason","Approved teacher"),root,0L,200);assertEquals("ACTIVE",active.get("status").asText());assertTrue(active.get("version").asLong()>0);
  change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","SUSPENDED","reason","Stale request"),root,0L,409);
  String normal=staff("Admin");change("PUT",base+"/teachers/"+profileId+"/status",Map.of("status","SUSPENDED","reason","Support suspension"),normal,active.get("version").asLong(),200);
  assertEquals("SUSPENDED",call("GET","/api/teacher/me",null,customerToken,200).get("status").asText());assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());assertEquals("teacher_seat",users.findByUUID(studentId).getSubscription().getType());
  tx().execute(status->{assertEquals(2L,em.createQuery("select count(a) from AdminAudit a where a.entityType='TeacherProfile' and a.entityId=:id",Long.class).setParameter("id",profileId).getSingleResult());return null;});
 }
 @Test void archiveInvalidatesInvitesAndReleasesOnlyUnactivatedOffers()throws Exception{
  change("POST",base+"/classes/"+classId+"/archive",Map.of("reason","Support archive"),root,0L,200);
  tx().execute(status->{TeacherClass c=em.find(TeacherClass.class,classId);assertTrue(c.archived);assertNull(c.inviteHash);assertNull(c.inviteToken);assertEquals(2L,em.createQuery("select count(s) from TeacherSeat s where s.orderId=:id and s.state='AVAILABLE'",Long.class).setParameter("id",orderId).getSingleResult());assertEquals(1L,em.createQuery("select count(s) from TeacherSeat s where s.orderId=:id and s.state='ACTIVATED'",Long.class).setParameter("id",orderId).getSingleResult());return null;});
  assertEquals("REVOKED",call("GET",base+"/orders/"+orderId+"/offers",null,root,200).get("content").get(0).get("state").asText());assertEquals(10,users.findByUUID(studentId).getSubscription().getQuota());
  change("POST",base+"/classes/"+classId+"/archive",Map.of("reason","Stale archive"),root,0L,409);
 }
 @Test void reviewIsAuditedAndNeverChangesPaymentsOrSeatCredits()throws Exception{
  change("PUT",base+"/orders/"+orderId+"/review",Map.of("reason","Missing review choice"),root,0L,400);
  JsonNode order=change("PUT",base+"/orders/"+orderId+"/review",Map.of("supportReview",false,"reason","Investigated with payment provider"),root,0L,200);assertFalse(order.get("supportReview").asBoolean());assertEquals("PAID",order.get("status").asText());assertEquals(5997,order.get("totalCents").asInt());assertEquals(3,call("GET",base+"/orders/"+orderId+"/seats",null,root,200).get("totalElements").asInt());
  change("PUT",base+"/orders/"+orderId+"/review",Map.of("supportReview",true,"reason","Stale review"),root,0L,409);
  tx().execute(status->{assertEquals(1L,em.createQuery("select count(a) from AdminAudit a where a.entityId=:id and a.action='TEACHER_ORDER_REVIEW_CHANGED'",Long.class).setParameter("id",orderId).getSingleResult());return null;});
 }
 public static void main(String[] args)throws Exception{
  var context=new org.springframework.test.context.TestContextManager(AdminTeacherIntegrationTest.class);int count=0;
  try{for(var method:AdminTeacherIntegrationTest.class.getDeclaredMethods())if(method.isAnnotationPresent(Test.class)){
   AdminTeacherIntegrationTest test=new AdminTeacherIntegrationTest();context.prepareTestInstance(test);test.login();test.seed();try{method.invoke(test);System.out.println("PASS "+method.getName());count++;}catch(java.lang.reflect.InvocationTargetException ex){throw new AssertionError(method.getName(),ex.getCause());}
  }System.out.println("Admin teacher tests passed: "+count);}finally{context.getTestContext().markApplicationContextDirty(org.springframework.test.annotation.DirtiesContext.HierarchyMode.EXHAUSTIVE);}
 }
}
