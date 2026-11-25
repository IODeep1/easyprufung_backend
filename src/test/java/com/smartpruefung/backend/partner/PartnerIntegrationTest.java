package com.smartpruefung.backend.partner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Utils.JwtUtils;
import com.fasterxml.jackson.databind.*;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import javax.persistence.*;
import java.time.*;
import java.util.*;

@org.springframework.test.context.TestPropertySource(locations="classpath:integration-fixtures.properties")
@SpringBootTest(properties={"smartpruefung.guest.exam-code=GUEST_TEST","smartpruefung.partner.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","smartpruefung.partner.scheduling-enabled=false","smartpruefung.partner.live-mode=false","smartpruefung.stripe.priceidb1=price_10","smartpruefung.stripe.priceidb1unlimited=price_unlimited","logging.level.root=ERROR"})
@AutoConfigureMockMvc @ActiveProfiles("admin-test")
class PartnerIntegrationTest {
 @Autowired PartnerService service;@Autowired PartnerStripeService processing;@Autowired PartnerCrypto crypto;@Autowired UsersRepository users;@Autowired PlatformTransactionManager manager;@Autowired ObjectMapper json;@Autowired MockMvc mvc;
 @PersistenceContext EntityManager em;
 @MockBean PartnerStripeGateway stripe;
 @MockBean org.springframework.web.client.RestTemplate google;
 @MockBean com.smartpruefung.backend.Shared.Service.EmailSenderService emails;
 TransactionTemplate tx;User owner,buyer;String auth;String id;
 @BeforeEach void setup() throws Exception {tx=new TransactionTemplate(manager);owner=user();buyer=user();auth="Bearer "+JwtUtils.getJWTToken(owner.getEmail(),"USER");PartnerService.Input b=new PartnerService.Input();b.displayName="Partner Test";b.contactEmail=owner.getEmail();b.country="DE";b.approach="I help German language learners in my community.";PartnerService.Social social=new PartnerService.Social();social.platform="Instagram";social.profile="@partner";b.socials=List.of(social);b.termsAccepted=true;b.privacyAccepted=true;b.termsVersion=PartnerService.TERMS;b.payout=new PartnerService.PayoutInput();b.payout.method="PAYPAL";b.payout.holder="Partner Test";b.payout.paypalEmail="payout@example.com";Map<?,?> result=(Map<?,?>)service.apply(auth,b);id=(String)result.get("id");tx.execute(s->{PartnerProfile p=em.find(PartnerProfile.class,id);p.status="APPROVED";p.payoutVerified=true;PartnerCode c=new PartnerCode();c.partnerId=id;c.promotionCodeId="promo_"+id;c.displayCode="TEST10";c.couponId="coupon_10";c.validFrom=Instant.now().minusSeconds(100);em.persist(c);return null;});}
 User user(){User u=new User();u.setUuid(UUID.randomUUID().toString());u.setEmail(UUID.randomUUID()+"@example.com");u.setFirstname("Partner");u.setLastname("Test");return users.save(u);}
 PartnerService.Input duplicate(){PartnerService.Input b=new PartnerService.Input();b.termsAccepted=true;b.privacyAccepted=true;b.termsVersion=PartnerService.TERMS;return b;}
 @Test void applicationEncryptsAndMasksAndOwnershipIsEnforced() throws Exception {String encoded=json.writeValueAsString(service.me(auth));assertFalse(encoded.contains("payout@example.com"));assertTrue(encoded.contains("p•••@example.com"));assertThrows(Exception.class,()->service.apply(auth,duplicate()));String other="Bearer "+JwtUtils.getJWTToken(buyer.getEmail(),"USER");assertEquals("NOT_REGISTERED",((Map<?,?>)service.me(other)).get("status"));mvc.perform(get("/api/partners/me")).andExpect(status().isForbidden());mvc.perform(get("/api/partners/me").header("Authorization",auth)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));mvc.perform(put("/api/admin/v1/partners/"+id+"/decision").header("Authorization",auth).header("If-Match",0).contentType("application/json").content("{\"status\":\"APPROVED\",\"reason\":\"test\"}")).andExpect(status().isUnauthorized());}
 @Test void exactMoneyAndEncryption(){assertEquals(302,PartnerMath.commission(1512,2000));assertEquals(605,PartnerMath.commission(3024,2000));assertEquals(101,PartnerMath.refundTarget(302,600,1799));assertEquals(302,PartnerMath.refundTarget(302,1799,1799));String cipher=crypto.encrypt("private");assertFalse(cipher.contains("private"));assertEquals("private",crypto.decrypt(cipher));assertNotEquals(cipher,crypto.encrypt("private"));}
 void credit(long cents){tx.execute(s->{PartnerSale sale=new PartnerSale();sale.partnerId=id;sale.checkoutSessionId="cs_"+UUID.randomUUID();sale.commissionCents=cents;sale.qualified=true;em.persist(sale);PartnerEntry e=new PartnerEntry();e.partnerId=id;e.saleId=sale.id;e.sourceKey="test:"+UUID.randomUUID();e.kind="EARNED";e.amountCents=cents;e.eligibleAt=Instant.now().minusSeconds(1);em.persist(e);return null;});}
 @Test void thresholdReservationIdempotencyAndSettlementPreserveLifetime(){credit(4999);assertThrows(Exception.class,()->service.requestPayout(auth,"request-0001"));credit(1);PartnerPayout pay=(PartnerPayout)service.requestPayout(auth,"request-0001");assertEquals(5000,pay.amountCents);assertEquals(pay.id,((PartnerPayout)service.requestPayout(auth,"request-0001")).id);assertThrows(Exception.class,()->service.requestPayout(auth,"request-0002"));assertEquals(5000L,service.overview(auth).get("reservedCents"));org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test-admin","",List.of()));PartnerPayout approved=(PartnerPayout)service.settle(pay.id,pay.version,"approve",null,null,"Verified transfer",false);assertThrows(Exception.class,()->service.settle(pay.id,approved.version,"mark-paid","ref",Instant.now(),"Transfer complete",false));PartnerPayout settled=(PartnerPayout)service.settle(pay.id,approved.version,"mark-paid","bank-transfer-001",Instant.now(),"Transfer complete",true);assertEquals("MARKED_PAID",settled.status);assertEquals(5000L,service.overview(auth).get("paidCents"));assertEquals(5000L,service.overview(auth).get("lifetimeEarnedCents"));assertEquals(0L,service.overview(auth).get("availableCents"));assertEquals(2L,service.overview(auth).get("lifetimeQualifiedUses"));}
 Session checkout(String sid){return ApiResource.GSON.fromJson("{\"id\":\""+sid+"\",\"mode\":\"payment\",\"payment_status\":\"paid\",\"livemode\":false,\"currency\":\"eur\",\"amount_total\":1799,\"payment_intent\":\"pi_"+sid+"\",\"customer_details\":{\"email\":\""+buyer.getEmail()+"\"},\"line_items\":{\"data\":[{\"price\":{\"id\":\"price_10\"}}],\"has_more\":false},\"total_details\":{\"amount_tax\":287,\"amount_shipping\":0,\"breakdown\":{\"discounts\":[{\"amount\":200,\"discount\":{\"id\":\"di_1\",\"promotion_code\":\"promo_"+id+"\"}}],\"taxes\":[]}}}",Session.class);}
 PartnerJob job(String sid,String event){return tx.execute(s->{PartnerJob j=new PartnerJob();j.eventId=event;j.eventType="checkout.session.completed";j.objectId=sid;j.eventAt=Instant.now();em.persist(j);return j;});}
 void mockPromo() throws Exception {PromotionCode p=ApiResource.GSON.fromJson("{\"id\":\"promo_"+id+"\",\"livemode\":false,\"active\":true,\"coupon\":{\"id\":\"coupon_10\",\"percent_off\":10}}",PromotionCode.class);when(stripe.promotion("promo_"+id)).thenReturn(p);}
 @Test void paidSessionDeduplicationSnapshotAndRefundCarryForward() throws Exception {String sid="cs_"+UUID.randomUUID();Session s=checkout(sid);when(stripe.session(sid)).thenReturn(s);mockPromo();PartnerJob j=job(sid,"evt_"+UUID.randomUUID());processing.process(j.id);processing.process(j.id);processing.process(job(sid,"evt_async_"+UUID.randomUUID()).id);assertEquals(302L,service.overview(auth).get("pendingCents"));assertEquals(1L,service.overview(auth).get("lifetimeQualifiedUses"));tx.execute(t->{PartnerProfile p=em.find(PartnerProfile.class,id);p.commissionBps=5000;return null;});Charge charge=ApiResource.GSON.fromJson("{\"object\":\"charge\",\"id\":\"ch_test\",\"payment_intent\":\"pi_"+sid+"\",\"livemode\":false,\"amount_refunded\":1799}",Charge.class);when(stripe.charge("ch_test")).thenReturn(charge);when(stripe.disputes("ch_test")).thenReturn(List.of());PartnerJob refund=tx.execute(t->{PartnerJob r=new PartnerJob();r.eventId="evt_refund_"+UUID.randomUUID();r.eventType="charge.refunded";r.objectId="ch_test";em.persist(r);return r;});processing.process(refund.id);processing.process(refund.id);assertEquals(302L,service.overview(auth).get("reversedCents"));assertEquals(0L,service.overview(auth).get("pendingCents"));assertEquals(302L,service.overview(auth).get("lifetimeEarnedCents"));}
 @Test void failedStripeReadIsDurableAndRetryable() throws Exception {String sid="cs_"+UUID.randomUUID();when(stripe.session(sid)).thenThrow(new RuntimeException("timeout"));PartnerJob j=job(sid,"evt_fail_"+UUID.randomUUID());processing.process(j.id);assertEquals("FAILED",tx.execute(t->em.find(PartnerJob.class,j.id).status));doReturn(checkout(sid)).when(stripe).session(sid);mockPromo();processing.process(j.id);assertEquals("PROCESSED",tx.execute(t->em.find(PartnerJob.class,j.id).status));assertEquals(302L,service.overview(auth).get("pendingCents"));}
 @Test void selfPurchasesAndTeacherSeatsAreExcluded() throws Exception {String sid="cs_"+UUID.randomUUID();Session s=checkout(sid);s.getCustomerDetails().setEmail(owner.getEmail());when(stripe.session(sid)).thenReturn(s);processing.process(job(sid,"evt_self_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));s.getCustomerDetails().setEmail(buyer.getEmail());s.setClientReferenceId(owner.getUuid());processing.process(job(sid,"evt_self_beneficiary_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));s.setMetadata(Map.of("purchaseType","TEACHER_SEATS"));processing.process(job(sid,"evt_teacher_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));}
 @Autowired com.smartpruefung.backend.Admin.Repository.AdminsRepository admins;
 @Autowired com.smartpruefung.backend.Admin.Repository.RolesRepository roles;
 @Autowired com.smartpruefung.backend.Admin.Console.Repository.AdminCredentialRepository credentials;
 String staff(String role) throws Exception {String email=UUID.randomUUID()+"@staff.example.com";com.smartpruefung.backend.Admin.Admin a=new com.smartpruefung.backend.Admin.Admin();a.setEmail(email);a.setFirstname("Partner");a.setLastname(role);a.setPassword(com.smartpruefung.backend.Security.AESEncryption.encrypt("Legacy-Password-493!"));a.addRole(roles.findByName(role));a=admins.save(a);com.smartpruefung.backend.Admin.Console.Model.AdminCredential c=new com.smartpruefung.backend.Admin.Console.Model.AdminCredential();c.setId(a.getId());c.setPasswordHash(com.smartpruefung.backend.Admin.Console.Security.AdminPasswords.encode("Dashboard-Password-493!"));credentials.save(c);return json.readTree(mvc.perform(post("/public/admin/session").contentType("application/json").content(json.writeValueAsBytes(Map.of("email",email,"password","Dashboard-Password-493!")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();}
 @Test void viewerCannotApproveChangeRatesOrRevealDestinations() throws Exception {String token="Bearer "+staff("Viewer");mvc.perform(get("/api/admin/v1/partners/"+id).header("Authorization",token)).andExpect(status().isOk());for(String path:List.of("decision","commission","payout-verification"))mvc.perform(put("/api/admin/v1/partners/"+id+"/"+path).header("Authorization",token).header("If-Match",0).contentType("application/json").content("{\"status\":\"APPROVED\",\"reason\":\"reviewed\"}")).andExpect(status().isForbidden());mvc.perform(post("/api/admin/v1/partners/"+id+"/payout-details").header("Authorization",token).contentType("application/json").content("{\"reason\":\"payment verification\"}")).andExpect(status().isForbidden());}
 @Test void adminRateChangesRequireCurrentVersionAndLeaveEarlierSalesAlone() throws Exception {String token="Bearer "+staff("Admin");credit(302);long version=tx.execute(t->em.find(PartnerProfile.class,id).version);mvc.perform(put("/api/admin/v1/partners/"+id+"/commission").header("Authorization",token).header("If-Match",version).contentType("application/json").content("{\"commissionBps\":2500,\"discountBps\":1000,\"reason\":\"New agreed rate\"}")).andExpect(status().isOk());mvc.perform(put("/api/admin/v1/partners/"+id+"/commission").header("Authorization",token).header("If-Match",version).contentType("application/json").content("{\"commissionBps\":3000,\"reason\":\"stale request\"}")).andExpect(status().isConflict());assertEquals(302L,service.overview(auth).get("lifetimeEarnedCents"));}
 @Test void parallelRequestsCannotReserveSameEntries() throws Exception {credit(5000);java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);try{java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.Callable<Boolean> request=()->{start.await();try{service.requestPayout(auth,UUID.randomUUID().toString());return true;}catch(Exception e){return false;}};var one=pool.submit(request);var two=pool.submit(request);start.countDown();assertNotEquals(one.get(),two.get());assertEquals(5000L,service.overview(auth).get("reservedCents"));}finally{pool.shutdownNow();}}
 @Test void payoutChangesReleaseReservationsAndRequireVerification(){credit(5000);PartnerPayout payout=(PartnerPayout)service.requestPayout(auth,"request-verify");PartnerService.PayoutInput method=new PartnerService.PayoutInput();method.method="BANK";method.holder="Partner Test";method.iban="DE89370400440532013000";method.bic="COBADEFFXXX";Map<?,?> result=(Map<?,?>)service.payoutMethod(auth,method);assertEquals(false,result.get("payoutVerified"));assertEquals("DE •••• 3000",result.get("payoutMasked"));assertEquals(5000L,service.overview(auth).get("availableCents"));assertEquals("CANCELLED",tx.execute(t->em.find(PartnerPayout.class,payout.id).status));assertThrows(Exception.class,()->service.requestPayout(auth,"request-new-bank"));}
 String qualify() throws Exception {String sid="cs_"+UUID.randomUUID();when(stripe.session(sid)).thenReturn(checkout(sid));mockPromo();processing.process(job(sid,"evt_"+UUID.randomUUID()).id);return sid;}
 void reverse(String sid,long refunded,String disputeStatus) throws Exception {Charge c=ApiResource.GSON.fromJson("{\"object\":\"charge\",\"id\":\"ch_"+sid+"\",\"payment_intent\":\"pi_"+sid+"\",\"amount_refunded\":"+refunded+",\"livemode\":false}",Charge.class);when(stripe.charge(c.getId())).thenReturn(c);if(disputeStatus==null)when(stripe.disputes(c.getId())).thenReturn(List.of());else {Dispute d=new Dispute();d.setStatus(disputeStatus);when(stripe.disputes(c.getId())).thenReturn(List.of(d));}PartnerJob j=tx.execute(t->{PartnerJob p=new PartnerJob();p.eventId="evt_reverse_"+UUID.randomUUID();p.eventType="charge.refunded";p.objectId=c.getId();em.persist(p);return p;});processing.process(j.id);}
 @Test void partialRefundsAreIncrementalAndDisputeWinRestoresOnlyUnrefundedCommission() throws Exception {String sid=qualify();reverse(sid,600,null);assertEquals(101L,service.overview(auth).get("reversedCents"));reverse(sid,600,null);assertEquals(101L,service.overview(auth).get("reversedCents"));reverse(sid,900,null);assertEquals(151L,service.overview(auth).get("reversedCents"));reverse(sid,900,"needs_response");assertEquals(302L,service.overview(auth).get("reversedCents"));reverse(sid,900,"won");assertEquals(151L,service.overview(auth).get("pendingCents"));assertEquals(151L,service.overview(auth).get("reversedCents"));}
 @Test void refundAfterPaidCreatesNegativeCarryForwardWithoutChangingPaidHistory() throws Exception {String sid=qualify();credit(5000);tx.execute(t->{em.createQuery("update PartnerEntry set eligibleAt=:now where partnerId=:p").setParameter("now",Instant.now().minusSeconds(1)).setParameter("p",id).executeUpdate();return null;});PartnerPayout payout=(PartnerPayout)service.requestPayout(auth,"request-paid-refund");org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test-admin","",List.of()));PartnerPayout approved=(PartnerPayout)service.settle(payout.id,payout.version,"approve",null,null,"Approval",false);service.settle(payout.id,approved.version,"mark-paid","bank-002",Instant.now(),"Transferred externally",true);reverse(sid,1799,null);assertEquals(5302L,service.overview(auth).get("paidCents"));assertEquals(-302L,service.overview(auth).get("availableCents"));assertEquals("MARKED_PAID",tx.execute(t->em.find(PartnerPayout.class,payout.id).status));}
 @Test void taxMissingAndStackedDiscountsStayInReview() throws Exception {String sid="cs_"+UUID.randomUUID();Session s=checkout(sid);s.getTotalDetails().setAmountTax(null);when(stripe.session(sid)).thenReturn(s);mockPromo();processing.process(job(sid,"evt_tax_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));assertEquals("REQUIRES_REVIEW",tx.execute(t->service.first(PartnerSale.class,"checkoutSessionId",sid).state));s.getTotalDetails().setAmountTax(287L);s.getTotalDetails().getBreakdown().getDiscounts().add(s.getTotalDetails().getBreakdown().getDiscounts().get(0));processing.process(job(sid,"evt_stacked_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));}
 @Test void unknownCodeUnpaidAndModeMismatchDoNotEarn() throws Exception {String sid="cs_"+UUID.randomUUID();Session s=checkout(sid);s.getTotalDetails().getBreakdown().getDiscounts().get(0).getDiscount().setPromotionCode("promo_unknown");when(stripe.session(sid)).thenReturn(s);processing.process(job(sid,"evt_unknown_"+UUID.randomUUID()).id);s=checkout(sid);s.setPaymentStatus("unpaid");when(stripe.session(sid)).thenReturn(s);processing.process(job(sid,"evt_unpaid_"+UUID.randomUUID()).id);s.setPaymentStatus("paid");s.setLivemode(true);processing.process(job(sid,"evt_wrong_mode_"+UUID.randomUUID()).id);assertEquals(0L,service.overview(auth).get("lifetimeEarnedCents"));}
 @Test void codeLinkAcceptsUnrestrictedCouponsAndNeverReassignsIds() throws Exception {PromotionCode promo=ApiResource.GSON.fromJson("{\"id\":\"promo_new\",\"active\":true,\"livemode\":false,\"code\":\"NEW10\",\"coupon\":{\"id\":\"coupon_new\",\"valid\":true,\"duration\":\"once\",\"percent_off\":10,\"applies_to\":{\"products\":[\"prod_10\",\"prod_unlimited\"]}}}",PromotionCode.class);promo.getCoupon().setAppliesTo(null);when(stripe.promotion("promo_new")).thenReturn(promo);Price ten=new Price();ten.setProduct("prod_10");Price unlimited=new Price();unlimited.setProduct("prod_unlimited");when(stripe.price("price_10")).thenReturn(ten);when(stripe.price("price_unlimited")).thenReturn(unlimited);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test-admin","",List.of()));long version=tx.execute(t->em.find(PartnerProfile.class,id).version);processing.link(id,version,"promo_new","Validated Stripe code");assertThrows(Exception.class,()->processing.link(id,version,"promo_new","Cannot reuse code"));verify(stripe,never()).price(anyString());}

 @Test void inboxSkipsUnpaidCompletionAndDeduplicatesVerifiedPaidEvents(){
  String eventId="evt_inbox_"+UUID.randomUUID();
  String payload="{\"object\":\"event\",\"id\":\""+eventId+"\",\"type\":\"checkout.session.completed\",\"created\":"+(System.currentTimeMillis()/1000)+",\"livemode\":false,\"data\":{\"object\":{\"object\":\"checkout.session\",\"id\":\"cs_inbox\",\"payment_status\":\"unpaid\"}}}";
  Event unpaid=ApiResource.GSON.fromJson(payload,Event.class);processing.enqueue(unpaid);
  assertNull(tx.execute(t->service.first(PartnerJob.class,"eventId",eventId)));
  Event paid=ApiResource.GSON.fromJson(payload.replace("unpaid","paid"),Event.class);processing.enqueue(paid);processing.enqueue(paid);
  long queued=tx.execute(t->em.createQuery("select count(j) from PartnerJob j where j.eventId=:e",Long.class).setParameter("e",eventId).getSingleResult());assertEquals(1L,queued);
 }
 @Test void knownFraudulentChargeCannotCreatePayableCommission() throws Exception {
  String sid=qualify();Charge c=new Charge();c.setId("ch_fraud");c.setPaymentIntent("pi_"+sid);c.setLivemode(false);c.setAmountRefunded(0L);Charge.FraudDetails details=new Charge.FraudDetails();details.setUserReport("fraudulent");c.setFraudDetails(details);when(stripe.charge("ch_fraud")).thenReturn(c);when(stripe.disputes("ch_fraud")).thenReturn(List.of());PartnerJob j=tx.execute(t->{PartnerJob p=new PartnerJob();p.eventId="evt_fraud_"+UUID.randomUUID();p.eventType="charge.refunded";p.objectId="ch_fraud";em.persist(p);return p;});processing.process(j.id);assertEquals(0L,service.overview(auth).get("pendingCents"));assertEquals(302L,service.overview(auth).get("reversedCents"));
 }
 @Autowired com.smartpruefung.backend.teacher.TeacherService teachers;
 @Test void partnerApplicationIncludesTeacherWorkspace(){
  assertEquals("ACTIVE",teachers.me(auth).get("status"));
  assertEquals(1L,tx.execute(t->em.createQuery("select count(p) from TeacherProfile p where p.userId=:u",Long.class).setParameter("u",owner.getUuid()).getSingleResult()).longValue());
 }
 @Test void existingPartnerGetsTeacherProfileAndSuspensionIsPreserved(){
  tx.execute(t->{em.createQuery("delete from TeacherProfile where userId=:u").setParameter("u",owner.getUuid()).executeUpdate();return null;});
  assertEquals("ACTIVE",teachers.me(auth).get("status"));
  tx.execute(t->{em.createQuery("update TeacherProfile set status='SUSPENDED' where userId=:u").setParameter("u",owner.getUuid()).executeUpdate();return null;});
  teachers.apply(auth);assertEquals("SUSPENDED",teachers.me(auth).get("status"));
 }
 @Test void unrelatedStudentDoesNotBecomeTeacher(){
  String studentAuth="Bearer "+JwtUtils.getJWTToken(buyer.getEmail(),"USER");
  assertEquals("NOT_REGISTERED",teachers.me(studentAuth).get("status"));
 }
 @Test void teacherCanUseTeacherModeWithoutApplyingAsPartner(){
  String teacherAuth="Bearer "+JwtUtils.getJWTToken(buyer.getEmail(),"USER");
  teachers.apply(teacherAuth);assertEquals("ACTIVE",teachers.me(teacherAuth).get("status"));
  assertEquals("NOT_REGISTERED",teachers.me(teacherAuth).get("partnerStatus"));
  assertNull(tx.execute(t->service.first(PartnerProfile.class,"userId",buyer.getUuid())));
  assertEquals("APPROVED",teachers.me(auth).get("partnerStatus"));
 }
 @Test void adminFinancialListsIncludePartnerNamesWithoutConfidentialPayoutData() throws Exception {
  credit(5000);service.requestPayout(auth,"request-admin-list");
  String viewer="Bearer "+staff("Viewer");
  for(String endpoint:List.of("partner-sales","partner-payouts")){
   String response=mvc.perform(get("/api/admin/v1/"+endpoint).param("partnerId",id).header("Authorization",viewer)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   JsonNode row=json.readTree(response).get("content").get(0);
   assertEquals("Partner Test",row.get("partnerName").asText());
   assertFalse(response.contains("destinationEncrypted"));assertFalse(response.contains("payoutEncrypted"));assertFalse(response.contains("payout@example.com"));
  }
 }
 @Test void manualReconciliationRetryRetainsTheAdministratorsReason() throws Exception {
  String job=tx.execute(t->{PartnerJob j=new PartnerJob();j.eventId="evt_audit_"+UUID.randomUUID();j.eventType="checkout.session.completed";j.objectId="cs_missing_"+UUID.randomUUID();j.eventAt=Instant.now();em.persist(j);return j.id;});
  String token="Bearer "+staff("Admin");String reason="Checked the Stripe event and requested another attempt";
  mvc.perform(post("/api/admin/v1/partner-reconciliation/"+job+"/retry").header("Authorization",token).contentType("application/json").content(json.writeValueAsBytes(Map.of("reason",reason)))).andExpect(status().isOk());
  String recorded=tx.execute(t->em.createQuery("select a.reason from PartnerAudit a where a.action='RECONCILIATION_RETRIED' order by a.createdAt desc",String.class).setMaxResults(1).getSingleResult());assertTrue(recorded.contains(reason));
 }
 @Test void analyticsUsesCompleteOwnedUtcHistoryAndSignedAdjustments() throws Exception {
  YearMonth current=YearMonth.now(ZoneOffset.UTC), oldest=current.minusMonths(5);
  Instant month=current.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), start=oldest.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  tx.execute(t->{
   for(int i=0;i<24;i++){
    PartnerSale sale=new PartnerSale();sale.partnerId=id;sale.checkoutSessionId="cs_analytics_"+UUID.randomUUID();sale.qualified=true;sale.paidAt=month;em.persist(sale);
    PartnerEntry e=new PartnerEntry();e.partnerId=id;e.saleId=sale.id;e.sourceKey="analytics_"+UUID.randomUUID();e.kind="EARNED";e.amountCents=100;e.createdAt=month;e.eligibleAt=month;e.status=i==0?"SETTLED":i==1?"RESERVED":"OPEN";em.persist(e);
   }
   long[] amounts={-900,150,300,-700,99999,99999,99999};
   String[] kinds={"REVERSAL","ADJUSTMENT","EARNED","REVERSAL","EARNED","EARNED","EARNED"};
   Instant[] dates={month,month,start,start,start.minusSeconds(1),current.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(),month};
   for(int i=0;i<amounts.length;i++){
    PartnerEntry e=new PartnerEntry();e.partnerId=i==6?"another-partner":id;e.saleId=UUID.randomUUID().toString();e.sourceKey="analytics_"+UUID.randomUUID();e.kind=kinds[i];e.amountCents=amounts[i];e.createdAt=dates[i];e.eligibleAt=dates[i];em.persist(e);
   }
   for(int i=0;i<3;i++){PartnerSale sale=new PartnerSale();sale.partnerId=i==0?"another-partner":id;sale.checkoutSessionId="cs_excluded_"+UUID.randomUUID();sale.qualified=i!=1;sale.paidAt=i==2?start.minusSeconds(1):month;em.persist(sale);}
   return null;
  });
  String body=mvc.perform(get("/api/partners/me/analytics").header("Authorization",auth)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();
  JsonNode result=json.readTree(body), months=result.get("months");assertEquals("UTC",result.get("timezone").asText());assertNotNull(result.get("asOf"));assertEquals(6,months.size());
  assertEquals(oldest.toString(),months.get(0).get("month").asText());assertEquals(-400,months.get(0).get("netCents").asLong());
  assertEquals(0,months.get(1).get("netCents").asLong());assertEquals(0,months.get(1).get("qualifiedPurchases").asLong());
  assertEquals(current.toString(),months.get(5).get("month").asText());assertEquals(2400,months.get(5).get("earnedCents").asLong());assertEquals(-750,months.get(5).get("adjustmentCents").asLong());assertEquals(1650,months.get(5).get("netCents").asLong());assertEquals(24,months.get(5).get("qualifiedPurchases").asLong());
  assertFalse(body.contains("checkoutSessionId"));assertFalse(body.contains("saleId"));assertFalse(body.contains(owner.getEmail()));
  mvc.perform(get("/api/partners/me/analytics")).andExpect(status().isForbidden());
  mvc.perform(get("/api/partners/me/analytics").header("Authorization","Bearer "+JwtUtils.getJWTToken(buyer.getEmail(),"USER"))).andExpect(status().isNotFound());
 }

 @Test void codeValidationIdentifiesEachConfigurationFailure() throws Exception {
  PromotionCode code=ApiResource.GSON.fromJson("{\"id\":\"promo_diagnostics\",\"active\":true,\"livemode\":false,\"code\":\"MEGA10\",\"coupon\":{\"id\":\"IWXERvxx\",\"valid\":true,\"duration\":\"once\",\"percent_off\":10,\"livemode\":false}}",PromotionCode.class);
  java.util.function.Consumer<String> fails=message->{org.springframework.web.server.ResponseStatusException e=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->processing.validate(code,1000));assertEquals(400,e.getStatus().value());assertTrue(e.getReason().contains(message),e.getReason());};
  // Unrestricted coupons are accepted when mode, percentage, duration and activity match.
  assertDoesNotThrow(()->processing.validate(code,1000));
  code.setLivemode(true);fails.accept("Stripe mode mismatch");code.setLivemode(false);
  code.setActive(false);fails.accept("inactive");code.setActive(true);
  Coupon coupon=code.getCoupon();coupon.setValid(false);fails.accept("no longer valid");coupon.setValid(true);
  coupon.setPercentOff(null);fails.accept("percentage-off");coupon.setPercentOff(new java.math.BigDecimal("20.0"));fails.accept("Coupon discount is 20%, but this partner's learner discount is 10%");coupon.setPercentOff(new java.math.BigDecimal("10.0"));
  coupon.setDuration("forever");fails.accept("duration must be once");coupon.setDuration("once");
  Coupon.AppliesTo scope=new Coupon.AppliesTo();scope.setProducts(List.of("prod_other"));coupon.setAppliesTo(scope);
  Price ten=new Price();ten.setProduct("prod_10");Price unlimited=new Price();unlimited.setProduct("prod_unlimited");when(stripe.price("price_10")).thenReturn(ten);when(stripe.price("price_unlimited")).thenReturn(unlimited);
  assertDoesNotThrow(()->processing.validate(code,1000));scope.setProducts(List.of("prod_10","prod_unlimited"));assertDoesNotThrow(()->processing.validate(code,1000));verify(stripe,never()).price(anyString());
  code.setCoupon((Coupon)null);fails.accept("no readable coupon");
 }

}
