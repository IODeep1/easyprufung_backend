package com.smartpruefung.backend.partner;

import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;

/** Durable inbox and independently retried accounting. Student access is fulfilled by the existing service first. */
@Service public class PartnerStripeService {
 @PersistenceContext EntityManager em;
 private final PartnerService partners;
 private final PartnerStripeGateway stripe;
 private final Environment env;
 private final TransactionTemplate tx;
 public PartnerStripeService(PartnerService partners,PartnerStripeGateway stripe,Environment env,PlatformTransactionManager manager){this.partners=partners;this.stripe=stripe;this.env=env;this.tx=new TransactionTemplate(manager);}
 boolean live(){return env.getProperty("smartpruefung.partner.live-mode",Boolean.class,true);}
 String price10(){return env.getRequiredProperty("smartpruefung.stripe.priceidb1");}
 String priceUnlimited(){return env.getRequiredProperty("smartpruefung.stripe.priceidb1unlimited");}
 public void enqueue(Event event){String type=event.getType();String object;
  if(type.equals("checkout.session.completed")||type.equals("checkout.session.async_payment_succeeded")){Session s=ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(),Session.class);if(type.equals("checkout.session.completed")&&!"paid".equals(s.getPaymentStatus()))return;object=s.getId();}
  else if(type.equals("charge.refunded")){object=ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(),Charge.class).getId();}
  else if(type.startsWith("charge.dispute.")){object=ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getRawJson(),Dispute.class).getCharge();}
  else return;
  if(Boolean.TRUE.equals(event.getLivemode())!=live())return;
  try{tx.execute(s->{if(partners.first(PartnerJob.class,"eventId",event.getId())==null){PartnerJob j=new PartnerJob();j.eventId=event.getId();j.eventType=type;j.objectId=object;j.liveMode=Boolean.TRUE.equals(event.getLivemode());j.eventAt=Instant.ofEpochSecond(event.getCreated());em.persist(j);}return null;});}catch(org.springframework.dao.DataIntegrityViolationException duplicate){if(!tx.execute(s->partners.first(PartnerJob.class,"eventId",event.getId())!=null))throw duplicate;}
 }
 public Object link(String id,long version,String promotionId,String reason) throws Exception {
  promotionId=PartnerService.text(promotionId,6,255,"Stripe promotion-code ID");String safeReason=PartnerService.text(reason,3,1000,"reason");PromotionCode remote=stripe.promotion(promotionId);
  return tx.execute(s->{PartnerProfile p=partners.lock(id);partners.version(p,version);if(!"APPROVED".equals(p.status))throw PartnerService.error(409,"Approve the partner before linking a code.");validate(remote,p.discountBps);PartnerCode existing=partners.first(PartnerCode.class,"promotionCodeId",remote.getId());if(existing!=null)throw PartnerService.error(409,"This Stripe code has already been linked. Historical ownership cannot be changed.");PartnerCode c=new PartnerCode();c.partnerId=id;c.promotionCodeId=remote.getId();c.couponId=remote.getCoupon().getId();c.displayCode=remote.getCode();c.liveMode=live();c.discountBps=p.discountBps;em.persist(c);partners.audit(id,partners.adminActor(),"STRIPE_CODE_LINKED",safeReason+"; "+remote.getId());em.flush();return partners.detail(id);});
 }
 void validate(PromotionCode code,int discountBps){
  if(code==null)throw PartnerService.error(400,"Stripe promotion code was not found. Enter its promo_ ID, not a coupon ID or display code.");
  if(!Boolean.TRUE.equals(code.getActive()))throw PartnerService.error(400,"This Stripe promotion code is inactive. Activate it or choose an active code.");
  if(Boolean.TRUE.equals(code.getLivemode())!=live())throw PartnerService.error(400,"Stripe mode mismatch: this promotion code is in "+(Boolean.TRUE.equals(code.getLivemode())?"live":"test")+" mode, but Partner Mode is configured for "+(live()?"live":"test")+" mode. Use a matching code. For isolated testing only, configure PARTNER_STRIPE_LIVE_MODE=false and matching test credentials and prices, then restart the backend.");
  Coupon coupon=code.getCoupon();
  if(coupon==null)throw PartnerService.error(400,"The Stripe promotion code has no readable coupon. Check the code's coupon in Stripe.");
  if(!Boolean.TRUE.equals(coupon.getValid()))throw PartnerService.error(400,"The Stripe coupon is no longer valid. Check its expiry and redemption limits or choose a valid coupon.");
  if(coupon.getPercentOff()==null)throw PartnerService.error(400,"Use a percentage-off coupon. Fixed-amount coupons are not supported for partner codes.");
  if(coupon.getPercentOff().multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(discountBps))!=0)throw PartnerService.error(400,"Coupon discount is "+coupon.getPercentOff().stripTrailingZeros().toPlainString()+"%, but this partner's learner discount is "+BigDecimal.valueOf(discountBps,2).stripTrailingZeros().toPlainString()+"%. Match the coupon to the learner discount, not the commission rate.");
  if(!"once".equals(coupon.getDuration()))throw PartnerService.error(400,"The Stripe coupon duration must be once. Create a once coupon and an active promotion code for it.");

 }

 public Object disable(String id,String codeId,long version,String reason) throws Exception {
  String validatedReason=PartnerService.text(reason,3,1000,"reason");return tx.execute(t->{partners.lock(id);PartnerCode c=em.find(PartnerCode.class,codeId);if(c==null||!id.equals(c.partnerId))throw PartnerService.error(404,"Code not found.");partners.version(c,version);try{stripe.deactivate(c.promotionCodeId);}catch(Exception e){throw PartnerService.error(503,"Unable to deactivate Stripe code. Please retry.");}c.active=false;c.validTo=Instant.now();c.retirementSource="ADMIN";partners.audit(id,partners.adminActor(),"CODE_DISABLED",validatedReason);em.flush();return partners.detail(id);});
 }
 public void drain(){List<String> ids=tx.execute(s->em.createQuery("select j.id from PartnerJob j where j.status in ('QUEUED','FAILED') and j.nextAttemptAt<=:now order by j.createdAt",String.class).setParameter("now",Instant.now()).setMaxResults(25).getResultList());for(String id:ids)process(id);}
 public void process(String id){try{tx.execute(s->{PartnerJob j=em.find(PartnerJob.class,id,LockModeType.PESSIMISTIC_WRITE);if(j==null||"PROCESSED".equals(j.status))return null;em.refresh(j);try{if(j.eventType.equals("partner.code.disable"))stripe.deactivate(j.objectId);else if(j.eventType.equals("partner.code.check"))reconcileCode(j.objectId);else if(j.eventType.startsWith("checkout.session."))attribute(j);else reconcileCharge(stripe.charge(j.objectId),j.eventId);j.status="PROCESSED";j.errorCode=null;j.attempts++;return null;}catch(Exception e){throw new ProcessingFailure(e);}});}catch(Exception e){tx.execute(s->{PartnerJob j=em.find(PartnerJob.class,id,LockModeType.PESSIMISTIC_WRITE);if(j!=null){j.status="FAILED";j.attempts++;j.errorCode="STRIPE_RECONCILIATION_REQUIRED";j.nextAttemptAt=Instant.now().plusSeconds(Math.min(86400,60L*(1L<<Math.min(j.attempts,10))));}return null;});}}
 static class ProcessingFailure extends RuntimeException{ProcessingFailure(Exception e){super(e);}}
 void attribute(PartnerJob job) throws Exception {
  Session s=stripe.session(job.objectId);
  if(!"paid".equals(s.getPaymentStatus())||!"payment".equals(s.getMode())||Boolean.TRUE.equals(s.getLivemode())!=live()||s.getMetadata()!=null&&("TEACHER_SEATS".equals(s.getMetadata().get("purchaseType"))||s.getMetadata().containsKey("teacher_seat_order_id")))return;
  PartnerSale previous=partners.first(PartnerSale.class,"checkoutSessionId",s.getId());if(previous!=null&&previous.qualified)return;
  if(s.getLineItems()==null||s.getLineItems().getData().size()!=1||Boolean.TRUE.equals(s.getLineItems().getHasMore()))return;
  LineItem line=s.getLineItems().getData().get(0);if(line.getPrice()==null||!List.of(price10(),priceUnlimited()).contains(line.getPrice().getId()))return;
  if(s.getTotalDetails()==null||s.getTotalDetails().getBreakdown()==null)throw new IllegalStateException("Tax and discount breakdown required");
  List<Session.TotalDetails.Breakdown.Discount> discounts=s.getTotalDetails().getBreakdown().getDiscounts();if(discounts==null||discounts.isEmpty())return;
  Set<String> mapped=new HashSet<>();PartnerCode code=null;
  for(Session.TotalDetails.Breakdown.Discount d:discounts){if(d.getDiscount()==null)throw new IllegalStateException("Discount not expanded");String promo=d.getDiscount().getPromotionCode();if(promo!=null){PartnerCode found=partners.first(PartnerCode.class,"promotionCodeId",promo);if(found!=null){mapped.add(found.partnerId);code=found;}}}
  if(mapped.isEmpty())return;
  PartnerProfile p=partners.lock(code.partnerId);
  // A disabled mapping is never reassigned. Purchases completed before its cutoff remain attributable.
  Instant paidAt=job.eventAt;
  if(code.liveMode!=live())return;
  boolean historical=code.validTo!=null&&!paidAt.isAfter(code.validTo);
  if((!code.active&&!historical)||paidAt.isBefore(code.validFrom)||(!"APPROVED".equals(p.status)&&!historical))return;
  PartnerSale sale=previous==null?new PartnerSale():previous;sale.partnerId=p.id;sale.checkoutSessionId=s.getId();sale.paymentIntentId=s.getPaymentIntent();sale.promotionCodeId=code.promotionCodeId;sale.priceId=line.getPrice().getId();sale.plan=price10().equals(sale.priceId)?"B1":"B1_UNLIMITED";sale.currency=s.getCurrency();sale.paidAt=paidAt;sale.eligibleAt=paidAt.plusSeconds(Math.max(0,env.getProperty("smartpruefung.partner.hold-days",Integer.class,14))*86400L);if(previous==null)sale.commissionBps=p.commissionBps;
  sale.grossCents=s.getAmountTotal()==null?0:s.getAmountTotal();sale.taxCents=s.getTotalDetails().getAmountTax()==null?0:s.getTotalDetails().getAmountTax();
  String review=null;
  if(discounts.size()!=1||mapped.size()!=1)review="Ambiguous or stacked discounts.";
  if(discounts.size()==1&&(discounts.get(0).getAmount()==null||discounts.get(0).getAmount()<=0))review="No verified positive partner discount.";
  if(!"eur".equalsIgnoreCase(sale.currency)||sale.grossCents<=0)return;
  if(s.getTotalDetails().getAmountTax()==null||sale.taxCents<0||sale.taxCents>sale.grossCents||s.getTotalDetails().getAmountShipping()==null||s.getTotalDetails().getAmountShipping()!=0)review="Tax or shipping breakdown requires review.";
  String purchaser=s.getCustomerDetails()!=null?s.getCustomerDetails().getEmail():s.getCustomerEmail();
  if(purchaser==null&&s.getCustomerObject()!=null)purchaser=s.getCustomerObject().getEmail();
  com.smartpruefung.backend.User.User buyer=purchaser==null?null:partners.actorByEmail(purchaser);
  com.smartpruefung.backend.User.User owner=partners.user(p.userId);
  boolean self=p.userId.equals(s.getClientReferenceId())||(buyer!=null&&p.userId.equals(buyer.getUuid()))||(purchaser!=null&&(purchaser.equalsIgnoreCase(p.contactEmail)||owner!=null&&purchaser.equalsIgnoreCase(owner.getEmail())))||(owner!=null&&owner.getSubscription()!=null&&s.getCustomer()!=null&&s.getCustomer().equals(owner.getSubscription().getCustomerId()));
  if(self)return;
  if(purchaser==null||buyer==null)review="Purchaser identity could not be verified.";
  if(buyer!=null)sale.purchaserId=buyer.getUuid();
  PromotionCode remote=stripe.promotion(code.promotionCodeId);
  if(Boolean.TRUE.equals(remote.getLivemode())!=live()||remote.getCoupon()==null||!code.couponId.equals(remote.getCoupon().getId()))review="Stripe code mapping changed.";
  if(!Boolean.TRUE.equals(remote.getActive())&&!(historical&&"ADMIN".equals(code.retirementSource)))review="Stripe code is inactive; verify redemption timing.";
  if(remote.getCoupon()!=null&&remote.getCoupon().getPercentOff()!=null&&remote.getCoupon().getPercentOff().multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(code.discountBps))!=0)review="Stripe discount changed.";
  sale.netCents=sale.grossCents-sale.taxCents;
  if(s.getPaymentIntentObject()!=null&&s.getPaymentIntentObject().getLatestCharge()!=null)sale.chargeId=s.getPaymentIntentObject().getLatestCharge();
  if(previous==null)em.persist(sale);
  if(review!=null){sale.reviewReason=review;return;}
  sale.qualified=true;sale.state="QUALIFIED";sale.reviewReason="";sale.commissionCents=PartnerMath.commission(sale.netCents,sale.commissionBps);entry(sale,sale.commissionCents,"EARNED","sale:"+sale.checkoutSessionId,sale.eligibleAt);
  if(sale.chargeId!=null)reconcileCharge(stripe.charge(sale.chargeId),job.eventId);
 }
 void entry(PartnerSale sale,long amount,String kind,String source,Instant eligible){if(amount==0)return;PartnerEntry e=new PartnerEntry();e.partnerId=sale.partnerId;e.saleId=sale.id;e.amountCents=amount;e.kind=kind;e.sourceKey=source;e.eligibleAt=eligible;em.persist(e);}
 void reconcileCharge(Charge charge,String source) throws Exception {
  if(Boolean.TRUE.equals(charge.getLivemode())!=live())return;
  PartnerSale sale=partners.first(PartnerSale.class,"paymentIntentId",charge.getPaymentIntent());
  if(sale==null){
   // Hydrate the paid session even when refund/dispute delivery preceded checkout delivery.
   for(Session session:stripe.sessionsForPayment(charge.getPaymentIntent())){PartnerJob hydration=new PartnerJob();hydration.eventId=source;hydration.objectId=session.getId();hydration.eventAt=Instant.ofEpochSecond(charge.getCreated());attribute(hydration);}
   sale=partners.first(PartnerSale.class,"paymentIntentId",charge.getPaymentIntent());if(sale==null)return;
  }
  partners.lock(sale.partnerId);em.refresh(sale);if(sale.commissionCents==0)return;
  sale.chargeId=charge.getId();boolean disputed=false;
  boolean fraudulent=charge.getFraudDetails()!=null&&("fraudulent".equals(charge.getFraudDetails().getStripeReport())||"fraudulent".equals(charge.getFraudDetails().getUserReport()));
  for(Dispute d:stripe.disputes(charge.getId()))if(!"won".equals(d.getStatus())&&!"warning_closed".equals(d.getStatus()))disputed=true;
  long refunded=charge.getAmountRefunded()==null?0:charge.getAmountRefunded();
  // A single item is refunded proportionally using its verified tax-exclusive/gross ratio.
  // Mixed tax components need an explicit review; reserve no money until reconciled.
  Session current=stripe.session(sale.checkoutSessionId);
  boolean mixed=current.getTotalDetails()==null||current.getTotalDetails().getBreakdown()==null||current.getTotalDetails().getBreakdown().getTaxes()==null||current.getTotalDetails().getBreakdown().getTaxes().size()>1;
  boolean verifiedRefund=sale.verifiedRefundGrossCents==refunded;
  boolean partialReview=refunded>0&&refunded<sale.grossCents&&mixed&&!verifiedRefund;
  long target=(fraudulent||disputed||partialReview)?sale.commissionCents:verifiedRefund?PartnerMath.refundTarget(sale.commissionCents,sale.verifiedRefundNetCents,sale.netCents):PartnerMath.refundTarget(sale.commissionCents,refunded,sale.grossCents);
  long delta=target-sale.reversedCents;
  if(delta!=0){partners.cancelReservations(sale.partnerId,"Refund/dispute changed the available balance.");boolean alreadyPaid=em.createQuery("select count(e) from PartnerEntry e where e.saleId=:s and e.status='SETTLED'",Long.class).setParameter("s",sale.id).getSingleResult()>0;
   entry(sale,-delta,delta>0?"REVERSAL":"ADJUSTMENT","reconcile:"+sale.id+":"+source,alreadyPaid?Instant.now():sale.eligibleAt);sale.reversedCents=target;}
  sale.refundedCents=refunded;sale.disputed=disputed;sale.state=fraudulent?"REQUIRES_REVIEW":partialReview?"REQUIRES_REVIEW":disputed?"DISPUTED":refunded>=sale.grossCents?"REFUNDED":refunded>0?"PARTIALLY_REFUNDED":"QUALIFIED";sale.reviewReason=fraudulent?"Stripe flagged this charge as fraudulent. No commission is payable.":partialReview?"Partial refund with mixed taxes. Verify tax-exclusive refunded amount before releasing any commission.":"";
 }
 public Object retry(String id){return retry(id,"Manual reconciliation retry");}
 public Object retry(String id,String reason){String retryReason=PartnerService.text(reason,3,1000,"retry reason");tx.execute(s->{PartnerJob j=em.find(PartnerJob.class,id,LockModeType.PESSIMISTIC_WRITE);if(j==null)throw PartnerService.error(404,"Reconciliation item not found.");if(!"PROCESSED".equals(j.status)){j.status="QUEUED";j.nextAttemptAt=Instant.now();}partners.audit(null,partners.adminActor(),"RECONCILIATION_RETRIED",retryReason+"; job="+id);return null;});process(id);return Map.of("queued",true);}
 public Object retrySale(String id,String reason){tx.execute(t->{PartnerSale sale=em.find(PartnerSale.class,id);if(sale==null)throw PartnerService.error(404,"Sale not found.");partners.audit(sale.partnerId,partners.adminActor(),"SALE_RECHECKED",PartnerService.text(reason,3,1000,"reason"));String event="sale-recheck:"+UUID.randomUUID();PartnerJob j=new PartnerJob();j.eventId=event;j.eventType=sale.commissionCents>0&&sale.chargeId!=null?"charge.refunded":"checkout.session.completed";j.objectId=sale.commissionCents>0&&sale.chargeId!=null?sale.chargeId:sale.checkoutSessionId;j.eventAt=sale.paidAt;j.liveMode=live();em.persist(j);return j.id;});drain();return Map.of("queued",true);}
 public Object verifyRefund(String id,long version,long netRefund,String reason) throws Exception {PartnerSale snapshot=tx.execute(t->em.find(PartnerSale.class,id));if(snapshot==null||snapshot.chargeId==null)throw PartnerService.error(404,"Refunded sale not found.");Charge charge=stripe.charge(snapshot.chargeId);return tx.execute(t->{partners.lock(snapshot.partnerId);PartnerSale sale=em.find(PartnerSale.class,id);em.refresh(sale);partners.version(sale,version);if(!"REQUIRES_REVIEW".equals(sale.state)||sale.commissionCents<=0||charge.getAmountRefunded()==null||charge.getAmountRefunded()<=0||netRefund<0||netRefund>sale.netCents||netRefund>charge.getAmountRefunded()||charge.getAmountRefunded()-netRefund>sale.taxCents)throw PartnerService.error(400,"Verify the tax-exclusive refunded amount against Stripe first.");sale.verifiedRefundGrossCents=charge.getAmountRefunded();sale.verifiedRefundNetCents=netRefund;String safeReason=PartnerService.text(reason,3,1000,"verified refund evidence");partners.audit(sale.partnerId,partners.adminActor(),"REFUND_NET_VERIFIED",safeReason+"; net refund cents="+netRefund);try{reconcileCharge(charge,"manual-refund:"+UUID.randomUUID());}catch(Exception e){throw new ProcessingFailure(e);}em.flush();return sale;});}
 public void reconcileCodes() {
  String after="";
  while(true){final String cursor=after;List<String> ids=tx.execute(t->em.createQuery("select c.id from PartnerCode c where c.active=true and c.id>:cursor order by c.id",String.class).setParameter("cursor",cursor).setMaxResults(100).getResultList());if(ids.isEmpty())break;
   for(String id:ids){try{reconcileCode(id);}catch(Exception failure){tx.execute(t->{String key="code-check:"+id+":"+java.time.LocalDate.now(java.time.ZoneOffset.UTC);if(partners.first(PartnerJob.class,"eventId",key)==null){PartnerJob j=new PartnerJob();j.eventId=key;j.eventType="partner.code.check";j.objectId=id;em.persist(j);}return null;});}}
   after=ids.get(ids.size()-1);
  }
 }
 void reconcileCode(String id) throws Exception {
  PartnerCode snapshot=tx.execute(t->em.find(PartnerCode.class,id));if(snapshot==null||!snapshot.active)return;
  PartnerProfile profile=tx.execute(t->em.find(PartnerProfile.class,snapshot.partnerId));PromotionCode remote=stripe.promotion(snapshot.promotionCodeId);
  boolean valid=profile!=null&&"APPROVED".equals(profile.status)&&Boolean.TRUE.equals(remote.getActive())&&Boolean.TRUE.equals(remote.getLivemode())==snapshot.liveMode&&remote.getCoupon()!=null&&snapshot.couponId.equals(remote.getCoupon().getId())&&remote.getCoupon().getPercentOff()!=null&&remote.getCoupon().getPercentOff().multiply(BigDecimal.valueOf(100)).compareTo(BigDecimal.valueOf(snapshot.discountBps))==0&&Boolean.TRUE.equals(remote.getCoupon().getValid());
  if(valid)return;
  if(Boolean.TRUE.equals(remote.getActive()))stripe.deactivate(snapshot.promotionCodeId);
  tx.execute(t->{partners.lock(snapshot.partnerId);PartnerCode code=em.find(PartnerCode.class,id);if(code.active){code.active=false;code.validTo=Instant.now();code.retirementSource="STRIPE";partners.audit(code.partnerId,"system","STRIPE_CODE_RETIRED","Stripe code was inactive, changed, or its partner was no longer approved.");}return null;});
 }
 public void suspendCodes(String id){List<PartnerCode> codes=tx.execute(s->em.createQuery("from PartnerCode where partnerId=:p",PartnerCode.class).setParameter("p",id).getResultList());for(PartnerCode c:codes)try{stripe.deactivate(c.promotionCodeId);}catch(Exception e){tx.execute(s->{String key="disable:"+c.id+":"+c.version; if(partners.first(PartnerJob.class,"eventId",key)==null){PartnerJob j=new PartnerJob();j.eventId=key;j.eventType="partner.code.disable";j.objectId=c.promotionCodeId;em.persist(j);}return null;});}}
}
