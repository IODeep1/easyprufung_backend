package com.smartpruefung.backend.Admin.Console;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.smartpruefung.backend.Admin.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.AdminPasswords;
import com.smartpruefung.backend.Admin.Repository.*;
import com.smartpruefung.backend.Security.AESEncryption;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Utils.JwtUtils;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest(properties="smartpruefung.guest.exam-code=GUEST_TEST")
@AutoConfigureMockMvc
@ActiveProfiles("admin-test")
class AdminIntegrationTest {
  @Autowired MockMvc mvc;
  @org.springframework.boot.test.mock.mockito.MockBean org.springframework.web.client.RestTemplate google;
  @Autowired com.smartpruefung.backend.exam.guest.GuestExamRepository guestRecords;
  @Autowired PlanSettingsRepository planSettings;
  @Autowired com.smartpruefung.backend.User.Service.SubscriptionService customerAccess;

  @org.springframework.boot.test.mock.mockito.MockBean
  com.smartpruefung.backend.Shared.Service.EmailSenderService emailSender;

  @Autowired ObjectMapper json;
  @Autowired AdminsRepository admins;
  @Autowired RolesRepository roles;
  @Autowired AdminCredentialRepository credentials;
  @Autowired UsersRepository users;
  String root;

  @BeforeEach
  void login() throws Exception {
    root = staff("SuperAdmin");
  }

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

  ObjectNode update(JsonNode u) {
    ObjectNode value = (ObjectNode) u.deepCopy();
    value.put("reason", "Support request");
    value.remove("password");
    return value;
  }

  ObjectNode exam(String source) {
    ObjectNode e = json.createObjectNode();
    e.put("code", "TEST_" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
    e.put("title", "Original stack exam");
    e.put("provider", "CUSTOM");
    e.put("level", "B1");
    e.put("definitionVersion", 1);
    e.put("maximumScore", 1);
    e.put("passPercentage", 60);
    e.put("reason", "Admin integration test");
    ObjectNode s = e.putArray("sections").addObject();
    s.put("sectionKey", "reading");
    s.put("title", "Reading");
    s.put("durationSeconds", 1800);
    ObjectNode p = s.putArray("parts").addObject();
    p.put("partKey", "reading_1");
    p.put("title", "Reading one");
    p.put("contentSource", source);
    p.put("evaluationMode", "OBJECTIVE");
    p.put("questionType", "SINGLE_CHOICE");
    p.put("firstQuestionNumber", 1);
    p.put("questionCount", 1);
    p.put("pointsPerQuestion", 1);
    p.put("maximumScore", 1);
    p.put("generationInstructions", "Create an original B1 question with one correct answer.");
    return e;
  }

  @Autowired com.smartpruefung.backend.activity.ActivityService activity;
  @Autowired org.springframework.jdbc.core.JdbcTemplate activityDb;
  @Autowired org.springframework.transaction.support.TransactionTemplate activityTx;

  JsonNode guestCall(String method,String suffix,Object value,String token,int expected) throws Exception {
    var req=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
      org.springframework.http.HttpMethod.valueOf(method),"/public/exams/guest"+suffix).contentType(MediaType.APPLICATION_JSON);
    if(token!=null) req.header("X-Guest-Token",token);
    if(value!=null) req.content(json.writeValueAsBytes(value));
    var result=mvc.perform(req).andExpect(status().is(expected)).andReturn();
    return result.getResponse().getContentAsString().isBlank()?NullNode.instance:body(result);
  }
  JsonNode claimGuest(String guest,String jwt,int expected) throws Exception {
    return body(mvc.perform(post("/api/exams/sessions/guest/claim").header("Authorization","Bearer "+jwt)
      .header("X-Guest-Token",guest)).andExpect(status().is(expected)).andReturn());
  }
  @Test
  void guestCapabilitiesExpireAndNewAllocationsAreRateLimited() throws Exception {
    String guest=guestCall("POST","",null,null,200).get("token").asText();
    var stored=guestRecords.findAll().stream().filter(g->g.getTokenHash().equals(com.smartpruefung.backend.exam.guest.GuestExamService.hash(guest))).findFirst().orElseThrow();
    assertNotEquals(guest,stored.getTokenHash());assertEquals(64,stored.getTokenHash().length());
    stored.setExpiresAt(java.time.Instant.now().minusSeconds(1));guestRecords.saveAndFlush(stored);
    guestCall("GET","",null,guest,404);
    for(int i=0;i<20;i++) mvc.perform(post("/public/exams/guest").with(req->{req.setRemoteAddr("198.51.100.44");return req;})).andExpect(status().isOk());
    mvc.perform(post("/public/exams/guest").with(req->{req.setRemoteAddr("198.51.100.44");return req;})).andExpect(status().isTooManyRequests());
  }

  @Test
  void guestExamSurvivesRegistrationAndPurchaseGatesDetailsAndFurtherExams() throws Exception {
    ObjectNode input=exam("PREDEFINED");input.put("code","GUEST_TEST");input.put("provider","TELC");
    JsonNode exam=call("POST","/api/admin/v1/exams",input,root,201);
    String part=exam.get("sections").get(0).get("parts").get(0).get("id").asText();
    Object template=Map.of("templateKey","guest_test","instructions","Choose an answer.","questions",List.of(
      Map.of("number","1","prompt","Choose a","type","SINGLE_CHOICE","maximumScore",1,
      "options",List.of(Map.of("key","a","text","Answer a"),Map.of("key","b","text","Answer b")),
      "correctAnswer",Map.of("acceptedOptionKeys",List.of("a")))));
    mvc.perform(post("/api/admin/v1/parts/"+part+"/templates").header("Authorization","Bearer "+root).header("X-Reason","Guest test")
      .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(template))).andExpect(status().isCreated());
    mvc.perform(put("/api/admin/v1/exams/"+exam.get("id").asText()+"/publication?active=true").header("Authorization","Bearer "+root)
      .header("If-Match",exam.get("entityVersion").asLong()).header("X-Reason","Guest test")).andExpect(status().isOk());
    String guest=guestCall("POST","",null,null,200).get("token").asText();
    JsonNode started=guestCall("POST","/start",null,guest,200);
    String id=started.get("sessionId").asText();
    assertEquals(id,guestCall("POST","/start",null,guest,200).get("sessionId").asText());
    assertFalse(started.toString().contains("correctAnswer"));
    guestCall("GET","",null,"x".repeat(43),404);
    call("POST","/api/exams/sessions/"+id+"/submit",Map.of("compactAnswers","1a"),null,403);
    JsonNode other=createUser();String otherJwt=JwtUtils.getJWTToken(other.get("email").asText(),"ROLE_USER");
    call("GET","/api/exams/sessions/"+id,null,otherJwt,404);
    claimGuest(guest,otherJwt,409); // cannot claim before finishing
    Object progress=Map.of("revision",0,"index",0,"answers",Map.of("1",Map.of("selectedOptionKeys",List.of("a"),"text","")));
    assertEquals(1,guestCall("PUT","/progress",progress,guest,200).get("revision").asInt());
    guestCall("PUT","/progress",progress,guest,409);
    assertEquals("a",guestCall("GET","",null,guest,200).get("progress").get("answers").get("1").get("selectedOptionKeys").get(0).asText());
    guestCall("POST","/finish",null,guest,204);
    guestCall("PUT","/progress",progress,guest,409);
    var registration=newUser();
    JsonNode registered=call("POST","/public/user/create",registration,null,200);
    String jwt=registered.get("token").asText();String uuid=registered.get("uuid").asText();
    assertEquals(id,claimGuest(guest,jwt,200).get("sessionId").asText());
    assertEquals(id,claimGuest(guest,jwt,200).get("sessionId").asText());
    claimGuest(guest,otherJwt,404);guestCall("GET","",null,guest,404);
    JsonNode restored=call("GET","/api/exams/sessions/"+id+"/progress",null,jwt,200);
    assertEquals("a",restored.get("answers").get("1").get("selectedOptionKeys").get(0).asText());
    var answers=Map.of("compactAnswers","1a");
    JsonNode result=call("POST","/api/exams/sessions/"+id+"/submit",answers,jwt,200);
    assertTrue(result.get("passed").asBoolean());assertEquals(0,result.get("questions").size());assertEquals(0,result.get("sections").size());
    assertEquals(0,call("GET","/api/exams/sessions/"+id+"/result",null,jwt,200).get("questions").size());
    call("POST","/api/exams/sessions/"+id+"/submit",answers,jwt,200);
    assertEquals(0,users.findByEmail(registered.get("email").asText()).getSubscription().getQuota());
    Object start=Map.of("userId",uuid,"provider","TELC","level","B1","examCode","GUEST_TEST");
    call("POST","/api/exams/sessions",start,jwt,402);
    call("POST","/api/exams/sessions/"+id+"/certificate",null,jwt,402);
    String second=guestCall("POST","",null,null,200).get("token").asText();
    guestCall("POST","/start",null,second,200);guestCall("POST","/finish",null,second,204);claimGuest(second,jwt,402);
    JsonNode adminUser=call("GET","/api/admin/v1/users/"+registered.get("id").asLong(),null,root,200);
    Object grant=Map.of("plan","b1","quota",10,"status","active","version",adminUser.get("subscription").get("version").asLong(),"reason","Paid access test");
    call("PUT","/api/admin/v1/users/"+registered.get("id").asLong()+"/subscription",grant,root,200);
    JsonNode unlocked=call("GET","/api/exams/sessions/"+id+"/result",null,jwt,200);
    assertEquals(1,unlocked.get("questions").size());assertEquals(1,unlocked.get("sections").size());
    call("POST","/api/exams/sessions",start,jwt,201);
    assertEquals(1,call("GET","/api/admin/v1/activity?user="+uuid+"&type=EXAM_SUBMITTED",null,root,200).get("totalElements").asInt());
    // A first Google signup can claim the same kind of saved guest work as a password signup.
    String googleGuest=guestCall("POST","",null,null,200).get("token").asText();
    String googleSession=guestCall("POST","/start",null,googleGuest,200).get("sessionId").asText();
    guestCall("PUT","/progress",progress,googleGuest,200);guestCall("POST","/finish",null,googleGuest,204);
    String googleEmail=UUID.randomUUID()+"@example.com";
    org.mockito.Mockito.when(google.getForObject(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq(String.class)))
      .thenReturn(json.writeValueAsString(Map.of("email",googleEmail,"given_name","Guest","family_name","Google")));
    JsonNode googleAccount=call("POST","/public/user/google/login",Map.of("access_token","test-token","preferredLanguage","fr"),null,200);
    claimGuest(googleGuest,googleAccount.get("token").asText(),200);
    assertEquals("a",call("GET","/api/exams/sessions/"+googleSession+"/progress",null,googleAccount.get("token").asText(),200)
      .get("answers").get("1").get("selectedOptionKeys").get(0).asText());
  }

  @Test
  void directWebsiteEndpointAllowsBothProductionOriginsAndDerivesCountryFromHeader() throws Exception {
    for(String origin:List.of("https://smartpruefung.com","https://www.smartpruefung.com")) {
      mvc.perform(options("/api/website-activity").header("Origin",origin)
        .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type"))
        .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin));
    }
    mvc.perform(options("/api/website-activity").header("Origin","https://unrelated.example")
      .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    String visitor=UUID.randomUUID().toString();
    var event=Map.of("eventId",UUID.randomUUID().toString(),"visitorId",visitor,"type","WEBSITE_CLICKED",
      "path","/en","target","Open Pricing","language","en","country","US");
    mvc.perform(post("/api/website-activity").header("Origin","https://smartpruefung.com")
      .header("CF-IPCountry","DE").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(event)))
      .andExpect(status().isNoContent()).andExpect(header().string("Access-Control-Allow-Origin","https://smartpruefung.com"));
    JsonNode list=call("GET","/api/admin/v1/activity?user=visitor:"+visitor,null,root,200);
    assertEquals(1,list.get("totalElements").asInt());
    assertEquals("DE",list.get("content").get(0).get("country").asText());
  }

  @Test
  void anonymousWebsiteActivityValidationDedupeAndAdminCounts() throws Exception {
    String visitor=UUID.randomUUID().toString();
    var event=new HashMap<String,Object>();
    event.put("eventId",UUID.randomUUID().toString());event.put("visitorId",visitor);
    event.put("type","WEBSITE_CLICKED");event.put("path","/en/pricing");event.put("target","Get 10 exams (pricing)");event.put("country","DE");
    event.put("destination","https://app.smartpruefung.com/register");event.put("language","en");
    call("POST","/public/activity/website",event,null,204);
    call("POST","/public/activity/website",event,null,204);
    String path="/api/admin/v1/activity?user=visitor:"+visitor;
    JsonNode list=call("GET",path,null,root,200);
    assertEquals(1,list.get("totalElements").asInt());
    assertEquals("/en/pricing",list.get("content").get(0).get("pagePath").asText());
    assertEquals("Get 10 exams (pricing)",list.get("content").get(0).get("target").asText());
    JsonNode summary=call("GET","/api/admin/v1/activity/summary?user=visitor:"+visitor,null,root,200);
    assertEquals(1,summary.get("targets").get(0).get("total").asInt());
    assertEquals(1,summary.get("users").asInt());
    assertEquals("DE",list.get("content").get(0).get("country").asText());
    assertEquals("DE",summary.get("countries").get(0).get("country").asText());
    call("GET",path,null,null,401);
    event.put("eventId",UUID.randomUUID().toString());event.put("type","EXAM_SUBMITTED");
    call("POST","/public/activity/website",event,null,400);
    event.put("type","WEBSITE_PAGE_VIEWED");event.put("path","/en?email=private");
    call("POST","/public/activity/website",event,null,400);
    event.put("path","/en");event.put("destination","/register?email=private");
    call("POST","/public/activity/website",event,null,400);
    event.put("destination","/register");event.put("visitorId","not-an-id");
    call("POST","/public/activity/website",event,null,400);
    event.put("visitorId",visitor);call("POST","/public/activity/website",event,null,204);
    assertEquals(2,call("GET",path,null,root,200).get("totalElements").asInt());
  }

  @Test
  void activityCollectionIdentityDedupeFilteringAndPermissions() throws Exception {
    JsonNode u=createUser(), other=createUser();
    String uuid=users.findByEmail(u.get("email").asText()).getUuid();
    String otherUuid=users.findByEmail(other.get("email").asText()).getUuid();
    String jwt=JwtUtils.getJWTToken(u.get("email").asText(), "ROLE_USER");
    String viewer=staff("Viewer");
    String id=UUID.randomUUID().toString();
    var event=new HashMap<String,Object>();
    event.put("eventId",id); event.put("type","PRICING_VIEWED"); event.put("source","pricing");
    event.put("userUuid",otherUuid); // ignored: caller cannot select another account
    call("POST","/api/activity",event,null,403);
    call("POST","/api/activity",event,jwt,204);
    call("POST","/api/activity",event,jwt,204);
    event.put("eventId",UUID.randomUUID().toString()); event.put("type","STRIPE_LINK_CLICKED");event.put("plan","b1");
    call("POST","/api/activity",event,jwt,204);
    call("GET","/api/admin/v1/activity",null,null,401);
    call("GET","/api/admin/v1/activity",null,jwt,401);
    String path="/api/admin/v1/activity?user="+uuid;
    JsonNode list=call("GET",path,null,viewer,200);
    assertEquals(2,list.get("totalElements").asInt());
    assertEquals(uuid,list.get("content").get(0).get("userUuid").asText());
    assertTrue(list.get("content").get(0).get("occurredAt").asText().endsWith("Z"));
    assertFalse(list.toString().contains("password"));
    JsonNode stats=call("GET","/api/admin/v1/activity/summary?user="+uuid,null,root,200);
    assertEquals(2,stats.get("total").asInt()); assertEquals(1,stats.get("users").asInt());
    assertEquals(2,stats.get("counts").size()); assertEquals(2,stats.get("trend").size());
    assertEquals(1,call("GET",path+"&type=PRICING_VIEWED",null,viewer,200).get("totalElements").asInt());
    assertEquals(0,call("GET","/api/admin/v1/activity?user="+otherUuid,null,root,200).get("totalElements").asInt());
    JsonNode grouped=call("GET",path+"&grouped=true",null,viewer,200);
    assertEquals(2,grouped.get("content").size());
    assertEquals(1,grouped.get("content").get(0).get("total").asInt());
    assertEquals(0,call("GET",path+"&page=1&size=2",null,root,200).get("content").size());
    call("GET",path+"&size=101",null,root,400);
    call("GET",path+"&type=BAD",null,root,400);
    call("GET",path+"&from=bad",null,root,400);
    call("GET",path+"&from=2020-01-01T00:00:00Z&to=2026-01-01T00:00:00Z",null,root,400);
    event.put("type","EXAM_STARTED");call("POST","/api/activity",event,jwt,400);
    event.put("type","STRIPE_LINK_CLICKED");event.put("plan","fake");call("POST","/api/activity",event,jwt,400);
    event.put("plan","b1");event.put("source","x".repeat(101));call("POST","/api/activity",event,jwt,400);
    event.put("source","pricing");event.put("eventId","bad");call("POST","/api/activity",event,jwt,400);
  }

  @Test
  void activityUsesUtcBoundariesAndRollsBackWithExamTransaction() throws Exception {
    JsonNode u=createUser();String uuid=users.findByEmail(u.get("email").asText()).getUuid();
    UUID session=UUID.randomUUID();
    activityTx.execute(status -> { activity.exam("EXAM_STARTED",uuid,session); status.setRollbackOnly(); return null; });
    assertEquals(0,call("GET","/api/admin/v1/activity?user="+uuid,null,root,200).get("totalElements").asInt());
    activity.exam("EXAM_STARTED",uuid,session);
    activityDb.update("update user_activity set occurred_at=? where user_uuid=?",java.time.LocalDateTime.parse("2026-11-01T23:59:59"),uuid);
    String base="/api/admin/v1/activity?user="+uuid+"&from=2026-11-01T00:00:00Z&to=2026-11-02T00:00:00Z";
    JsonNode list=call("GET",base,null,root,200);
    assertEquals(1,list.get("totalElements").asInt());
    assertEquals("2026-11-01T23:59:59Z",list.get("content").get(0).get("occurredAt").asText());
    activityDb.update("update user_activity set occurred_at=? where user_uuid=?",java.time.LocalDateTime.parse("2026-11-02T00:00:00"),uuid);
    assertEquals(0,call("GET",base,null,root,200).get("totalElements").asInt());
  }

  @Test
  void adminAuthenticationAndRoleRestrictions() throws Exception {
    call("GET", "/api/admin/v1/dashboard", null, null, 401);
    String viewer = staff("Viewer"), manager = staff("Admin");
    call("GET", "/api/admin/v1/users", null, viewer, 200);
    call("POST", "/api/admin/v1/users", newUser(), viewer, 403);
    call("GET", "/api/admin/v1/admins", null, manager, 403);
    call("POST", "/api/admin/update", Map.of(), manager, 403);
    JsonNode user = createUser();
    String token = JwtUtils.getJWTToken(user.get("email").asText(), "ROLE_USER");
    call("GET", "/api/admin/v1/dashboard", null, token, 401);
    assertFalse(user.has("password"));
    assertFalse(user.has("resetPasswordToken"));
    call("DELETE", "/api/admin/v1/session", null, viewer, 204);
    call("GET", "/api/admin/v1/users", null, viewer, 401);
  }

  @Test
  void userManagementPreservesLegacyLoginAndAddsDeactivation() throws Exception {
    JsonNode user = createUser();
    String email = user.get("email").asText();
    String encrypted = users.findByEmail(email).getPassword();
    assertEquals("Learner-Password-493!", AESEncryption.decrypt(encrypted));
    JsonNode login =
        call(
            "POST",
            "/public/user/login",
            Map.of("email", email, "password", "Learner-Password-493!"),
            null,
            200);
    String token = login.get("token").asText();
    call("GET", "/api/user/get", null, token, 200);
    ObjectNode changes = update(user);
    changes.put("enabled", false);
    JsonNode saved =
        call("PUT", "/api/admin/v1/users/" + user.get("id").asLong(), changes, root, 200);
    assertFalse(saved.get("enabled").asBoolean());
    call("GET", "/api/user/get", null, token, 403);
    call("PUT", "/api/admin/v1/users/" + user.get("id").asLong(), changes, root, 409);
  }

  @Test
  void accessHistoryPlanSettingsAndPaginationUseRealRecords() throws Exception {
    JsonNode user = createUser();
    long id = user.get("id").asLong();
    ObjectNode in = json.createObjectNode();
    in.put("plan", "b1");
    in.put("quota", 7);
    in.put("status", "active");
    in.put("version", user.get("subscription").get("version").asLong());
    in.put("reason", "Courtesy access");
    JsonNode sub = call("PUT", "/api/admin/v1/users/" + id + "/subscription", in, root, 200);
    assertEquals(7, sub.get("quota").asInt());
    assertEquals(7, users.findByEmail(user.get("email").asText()).getSubscription().getQuota());
    JsonNode history =
        call(
            "GET",
            "/api/admin/v1/subscriptions/" + sub.get("id").asLong() + "/history",
            null,
            root,
            200);
    assertEquals(1, history.get("totalElements").asInt());
    call("GET", "/api/admin/v1/users?size=101", null, root, 400);
    call("GET", "/api/admin/v1/subscriptions?plan=b1&status=active", null, root, 200);
    JsonNode plans = call("GET", "/api/admin/v1/plans", null, root, 200);
    ObjectNode p = null;
    for (JsonNode x : plans) if (x.get("code").asText().equals("b1")) p = (ObjectNode) x.deepCopy();
    assertNotNull(p);
    p.put("title", "Admin grant B1");
    p.put("reason", "Admin grant configuration");
    call("PUT", "/api/admin/v1/plans/b1", p, root, 200);
  }

  @Test
  void examDraftVersioningAndTemplateLifecycle() throws Exception {
    JsonNode exam = call("POST", "/api/admin/v1/exams", exam("PREDEFINED"), root, 201);
    String id = exam.get("id").asText(),
        part = exam.get("sections").get(0).get("parts").get(0).get("id").asText();
    mvc.perform(
            put("/api/admin/v1/exams/" + id + "/publication?active=true")
                .header("Authorization", "Bearer " + root)
                .header("If-Match", exam.get("entityVersion").asLong())
                .header("X-Reason", "Publish"))
        .andExpect(status().isConflict());
    ObjectNode t = json.createObjectNode();
    t.put("templateKey", "test_original_stack");
    t.put("instructions", "Choose the correct option.");
    ObjectNode q = t.putArray("questions").addObject();
    q.put("number", "1");
    q.put("prompt", "Which option is correct?");
    q.put("type", "SINGLE_CHOICE");
    q.put("maximumScore", 1);
    q.putArray("options").addObject().put("key", "a").put("text", "Correct option");
    q.putObject("correctAnswer").putArray("acceptedOptionKeys").add("a");
    JsonNode saved =
        body(
            mvc.perform(
                    post("/api/admin/v1/parts/" + part + "/templates")
                        .header("Authorization", "Bearer " + root)
                        .header("X-Reason", "Create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(t)))
                .andExpect(status().isCreated())
                .andReturn());
    mvc.perform(
            put("/api/admin/v1/exams/" + id + "/publication?active=true")
                .header("Authorization", "Bearer " + root)
                .header("If-Match", exam.get("entityVersion").asLong())
                .header("X-Reason", "Publish"))
        .andExpect(status().isOk());
    JsonNode learner=createUser();
    String learnerUuid=users.findByEmail(learner.get("email").asText()).getUuid();
    String learnerToken=JwtUtils.getJWTToken(learner.get("email").asText(),"ROLE_USER");
    Object startInput=Map.of("userId","untrusted-client-id","provider","CUSTOM","level","B1","examCode",exam.get("code").asText());
    JsonNode started=call("POST","/api/exams/sessions",startInput,learnerToken,201);
    JsonNode resumed=call("POST","/api/exams/sessions",startInput,learnerToken,201);
    assertEquals(started.get("sessionId"),resumed.get("sessionId"));
    String activityPath="/api/admin/v1/activity/summary?user="+learnerUuid;
    assertEquals(1,call("GET",activityPath,null,root,200).get("total").asInt());
    String submitPath="/api/exams/sessions/"+started.get("sessionId").asText()+"/submit";
    call("POST",submitPath,Map.of("compactAnswers","99a"),learnerToken,400);
    assertEquals(1,call("GET",activityPath,null,root,200).get("total").asInt());
    call("POST",submitPath,Map.of("compactAnswers","1a"),learnerToken,200);
    call("POST",submitPath,Map.of("compactAnswers","1a"),learnerToken,200);
    assertEquals(2,call("GET",activityPath,null,root,200).get("total").asInt());
    mvc.perform(
            delete("/api/admin/v1/templates/" + saved.get("id").asText())
                .header("Authorization", "Bearer " + root)
                .header("If-Match", saved.get("entityVersion").asLong())
                .header("X-Reason", "Delete"))
        .andExpect(status().isConflict());
    JsonNode copy =
        body(
            mvc.perform(
                    post("/api/admin/v1/exams/" + id + "/clone")
                        .header("Authorization", "Bearer " + root)
                        .header("X-Reason", "Clone"))
                .andExpect(status().isCreated())
                .andReturn());
    assertEquals(2, copy.get("definitionVersion").asInt());
    assertFalse(copy.get("active").asBoolean());
  }

  @Test
  void dashboardStaffInboxAndPromoManagement() throws Exception {
    createUser();
    JsonNode stats = call("GET", "/api/admin/v1/dashboard?days=7", null, root, 200);
    assertEquals(users.count(), stats.get("totalUsers").asLong());
    assertEquals(7, stats.get("trend").size());
    call("GET", "/api/admin/v1/dashboard?days=1", null, root, 400);
    String code = "CODE_" + UUID.randomUUID().toString().replace("-", "");
    call(
        "POST",
        "/api/admin/v1/promos",
        Map.of("code", code, "plan", "tester", "active", true, "reason", "Inventory test"),
        root,
        201);
    assertEquals(
        1,
        call("GET", "/api/admin/v1/promos?q=" + code, null, root, 200)
            .get("totalElements")
            .asInt());
    ObjectNode staff = json.createObjectNode();
    staff.put("email", UUID.randomUUID() + "@staff.example.com");
    staff.put("firstname", "Support");
    staff.put("lastname", "Viewer");
    staff.put("password", "Dashboard-Staff-493!");
    staff.put("enabled", true);
    staff.put("reason", "Staff creation");
    staff.putArray("roles").add("Viewer");
    JsonNode created = call("POST", "/api/admin/v1/admins", staff, root, 201);
    assertFalse(created.has("password"));
    call("GET", "/api/admin/v1/admins", null, root, 200);
    call("GET", "/api/admin/v1/contacts", null, root, 200);
    call("GET", "/api/admin/v1/audit", null, root, 200);
    call("GET", "/api/admin/v1/sessions", null, root, 200);
  }

  JsonNode createPromo(String code, String plan, boolean active) throws Exception {
    return call(
        "POST",
        "/api/admin/v1/promos",
        Map.of("code", code, "plan", plan, "active", active, "reason", "Promo regression test"),
        root,
        201);
  }

  String customerToken(JsonNode user) {
    return JwtUtils.getJWTToken(user.get("email").asText(), "ROLE_User");
  }

  String redeemPromo(String code, JsonNode user) throws Exception {
    return call("POST", "/api/promocode/activate", code, customerToken(user), 200).asText();
  }

  @Test
  void adminPromosRedeemThroughExistingClientEndpointOncePerCustomer() throws Exception {
    String code = "CAMPAIGN_" + UUID.randomUUID().toString().replace("-", "");
    JsonNode promo = createPromo(code, "b1", true), first = createUser(), second = createUser();
    assertEquals("ACTIVATED", redeemPromo("  " + code.toLowerCase(Locale.ROOT) + "  ", first));
    var subscription = users.findByEmail(first.get("email").asText()).getSubscription();
    assertEquals("b1", subscription.getPlan());
    assertEquals(10, subscription.getQuota());
    assertTrue(Boolean.TRUE.equals(subscription.getIsActive()));
    long duration = subscription.getEndDate().getTime() - subscription.getStartDate().getTime();
    assertEquals(60L * 24 * 60 * 60 * 1000, duration);
    assertEquals("INVALID", redeemPromo(code, first));
    assertEquals("ACTIVATED", redeemPromo(code, second));
    assertEquals("INVALID", redeemPromo("UNKNOWN_" + UUID.randomUUID(), createUser()));
    call(
        "PUT",
        "/api/admin/v1/promos/" + promo.get("id").asLong(),
        Map.of("code", code, "plan", "b1", "active", false, "reason", "Disable promotion"),
        root,
        200);
    JsonNode third = createUser();
    assertEquals("INVALID", redeemPromo(code, third));
    assertEquals(
        "free", users.findByEmail(third.get("email").asText()).getSubscription().getPlan());
    call(
        "POST",
        "/api/admin/v1/promos",
        Map.of(
            "code",
            "starter4next",
            "plan",
            "b1",
            "active",
            true,
            "reason",
            "Reserved name rejected"),
        root,
        409);
    mvc.perform(
            delete("/api/admin/v1/promos/" + promo.get("id").asLong())
                .header("Authorization", "Bearer " + root)
                .header("X-Reason", "Test cleanup"))
        .andExpect(status().isNoContent());
    createPromo(code, "b1", true);
    assertEquals("INVALID", redeemPromo(code, first));
    JsonNode history =
        call(
            "GET",
            "/api/admin/v1/subscriptions/" + subscription.getId() + "/history",
            null,
            root,
            200);
    assertTrue(history.get("totalElements").asLong() > 0);
  }

  @Test
  void promoHonorsPlanSettingsAndSupportsUnlimitedWithoutChangingStripe() throws Exception {
    String code = "UNLIMITED_" + UUID.randomUUID().toString().replace("-", "");
    createPromo(code, "b1_unlimited", true);
    JsonNode customer = createUser();
    PlanSettings original = planSettings.findById("b1_unlimited").orElseThrow();
    int oldQuota = original.getQuota();
    Integer oldDays = original.getAccessDays();
    boolean oldEnabled = original.isEnabled();
    try {
      original.setEnabled(false);
      planSettings.saveAndFlush(original);
      assertEquals("INVALID", redeemPromo(code, customer));
      original = planSettings.findById("b1_unlimited").orElseThrow();
      original.setEnabled(true);
      original.setAccessDays(14);
      planSettings.saveAndFlush(original);
      assertEquals("ACTIVATED", redeemPromo(code, customer));
      var subscription = users.findByEmail(customer.get("email").asText()).getSubscription();
      assertEquals("b1_unlimited", subscription.getPlan());
      assertEquals(0, subscription.getQuota());
      assertTrue(customerAccess.hasAvailableQuota(subscription));
      assertEquals(
          14L * 24 * 60 * 60 * 1000,
          subscription.getEndDate().getTime() - subscription.getStartDate().getTime());
    } finally {
      original = planSettings.findById("b1_unlimited").orElseThrow();
      original.setEnabled(oldEnabled);
      original.setQuota(oldQuota);
      original.setAccessDays(oldDays);
      planSettings.saveAndFlush(original);
    }
  }

  @Test
  void concurrentPromoClaimsCannotGrantTwice() throws Exception {
    String code = "RACE_" + UUID.randomUUID().toString().replace("-", "");
    createPromo(code, "tester", true);
    JsonNode customer = createUser();
    var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
    var gate = new java.util.concurrent.CountDownLatch(1);
    try {
      java.util.concurrent.Callable<String> task =
          () -> {
            gate.await();
            return redeemPromo(code, customer);
          };
      var a = workers.submit(task);
      var b = workers.submit(task);
      gate.countDown();
      var results =
          List.of(
              a.get(15, java.util.concurrent.TimeUnit.SECONDS),
              b.get(15, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(1, results.stream().filter("ACTIVATED"::equals).count());
      assertEquals(1, results.stream().filter("INVALID"::equals).count());
      assertEquals(
          2, users.findByEmail(customer.get("email").asText()).getSubscription().getQuota());
    } finally {
      workers.shutdownNow();
    }
  }

  @Test
  void originalBuiltInCodesStillGrantTheirOriginalTesterAccess() throws Exception {
    for (String code : List.of("starter4next", "AICO2025")) {
      JsonNode customer = createUser();
      assertEquals("ACTIVATED", redeemPromo(code, customer));
      var subscription = users.findByEmail(customer.get("email").asText()).getSubscription();
      assertEquals("tester", subscription.getPlan());
      assertEquals(2, subscription.getQuota());
    }
  }
}
