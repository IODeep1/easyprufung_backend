package com.smartpruefung.backend.partner;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.*;

/** The only partner component that calls Stripe. Use explicit credentials instead of Stripe.apiKey. */
@Component public class PartnerStripeGateway {
 private final Environment env;
 public PartnerStripeGateway(Environment env){this.env=env;}
 RequestOptions options(){return RequestOptions.builder().setApiKey(env.getRequiredProperty("smartpruefung.stripe.apisecret")).build();}
 public Session session(String id) throws Exception{return Session.retrieve(id,Map.of("expand",List.of("line_items","total_details.breakdown","payment_intent.latest_charge")),options());}
 public PromotionCode promotion(String id) throws Exception{return PromotionCode.retrieve(id,Map.of("expand",List.of("coupon.applies_to")),options());}
 public Price price(String id) throws Exception{return Price.retrieve(id,options());}
 public void deactivate(String id) throws Exception{promotion(id).update(Map.of("active",false),options());}
 public List<Session> sessionsForPayment(String id) throws Exception {return Session.list(Map.of("payment_intent",id,"limit",100),options()).getData();}
 public Charge charge(String id) throws Exception{return Charge.retrieve(id,options());}
 public List<Dispute> disputes(String charge) throws Exception{List<Dispute> all=new ArrayList<>();for(Dispute d:Dispute.list(Map.of("charge",charge,"limit",100),options()).autoPagingIterable())all.add(d);return all;}
}
