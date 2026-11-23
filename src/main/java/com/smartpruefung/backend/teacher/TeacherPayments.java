package com.smartpruefung.backend.teacher;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Subscription;
import com.smartpruefung.backend.User.Service.UserService;
import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.*;
import java.util.*;

/** Checkout redirects never grant seats. This service only fulfills verified server-retrieved Stripe objects. */
@Service
public class TeacherPayments {
 @PersistenceContext EntityManager em;
 private final TeacherService teachers;
 private final UserService users;
 private final com.smartpruefung.backend.User.Service.SubscriptionService subscriptions;
 private final com.smartpruefung.backend.exam.guest.ExamAccessService access;
 private final com.smartpruefung.backend.User.Repository.UsersRepository usersRepository;
 private final org.springframework.core.env.Environment environment;
 private final TransactionTemplate tx;
 public TeacherPayments(TeacherService teachers,UserService users,PlatformTransactionManager manager,
  com.smartpruefung.backend.User.Service.SubscriptionService subscriptions,com.smartpruefung.backend.exam.guest.ExamAccessService access,
  com.smartpruefung.backend.User.Repository.UsersRepository usersRepository,org.springframework.core.env.Environment environment){
  this.teachers=teachers;this.users=users;this.tx=new TransactionTemplate(manager);this.subscriptions=subscriptions;this.access=access;this.usersRepository=usersRepository;this.environment=environment;
 }
 public Map<String,Object> checkout(String auth,int quantity) throws Exception {
  User user=tx.execute(status->teachers.teacher(auth));
  if(!environment.getProperty("smartpruefung.teacher.checkout-enabled",Boolean.class,false))throw TeacherService.error(409,"Seat checkout is not enabled yet. Class management is free.");
  int unit=SeatPricing.unit(quantity);
  RequestOptions requestOptions=RequestOptions.builder().setApiKey(environment.getRequiredProperty("smartpruefung.stripe.apisecret")).build();
  Price price=teacherPrice(quantity,requestOptions);
  TeacherSeatOrder order=tx.execute(status->{
   // Resolve the wallet owner's previous Stripe customer; do not trust browser-supplied customer IDs.
   User current=teachers.teacher(auth);
   String customer=em.createQuery("select o.stripeCustomerId from TeacherSeatOrder o where o.teacherId=:u and o.stripeCustomerId is not null and o.status='PAID' order by o.createdAt desc",String.class)
    .setParameter("u",current.getUuid()).setMaxResults(1).getResultStream().findFirst().orElse(null);
   if(customer==null && current.getSubscription()!=null)customer=current.getSubscription().getCustomerId();
   if(customer!=null && !customer.startsWith("cus_"))customer=null;
   TeacherSeatOrder o=new TeacherSeatOrder();o.teacherId=current.getUuid();o.quantity=quantity;o.unitPriceCents=unit;o.totalCents=Math.multiplyExact(unit,quantity);o.currency="eur";o.status="PENDING";
   o.stripePriceId=price.getId();o.pricingTier=SeatPricing.tier(quantity);o.stripeCustomerId=customer;em.persist(o);return o;
  });
  // The order and selected tier are committed before opening hosted Checkout.
  String base=environment.getProperty("smartpruefung.teacher.app-url","https://app.smartpruefung.com").replaceAll("/$","");
  SessionCreateParams.Builder builder=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
   .setClientReferenceId("teacher-seat:"+order.id)
   .putMetadata("purchaseType","TEACHER_SEATS").putMetadata("teacher_seat_order_id",order.id).putMetadata("teacherId",order.teacherId)
   .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder().putMetadata("purchaseType","TEACHER_SEATS").putMetadata("teacher_seat_order_id",order.id).build())
   .setSuccessUrl(base+"/teacher/billing?checkout=returned&order="+order.id+"&session_id={CHECKOUT_SESSION_ID}")
   .setCancelUrl(base+"/teacher/seats?checkout=canceled&order="+order.id)
   // Omitting adjustable_quantity keeps quantity fixed. A change must recalculate the tier on the backend.
   .addLineItem(SessionCreateParams.LineItem.builder().setPrice(order.stripePriceId).setQuantity((long)quantity).build());
  if(order.stripeCustomerId!=null)builder.setCustomer(order.stripeCustomerId);
  else builder.setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS).setCustomerEmail(user.getEmail());
  Session session;
  try{session=Session.create(builder.build(),RequestOptions.builder().setApiKey(requestOptions.getApiKey()).setIdempotencyKey("teacher-seat:"+order.id).build());}
  catch(Exception failure){
   tx.execute(status->{TeacherSeatOrder o=em.find(TeacherSeatOrder.class,order.id,LockModeType.PESSIMISTIC_WRITE);em.refresh(o);if("PENDING".equals(o.status)&&o.stripeSessionId==null)o.status="FAILED";return null;});
   throw failure;
  }
  tx.execute(status->{TeacherSeatOrder o=em.find(TeacherSeatOrder.class,order.id,LockModeType.PESSIMISTIC_WRITE);if(o.stripeSessionId!=null && !session.getId().equals(o.stripeSessionId))throw new IllegalStateException("Session mismatch");o.stripeSessionId=session.getId();return null;});
  return Map.of("url",session.getUrl(),"orderId",order.id);
 }
 private Price teacherPrice(int quantity,RequestOptions options) throws Exception {
  String product=null;Price selected=null;
  for(Map<String,Object> tier:SeatPricing.schedule()){
   String code=(String)tier.get("code");String id=environment.getProperty("smartpruefung.stripe.teacher.price."+code,"").trim();
   if(!id.startsWith("price_"))throw TeacherService.error(409,"Teacher checkout is not configured. Set all four teacher Stripe Price IDs.");
   Price price=Price.retrieve(id,options);
   if(!Boolean.TRUE.equals(price.getActive()) || !"one_time".equals(price.getType()) || price.getRecurring()!=null || price.getTransformQuantity()!=null || price.getCustomUnitAmount()!=null || !"per_unit".equals(price.getBillingScheme()) || !"eur".equals(price.getCurrency()) || price.getUnitAmount()==null || price.getUnitAmount().longValue()!=((Integer)tier.get("unitPriceCents")).longValue())
    throw TeacherService.error(409,"A teacher Stripe Price does not match the configured one-time EUR seat pricing.");
   if(price.getProduct()==null || product!=null && !product.equals(price.getProduct()))throw TeacherService.error(409,"All teacher seat Prices must belong to the same Stripe product.");
   product=price.getProduct();if(code.equals(SeatPricing.tier(quantity)))selected=price;
  }
  return selected;
 }
 public boolean isTeacher(Session session){return session.getMetadata()!=null && "TEACHER_SEATS".equals(session.getMetadata().get("purchaseType")) || session.getClientReferenceId()!=null && session.getClientReferenceId().startsWith("teacher-seat:") || session.getMetadata()!=null && session.getMetadata().containsKey("teacher_seat_order_id");}
 public void fulfill(Event event,Session session) {
  tx.execute(status->{
   String id=session.getMetadata()==null?null:session.getMetadata().get("teacher_seat_order_id");
   if(id==null)throw new IllegalStateException("Missing teacher seat order metadata");
   TeacherSeatOrder o=em.find(TeacherSeatOrder.class,id,LockModeType.PESSIMISTIC_WRITE);
   if(o==null)throw new IllegalStateException("Teacher seat order not found");em.refresh(o);
   if(seen(event.getId()))return null;
   if(!("teacher-seat:"+o.id).equals(session.getClientReferenceId()) || (o.stripeSessionId!=null && !o.stripeSessionId.equals(session.getId())))throw new IllegalStateException("Teacher order/session mismatch");
   if(!"payment".equals(session.getMode()) || !"paid".equals(session.getPaymentStatus()))return null;
   if(o.stripePriceId!=null && (session.getMetadata()==null || !"TEACHER_SEATS".equals(session.getMetadata().get("purchaseType")) || !o.teacherId.equals(session.getMetadata().get("teacherId"))))throw new IllegalStateException("Teacher purchase metadata mismatch");
   if(o.stripeCustomerId!=null && !o.stripeCustomerId.equals(session.getCustomer()))throw new IllegalStateException("Teacher customer mismatch");
   if(!o.currency.equalsIgnoreCase(session.getCurrency()) || session.getAmountTotal()==null || session.getAmountTotal()!=o.totalCents)throw new IllegalStateException("Teacher payment amount mismatch");
   if(session.getLineItems()==null || session.getLineItems().getData().size()!=1)throw new IllegalStateException("Unexpected teacher line items");
   LineItem line=session.getLineItems().getData().get(0);
   if(line.getQuantity()==null || line.getQuantity()!=o.quantity || line.getPrice()==null || line.getPrice().getUnitAmount()==null || line.getPrice().getUnitAmount()!=o.unitPriceCents)throw new IllegalStateException("Teacher quantity/price mismatch");
   if(o.stripePriceId!=null && !o.stripePriceId.equals(line.getPrice().getId()))throw new IllegalStateException("Teacher Stripe Price mismatch");
   o.stripeSessionId=session.getId();o.paymentIntentId=session.getPaymentIntent();o.stripeCustomerId=session.getCustomer();
   // Inspect the charge as well: a refund/dispute may arrive before the checkout notification.
   Charge charge=charge(session.getPaymentIntent());
   if(charge!=null){o.receiptUrl=charge.getReceiptUrl();if(Boolean.TRUE.equals(charge.getRefunded()) || Boolean.TRUE.equals(charge.getDisputed()) || charge.getAmountRefunded()!=null && charge.getAmountRefunded()>0){reverse(o,"SUPPORT_REVIEW");record(event,session.getId());return null;}}
   if("PENDING".equals(o.status) || "FAILED".equals(o.status)){
    o.status="PAID";
    for(int i=0;i<o.quantity;i++){TeacherSeat seat=new TeacherSeat();seat.orderId=o.id;seat.seatNumber=i;seat.teacherId=o.teacherId;seat.state="AVAILABLE";em.persist(seat);}
    teachers.audit(o.teacherId,"SEAT_ORDER_PAID",o.id);
   }
   record(event,session.getId());return null;
  });
 }
 public void failedCheckout(Event event,Session session,String state) {
  if(!isTeacher(session))return;
  tx.execute(status->{String id=session.getMetadata()==null?null:session.getMetadata().get("teacher_seat_order_id");
   if(id==null)throw new IllegalStateException("Missing order metadata");
   TeacherSeatOrder o=em.find(TeacherSeatOrder.class,id,LockModeType.PESSIMISTIC_WRITE);
   if(o==null)throw new IllegalStateException("Missing teacher order");em.refresh(o);
   if(seen(event.getId()))return null;
   if(!("teacher-seat:"+o.id).equals(session.getClientReferenceId()) || o.stripeSessionId!=null&&!o.stripeSessionId.equals(session.getId()))throw new IllegalStateException("Order/session mismatch");
   if("PENDING".equals(o.status))o.status=state;record(event,session.getId());return null;
  });
 }
 public void reverseEvent(Event event,String paymentIntent,String state) {
  if(paymentIntent==null)return;
  tx.execute(status->{
   List<TeacherSeatOrder> found=em.createQuery("from TeacherSeatOrder where paymentIntentId=:p",TeacherSeatOrder.class).setParameter("p",paymentIntent).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
   if(found.isEmpty())return null;
   if(seen(event.getId()))return null;
   for(TeacherSeatOrder o:found){em.refresh(o);reverse(o,state);}record(event,paymentIntent);return null;
  });
 }
 private void reverse(TeacherSeatOrder o,String state) {
  o.status=state;
  for(TeacherSeat s:em.createQuery("from TeacherSeat where orderId=:o order by id",TeacherSeat.class).setParameter("o",o.id).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList()){
   em.refresh(s);
   if("ACTIVATED".equals(s.state)){o.supportReview=true;continue;}
   s.state="REVERSED";
   for(SeatOffer offer:em.createQuery("from SeatOffer where seatId=:s and state='PENDING'",SeatOffer.class).setParameter("s",s.id).getResultList()){offer.state="REVOKED";offer.resolvedAt=java.time.Instant.now();}
  }
  teachers.audit(o.teacherId,"SEAT_ORDER_"+state,o.id);
 }
 /** Serialize personal fulfillment with teacher-seat acceptance using the existing user row lock. */
 public void fulfillStudent(Event event,Session session,String priceId) {
  tx.execute(status->{
   User u=access.lockUser(session.getClientReferenceId());
   if(seen(event.getId()) || !em.createQuery("from PaymentWebhookEvent where objectId=:o and eventType='STUDENT_PAID'",PaymentWebhookEvent.class).setParameter("o",session.getId()).getResultList().isEmpty())return null;
   String email=u.getEmail();Subscription s=u.getSubscription();
   if(s==null){s=subscriptions.createB1PaidSubscriptionByPriceId(session.getCustomer(),email,priceId);u.setSubscription(s);usersRepository.save(u);}
   else subscriptions.activateB1PaidSubscriptionByPriceId(s,session.getCustomer(),email,priceId);
   PaymentWebhookEvent receipt=new PaymentWebhookEvent();receipt.eventId=event.getId();receipt.objectId=session.getId();receipt.eventType="STUDENT_PAID";em.persist(receipt);
   // Email is asynchronous in the existing service.
   users.sendNewSubscriptionEmail(u);return null;
  });
 }
 private Charge charge(String intent) {
  if(intent==null)return null;
  try{PaymentIntent pi=PaymentIntent.retrieve(intent);return pi.getLatestCharge()==null?null:Charge.retrieve(pi.getLatestCharge());}
  catch(Exception ex){throw new IllegalStateException("Could not verify paid charge",ex);}
 }
 boolean seen(String event){return teachers.first(PaymentWebhookEvent.class,"eventId",event)!=null;}
 void record(Event event,String object){PaymentWebhookEvent e=new PaymentWebhookEvent();e.eventId=event.getId();e.eventType=event.getType();e.objectId=object;em.persist(e);}
}
