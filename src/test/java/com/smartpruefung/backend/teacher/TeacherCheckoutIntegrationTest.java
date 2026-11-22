package com.smartpruefung.backend.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.User.Service.SubscriptionService;
import com.smartpruefung.backend.Utils.JwtUtils;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.*;
import java.util.*;
import java.time.Instant;
import javax.persistence.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties={"smartpruefung.teacher.checkout-enabled=true","smartpruefung.teacher.approval-required=false","smartpruefung.stripe.apisecret=sk_test_fixture","smartpruefung.stripe.webhooksecret=whsec_fixture","stripe.teacher.price.standard=price_teacher_standard","stripe.teacher.price.10plus=price_teacher_10plus","stripe.teacher.price.25plus=price_teacher_25plus","stripe.teacher.price.50plus=price_teacher_50plus"})
@AutoConfigureMockMvc @ActiveProfiles("admin-test")
public class TeacherCheckoutIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired TeacherPayments payments; @Autowired TeacherService teachers;
 @Autowired UsersRepository users; @Autowired SubscriptionService subscriptions; @Autowired PlatformTransactionManager manager;
 @PersistenceContext EntityManager em;
 @MockBean com.smartpruefung.backend.Shared.Service.EmailSenderService emails;
 @MockBean org.springframework.web.client.RestTemplate remote;
 String teacher,other,student,teacherId; User owner;
 StripeResponseGetter original;
 Map<String,Price> prices=new HashMap<>();Map<String,Session> sessions=new HashMap<>();Map<String,Object> lastParams;int checkoutRequests;boolean failCreate;
 TransactionTemplate tx(){return new TransactionTemplate(manager);}
 User user(){User u=new User();u.setUuid(UUID.randomUUID().toString());u.setEmail(u.getUuid()+"@example.test");u.setFirstname("Checkout");u.setLastname("Teacher");u=users.save(u);u.setSubscription(subscriptions.createFreeSubscription(u.getEmail()));return users.save(u);}
 String token(User u){return "Bearer "+JwtUtils.getJWTToken(u.getEmail(),"ROLE_USER");}
 @BeforeEach void setup()throws Exception{
  owner=user();teacherId=owner.getUuid();teacher=token(owner);other=token(user());student=token(user());teachers.apply(teacher);teachers.apply(other);
  for(var tier:SeatPricing.schedule()){Price p=new Price();p.setId("price_teacher_"+tier.get("code"));p.setActive(true);p.setType("one_time");p.setBillingScheme("per_unit");p.setCurrency("eur");p.setUnitAmount(((Integer)tier.get("unitPriceCents")).longValue());p.setProduct("prod_teacher_fixture");p.setTaxBehavior("inclusive");prices.put(p.getId(),p);}
  var getter=ApiResource.class.getDeclaredMethod("getGlobalResponseGetter");getter.setAccessible(true);original=(StripeResponseGetter)getter.invoke(null);
  ApiResource.setStripeResponseGetter(Mockito.mock(StripeResponseGetter.class,inv->{
   if(!"request".equals(inv.getMethod().getName()))return null;
   Object[] args=inv.getArguments();String path;Map<String,Object> params;
   if(args[0] instanceof ApiRequest){ApiRequest request=(ApiRequest)args[0];path=request.getPath();params=request.getParams();}
   else{path=(String)args[2];params=(Map<String,Object>)args[3];}
   if(path.contains("/prices/"))return prices.get(path.substring(path.lastIndexOf('/')+1));
   if(path.endsWith("/checkout/sessions")){
    checkoutRequests++;lastParams=params;
    String id=(String)((Map<String,Object>)params.get("metadata")).get("teacher_seat_order_id");
    TeacherSeatOrder committed=tx().execute(status->em.find(TeacherSeatOrder.class,id));assertNotNull(committed);assertEquals("PENDING",committed.status);assertNotNull(committed.stripePriceId);
    if(failCreate)throw new IllegalStateException("Stripe transport unavailable");
    Session session=new Session();session.setId("cs_fixture_"+id);session.setUrl("https://checkout.stripe.test/"+id);session.setMode("payment");session.setPaymentStatus("unpaid");session.setClientReferenceId((String)params.get("client_reference_id"));
    session.setMetadata((Map<String,String>)(Map)params.get("metadata"));session.setCurrency("eur");session.setAmountTotal((long)committed.totalCents);session.setCustomer((String)params.get("customer"));
    sessions.put(session.getId(),session);return session;
   }
   if(path.contains("/checkout/sessions/"))return sessions.get(path.substring(path.lastIndexOf('/')+1));
   throw new AssertionError("Unexpected Stripe request: "+path);
  }));
 }
 @AfterEach void cleanup(){ApiResource.setStripeResponseGetter(original);}
 JsonNode call(String method,String path,Object body,String auth,int code)throws Exception{
  var request=request(org.springframework.http.HttpMethod.valueOf(method),path).contentType(MediaType.APPLICATION_JSON);if(auth!=null)request.header("Authorization",auth);if(body!=null)request.content(json.writeValueAsBytes(body));
  var response=mvc.perform(request).andExpect(status().is(code)).andReturn().getResponse();return json.readTree(response.getContentAsString());
 }
 Event event(String type){Event e=new Event();e.setId("evt_fixture_"+UUID.randomUUID());e.setType(type);return e;}
 Session paid(String orderId){TeacherSeatOrder order=tx().execute(status->em.find(TeacherSeatOrder.class,orderId));Session s=sessions.get(order.stripeSessionId);s.setPaymentStatus("paid");
  String payload="{\"data\":[{\"quantity\":"+order.quantity+",\"price\":{\"id\":\""+order.stripePriceId+"\",\"unit_amount\":"+order.unitPriceCents+"}}]}";
  s.setLineItems(ApiResource.GSON.fromJson(payload,LineItemCollection.class));return s;
 }
 @Test void quantityBoundariesUseConfiguredPriceAndFixedQuantity()throws Exception{
  for(int qty:new int[]{1,9,10,24,25,49,50}){
   JsonNode result=call("POST","/api/teacher/seats/checkout",Map.of("quantity",qty,"priceId","price_cheaper_attack","unitPriceCents",1),teacher,200);
   var line=(Map<String,Object>)((List<?>)lastParams.get("line_items")).get(0);assertEquals("price_teacher_"+SeatPricing.tier(qty),line.get("price"));assertEquals((long)qty,((Number)line.get("quantity")).longValue());assertFalse(line.containsKey("price_data"));assertFalse(line.containsKey("adjustable_quantity"));assertEquals("payment",lastParams.get("mode"));assertEquals("TEACHER_SEATS",((Map<?,?>)lastParams.get("metadata")).get("purchaseType"));
   String order=result.get("orderId").asText();JsonNode before=call("GET","/api/teacher/seats/orders/"+order,null,teacher,200);assertEquals("PENDING",before.get("order").get("status").asText());assertEquals(0,before.get("seats").get("available").asInt());
   assertTrue(((String)lastParams.get("success_url")).contains("/teacher/billing?"));assertTrue(((String)lastParams.get("cancel_url")).contains("/teacher/seats?"));
   Session session=paid(order);Event e=event("checkout.session.completed");payments.fulfill(e,session);payments.fulfill(e,session);payments.fulfill(event("checkout.session.async_payment_succeeded"),session);
   JsonNode after=call("GET","/api/teacher/seats/orders/"+order,null,teacher,200);assertEquals(qty,after.get("seats").get("available").asInt());assertEquals(qty,after.get("seats").get("purchased").asInt());assertEquals(0,after.get("seats").get("assigned").asInt());
  }
 }
 @Test void onlyTeachersCanBuyAndOnlyOwnerCanReadOrder()throws Exception{
  call("POST","/api/teacher/seats/checkout",Map.of("quantity",1),null,403);call("POST","/api/teacher/seats/checkout",Map.of("quantity",1),student,403);call("GET","/api/teacher/seats/pricing",null,student,403);
  call("POST","/api/teacher/seats/checkout",Map.of("quantity",0),teacher,400);call("POST","/api/teacher/seats/checkout",Map.of("quantity",1001),teacher,400);call("POST","/api/teacher/seats/checkout",Map.of("quantity",15.5),teacher,400);call("POST","/api/teacher/seats/checkout",Map.of(),teacher,400);assertEquals(0,checkoutRequests);
  String id=call("POST","/api/teacher/seats/checkout",Map.of("quantity",1),teacher,200).get("orderId").asText();call("GET","/api/teacher/seats/orders/"+id,null,other,404);
 }
 @Test void validatesPricesWithoutRestrictingTaxBehavior()throws Exception{
  Price p=prices.get("price_teacher_10plus");p.setUnitAmount(1L);assertThrows(Exception.class,()->payments.checkout(teacher,15));p.setUnitAmount(1599L);p.setCurrency("usd");assertThrows(Exception.class,()->payments.checkout(teacher,15));p.setCurrency("eur");p.setProduct("prod_wrong");assertThrows(Exception.class,()->payments.checkout(teacher,15));p.setProduct("prod_teacher_fixture");p.setType("recurring");assertThrows(Exception.class,()->payments.checkout(teacher,15));assertEquals(0,checkoutRequests);assertEquals(0L,teachers.wallet(teacher).get("available"));
  p.setType("one_time");for(String behavior:new String[]{"inclusive","exclusive","unspecified"}){p.setTaxBehavior(behavior);call("POST","/api/teacher/seats/checkout",Map.of("quantity",15),teacher,200);}
  assertEquals(3,checkoutRequests);assertEquals(0L,teachers.wallet(teacher).get("available"));
 }
 @Test void reusesExistingCustomerAndNeverCreditsFromReturn()throws Exception{
  tx().execute(status->{User u=users.findByUUID(teacherId);u.getSubscription().setCustomerId("cus_teacher_existing");return null;});
  String id=call("POST","/api/teacher/seats/checkout",Map.of("quantity",15),teacher,200).get("orderId").asText();assertEquals("cus_teacher_existing",lastParams.get("customer"));assertFalse(lastParams.containsKey("customer_email"));
  for(int i=0;i<2;i++)assertEquals(0,call("GET","/api/teacher/seats/orders/"+id,null,teacher,200).get("seats").get("available").asInt());
  Session s=paid(id);s.setCustomer("cus_wrong");assertThrows(RuntimeException.class,()->payments.fulfill(event("checkout.session.completed"),s));s.setCustomer("cus_teacher_existing");payments.fulfill(event("checkout.session.completed"),s);assertEquals(15L,teachers.wallet(teacher).get("available"));
 }
 @Test void missingOrWrongPurchaseMetadataAndPriceCannotCredit()throws Exception{
  String id=call("POST","/api/teacher/seats/checkout",Map.of("quantity",10),teacher,200).get("orderId").asText();Session s=paid(id);s.getMetadata().remove("purchaseType");assertThrows(RuntimeException.class,()->payments.fulfill(event("checkout.session.completed"),s));s.getMetadata().put("purchaseType","TEACHER_SEATS");s.getLineItems().getData().get(0).getPrice().setId("price_wrong");assertThrows(RuntimeException.class,()->payments.fulfill(event("checkout.session.completed"),s));assertEquals(0L,teachers.wallet(teacher).get("available"));
 }
 @Test void canceledUnpaidFailedAndTransportFailureCreditZero()throws Exception{
  String id=call("POST","/api/teacher/seats/checkout",Map.of("quantity",5),teacher,200).get("orderId").asText();Session s=paid(id);s.setPaymentStatus("unpaid");payments.fulfill(event("checkout.session.completed"),s);assertEquals(0L,teachers.wallet(teacher).get("available"));payments.failedCheckout(event("checkout.session.async_payment_failed"),s,"FAILED");assertEquals("FAILED",call("GET","/api/teacher/seats/orders/"+id,null,teacher,200).get("order").get("status").asText());assertEquals(0L,teachers.wallet(teacher).get("available"));
  String expired=call("POST","/api/teacher/seats/checkout",Map.of("quantity",5),teacher,200).get("orderId").asText();payments.failedCheckout(event("checkout.session.expired"),sessions.values().stream().filter(x->x.getClientReferenceId().equals("teacher-seat:"+expired)).findFirst().orElseThrow(),"EXPIRED");assertEquals(0L,teachers.wallet(teacher).get("available"));failCreate=true;assertThrows(Exception.class,()->payments.checkout(teacher,5));assertEquals("FAILED",teachers.orders(teacher).get(0).status);assertEquals(0L,teachers.wallet(teacher).get("available"));
 }
 @Test void verifiedDelayedPaymentWebhookRoutesTeacherPurchaseAndCreditsOnce()throws Exception{
  String id=call("POST","/api/teacher/seats/checkout",Map.of("quantity",15),teacher,200).get("orderId").asText();Session session=paid(id);session.setPaymentStatus("unpaid");
  String completed=json.writeValueAsString(Map.of("id","evt_completed_"+id,"object","event","type","checkout.session.completed","data",Map.of("object",Map.of("id",session.getId(),"object","checkout.session"))));
  signed(completed);assertEquals(0L,teachers.wallet(teacher).get("available"));
  session.setPaymentStatus("paid");String settled=json.writeValueAsString(Map.of("id","evt_settled_"+id,"object","event","type","checkout.session.async_payment_succeeded","data",Map.of("object",Map.of("id",session.getId(),"object","checkout.session"))));signed(settled);signed(settled);assertEquals(15L,teachers.wallet(teacher).get("available"));assertEquals("free",users.findByUUID(teacherId).getSubscription().getPlan());
 }
 void signed(String payload)throws Exception{
  long now=Instant.now().getEpochSecond();javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("whsec_fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));StringBuilder signature=new StringBuilder();for(byte b:mac.doFinal((now+"."+payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)))signature.append(String.format("%02x",b));
  mvc.perform(post("/public/stripe/webhook").contentType(MediaType.APPLICATION_JSON).header("Stripe-Signature","t="+now+",v1="+signature).content(payload)).andExpect(status().isOk());
 }
 public static void main(String[] args)throws Exception{
  var context=new org.springframework.test.context.TestContextManager(TeacherCheckoutIntegrationTest.class);int count=0;
  try{for(var method:TeacherCheckoutIntegrationTest.class.getDeclaredMethods())if(method.isAnnotationPresent(Test.class)){
   TeacherCheckoutIntegrationTest test=new TeacherCheckoutIntegrationTest();context.prepareTestInstance(test);test.setup();try{method.invoke(test);System.out.println("PASS "+method.getName());count++;}catch(java.lang.reflect.InvocationTargetException ex){throw new AssertionError(method.getName(),ex.getCause());}finally{test.cleanup();}
  }System.out.println("Teacher checkout tests passed: "+count);}finally{context.getTestContext().markApplicationContextDirty(org.springframework.test.annotation.DirtiesContext.HierarchyMode.EXHAUSTIVE);}
 }
}
