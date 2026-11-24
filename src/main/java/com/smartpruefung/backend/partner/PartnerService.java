package com.smartpruefung.backend.partner;

import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.persistence.*;
import java.time.*;
import java.util.*;
import java.math.BigInteger;

@Service @Transactional
public class PartnerService {
 @PersistenceContext EntityManager em;
 private final UsersRepository users;
 private final ObjectMapper json;
 private final PartnerCrypto crypto;
 private final com.smartpruefung.backend.teacher.TeacherService teachers;
 private final org.springframework.core.env.Environment environment;
 private final Map<String,long[]> applicationWindows=new LinkedHashMap<>();
 private synchronized void applicationRate(String id){long minute=System.currentTimeMillis()/60000;applicationWindows.entrySet().removeIf(e->e.getValue()[0]!=minute);if(applicationWindows.size()>10000)throw error(429,"Please retry in a minute.");long[] w=applicationWindows.computeIfAbsent(id,k->new long[]{minute,0});if(++w[1]>10)throw error(429,"Too many application attempts. Please retry in a minute.");}
 public static final String TERMS="2026-11-24";
 public PartnerService(UsersRepository users,ObjectMapper json,PartnerCrypto crypto,org.springframework.core.env.Environment environment,com.smartpruefung.backend.teacher.TeacherService teachers){this.teachers=teachers;this.users=users;this.json=json;this.crypto=crypto;this.environment=environment;}
 public static ResponseStatusException error(int status,String message){return new ResponseStatusException(HttpStatus.valueOf(status),message);}
 public static class Social {public String platform,profile;public Long audienceSize;}
 public static class Input {
  public String displayName,contactEmail,country,website,approach;
  public List<Social> socials;
  public PayoutInput payout;
  public boolean termsAccepted,privacyAccepted;
  public String termsVersion;
 }
 public static class PayoutInput {public String method,holder,iban,bic,paypalEmail;}
 com.smartpruefung.backend.User.User actorByEmail(String email){return users.findByEmail(email.toLowerCase(Locale.ROOT));}
 com.smartpruefung.backend.User.User user(String id){return users.findByUUID(id);}
 User actor(String auth){if(auth==null||!JwtUtils.validateToken(auth))throw error(401,"Sign in to continue.");User u=users.findByEmail(JwtUtils.getHttpClientConsumer(auth).email);if(u==null)throw error(401,"Sign in to continue.");return u;}
 <T> T first(Class<T> type,String field,Object value){List<T> rows=em.createQuery("from "+type.getSimpleName()+" where "+field+"=:v",type).setParameter("v",value).setMaxResults(1).getResultList();return rows.isEmpty()?null:rows.get(0);}
 PartnerProfile own(String auth){PartnerProfile p=first(PartnerProfile.class,"userId",actor(auth).getUuid());if(p==null)throw error(404,"Apply to join the partner program first.");return p;}
 PartnerProfile lock(String id){PartnerProfile p=em.find(PartnerProfile.class,id,LockModeType.PESSIMISTIC_WRITE);if(p==null)throw error(404,"Partner not found.");em.refresh(p);return p;}
 void version(PartnerRecord p,long version){if(p.version!=version)throw error(409,"This record changed. Reload before saving.");}
 static String text(String s,int min,int max,String label){s=s==null?"":s.trim();if(s.length()<min||s.length()>max)throw error(400,"Check "+label+".");return s;}
 static String email(String s){s=text(s,3,254,"email").toLowerCase(Locale.ROOT);if(!s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw error(400,"Enter a valid email address.");return s;}
 static String url(String s){s=text(s,0,500,"website");if(!s.isEmpty()){try{java.net.URI u=java.net.URI.create(s);if(!Set.of("https","http").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}catch(Exception e){throw error(400,"Use a public http or https website URL.");}}return s;}
 void profile(PartnerProfile p,Input b){p.displayName=text(b.displayName,2,160,"display name");p.contactEmail=email(b.contactEmail);p.country=text(b.country,2,2,"two-letter country code").toUpperCase(Locale.ROOT);if(!Arrays.asList(Locale.getISOCountries()).contains(p.country))throw error(400,"Choose a valid country.");p.website=url(b.website);p.approach=text(b.approach,20,2000,"marketing approach (20–2000 characters)");if(b.socials==null||b.socials.isEmpty()||b.socials.size()>10)throw error(400,"Add between one and ten social profiles.");for(Social s:b.socials){s.platform=text(s.platform,2,60,"social platform");s.profile=text(s.profile,2,500,"social profile");if(s.audienceSize!=null&&(s.audienceSize<0||s.audienceSize>1000000000L))throw error(400,"Check audience size.");}try{p.socialsJson=json.writeValueAsString(b.socials);}catch(Exception e){throw error(400,"Invalid social profiles.");}}
 public Object apply(String auth,Input b){User u=actor(auth);applicationRate(u.getUuid());em.lock(em.find(User.class,u.getId()),LockModeType.PESSIMISTIC_WRITE);if(first(PartnerProfile.class,"userId",u.getUuid())!=null)throw error(409,"You already have a partner application.");if(!b.termsAccepted||!b.privacyAccepted||!TERMS.equals(b.termsVersion))throw error(400,"Accept the current partner terms and privacy notice.");PartnerProfile p=new PartnerProfile();p.userId=u.getUuid();profile(p,b);setPayout(p,b.payout);p.termsVersion=TERMS;p.termsAcceptedAt=Instant.now();em.persist(p);teachers.apply(auth);audit(p.id,u.getUuid(),"APPLICATION_SUBMITTED","Terms "+TERMS);return view(p);}
 public Object me(String auth){PartnerProfile p=first(PartnerProfile.class,"userId",actor(auth).getUuid());return p==null?Map.of("status","NOT_REGISTERED","termsVersion",TERMS):view(p);}
 public Object update(String auth,Input b){PartnerProfile p=lock(own(auth).id);profile(p,b);audit(p.id,p.userId,"PROFILE_UPDATED","");return view(p);}
 public Object payoutMethod(String auth,PayoutInput b){PartnerProfile p=lock(own(auth).id);cancelReservations(p.id,"Payout details changed; destination must be verified again.");setPayout(p,b);audit(p.id,p.userId,"PAYOUT_METHOD_UPDATED","");return view(p);}
 void setPayout(PartnerProfile p,PayoutInput b){if(b==null)throw error(400,"Choose a payout method.");b.holder=text(b.holder,2,160,"account holder");if("BANK".equals(b.method)){b.iban=text(b.iban==null?null:b.iban.replace(" ",""),15,34,"IBAN").toUpperCase(Locale.ROOT);if(!b.iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}"))throw error(400,"Enter a valid IBAN.");String rotated=b.iban.substring(4)+b.iban.substring(0,4);StringBuilder digits=new StringBuilder();for(char c:rotated.toCharArray())digits.append(Character.isDigit(c)?String.valueOf(c):String.valueOf(c-'A'+10));if(!new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE))throw error(400,"Enter a valid IBAN.");b.bic=text(b.bic,0,11,"BIC").toUpperCase(Locale.ROOT);if(!b.bic.isEmpty()&&!b.bic.matches("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?"))throw error(400,"Enter a valid BIC.");b.paypalEmail=null;p.payoutMasked=b.iban.substring(0,2)+" •••• "+b.iban.substring(b.iban.length()-4);}else if("PAYPAL".equals(b.method)){b.paypalEmail=email(b.paypalEmail);b.iban=null;b.bic=null;p.payoutMasked=b.paypalEmail.charAt(0)+"•••@"+b.paypalEmail.substring(b.paypalEmail.indexOf('@')+1);}else throw error(400,"Choose BANK or PAYPAL.");try{p.payoutEncrypted=crypto.encrypt(json.writeValueAsString(b));}catch(IllegalStateException e){throw error(503,"Partner applications are temporarily unavailable. Please try again later.");}catch(Exception e){throw error(400,"Check payout details.");}p.payoutMethod=b.method;p.payoutVerified=false;}
 Map<String,Object> view(PartnerProfile p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.id);m.put("version",p.version);m.put("status",p.status);m.put("displayName",p.displayName);m.put("contactEmail",p.contactEmail);m.put("country",p.country);m.put("website",p.website);m.put("approach",p.approach);try{m.put("socials",json.readTree(p.socialsJson));}catch(Exception e){m.put("socials",List.of());}m.put("commissionBps",p.commissionBps);m.put("discountBps",p.discountBps);m.put("reviewReason",p.reviewReason);m.put("payoutMethod",p.payoutMethod);m.put("payoutMasked",p.payoutMasked);m.put("payoutVerified",p.payoutVerified);m.put("termsVersion",p.termsVersion);m.put("createdAt",p.createdAt);return m;}
 public Object codes(String auth){PartnerProfile p=own(auth);if(!"APPROVED".equals(p.status))return List.of();return em.createQuery("from PartnerCode where partnerId=:p and active=true",PartnerCode.class).setParameter("p",p.id).getResultList();}
 public Map<String,Object> overview(String auth){return balances(own(auth).id);}
 /** Calendar-month reporting in UTC; independent of history pagination and payout settlement. */
 public Map<String,Object> analytics(String auth){
  String id=own(auth).id;
  YearMonth current=YearMonth.now(ZoneOffset.UTC), first=current.minusMonths(5);
  Instant start=first.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(), end=Instant.now();
  Map<String,Map<String,Object>> buckets=new LinkedHashMap<>();
  for(int i=0;i<6;i++){
   Map<String,Object> row=new LinkedHashMap<>();row.put("month",first.plusMonths(i).toString());
   row.put("earnedCents",0L);row.put("adjustmentCents",0L);row.put("netCents",0L);row.put("qualifiedPurchases",0L);
   buckets.put(first.plusMonths(i).toString(),row);
  }
  for(Object[] record:em.createQuery("select e.createdAt,e.kind,e.amountCents from PartnerEntry e where e.partnerId=:p and e.createdAt>=:start and e.createdAt<=:end",Object[].class).setParameter("p",id).setParameter("start",start).setParameter("end",end).getResultList()){
   Map<String,Object> row=buckets.get(YearMonth.from(((Instant)record[0]).atZone(ZoneOffset.UTC)).toString());
   String key="EARNED".equals(record[1])?"earnedCents":"adjustmentCents";long amount=((Number)record[2]).longValue();
   row.put(key,(Long)row.get(key)+amount);row.put("netCents",(Long)row.get("netCents")+amount);
  }
  for(Object[] record:em.createQuery("select s.paidAt,count(s) from PartnerSale s where s.partnerId=:p and s.qualified=true and s.paidAt>=:start and s.paidAt<=:end group by s.paidAt",Object[].class).setParameter("p",id).setParameter("start",start).setParameter("end",end).getResultList()){
   Map<String,Object> row=buckets.get(YearMonth.from(((Instant)record[0]).atZone(ZoneOffset.UTC)).toString());
   row.put("qualifiedPurchases",(Long)row.get("qualifiedPurchases")+((Number)record[1]).longValue());
  }
  return Map.of("months",new ArrayList<>(buckets.values()),"timezone","UTC","asOf",end);
 }

 Map<String,Object> balances(String id) {
  long pending=0,available=0,reserved=0,paid=0,reversed=0,earned=0;
  for(PartnerEntry e:em.createQuery("from PartnerEntry where partnerId=:p",PartnerEntry.class).setParameter("p",id).getResultList()) {
   if("EARNED".equals(e.kind))earned+=e.amountCents;
   else reversed-=e.amountCents;
   if("SETTLED".equals(e.status))paid+=e.amountCents;
   else if(e.payoutId!=null)reserved+=e.amountCents;
   else if(e.eligibleAt.isAfter(Instant.now()))pending+=e.amountCents;
   else available+=e.amountCents;
  }
  long lifetime=em.createQuery("select count(s) from PartnerSale s where s.partnerId=:p and s.qualified=true",Long.class).setParameter("p",id).getSingleResult();
  Instant month=java.time.LocalDate.now(java.time.ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
  long monthly=em.createQuery("select count(s) from PartnerSale s where s.partnerId=:p and s.qualified=true and s.paidAt>=:month",Long.class).setParameter("p",id).setParameter("month",month).getSingleResult();
  long cycle=em.createQuery("select e.saleId from PartnerEntry e where e.partnerId=:p and e.status<>'SETTLED' group by e.saleId having sum(e.amountCents)>0",String.class).setParameter("p",id).getResultList().size();
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("pendingCents",pending);result.put("availableCents",available);result.put("reservedCents",reserved);
  result.put("paidCents",paid);result.put("reversedCents",Math.max(0,reversed));result.put("lifetimeEarnedCents",earned);
  result.put("lifetimeQualifiedUses",lifetime);result.put("monthQualifiedUses",monthly);result.put("unpaidUses",cycle);
  result.put("minimumPayoutCents",5000);result.put("holdDays",environment.getProperty("smartpruefung.partner.hold-days",Integer.class,14));
  return result;
 }

 public Object commissions(String auth,int page,int size){return page("PartnerEntry","partnerId=:p",Map.of("p",own(auth).id),page,size);}
 public Object payouts(String auth,int page,int size){return page("PartnerPayout","partnerId=:p",Map.of("p",own(auth).id),page,size);}
 public Object requestPayout(String auth,String key){return requestPayout(lock(own(auth).id),key);}
 Object requestPayout(PartnerProfile p,String key){key=text(key,8,100,"request identifier");String scoped=p.id+":"+key;PartnerPayout previous=first(PartnerPayout.class,"requestKey",scoped);if(previous!=null)return previous;if(!"APPROVED".equals(p.status))throw error(403,"Partner account must be approved.");if(!p.payoutVerified)throw error(409,"An administrator must verify your payout details first.");List<PartnerEntry> entries=em.createQuery("from PartnerEntry where partnerId=:p and status='OPEN' and payoutId is null and eligibleAt<=:now",PartnerEntry.class).setParameter("p",p.id).setParameter("now",Instant.now()).getResultList();long amount=entries.stream().mapToLong(e->e.amountCents).sum();if(amount<5000)throw error(400,"At least €50 in available commission is required.");PartnerPayout pay=new PartnerPayout();pay.partnerId=p.id;pay.requestKey=scoped;pay.amountCents=amount;pay.method=p.payoutMethod;pay.maskedDestination=p.payoutMasked;pay.destinationEncrypted=p.payoutEncrypted;em.persist(pay);for(PartnerEntry e:entries){e.payoutId=pay.id;e.status="RESERVED";}audit(p.id,p.userId,"PAYOUT_REQUESTED",pay.id);return pay;}
 public Object list(String q,String status,int page,int size){return page("PartnerProfile","(lower(displayName) like :q or lower(contactEmail) like :q) and (:s='' or status=:s)",Map.of("q","%"+text(q,0,160,"search").toLowerCase(Locale.ROOT)+"%","s",status),page,size,true);}
 public Object detail(String id){PartnerProfile p=em.find(PartnerProfile.class,id);if(p==null)throw error(404,"Partner not found.");return Map.of("profile",view(p),"overview",balances(id),"codes",em.createQuery("from PartnerCode where partnerId=:p order by createdAt desc",PartnerCode.class).setParameter("p",id).getResultList());}
 public Object adminPage(String entity,String partnerId,String status,int page,int size){
  @SuppressWarnings("unchecked") Map<String,Object> result=new LinkedHashMap<>((Map<String,Object>)page(entity,"(:p='' or partnerId=:p) and (:s='' or "+("PartnerSale".equals(entity)?"state":"status")+"=:s)",Map.of("p",partnerId,"s",status),page,size));
  List<?> records=(List<?>)result.get("content");
  Set<String> ids=new HashSet<>();
  for(Object record:records)ids.add(record instanceof PartnerSale?((PartnerSale)record).partnerId:((PartnerPayout)record).partnerId);
  Map<String,String> names=new HashMap<>();
  if(!ids.isEmpty())for(Object[] row:em.createQuery("select p.id,p.displayName from PartnerProfile p where p.id in :ids",Object[].class).setParameter("ids",ids).getResultList())names.put((String)row[0],(String)row[1]);
  List<Map<String,Object>> rows=new ArrayList<>();
  for(Object record:records){
   @SuppressWarnings("unchecked") Map<String,Object> row=json.convertValue(record,LinkedHashMap.class);
   String id=record instanceof PartnerSale?((PartnerSale)record).partnerId:((PartnerPayout)record).partnerId;
   row.put("partnerName",names.getOrDefault(id,"Partner account"));rows.add(row);
  }
  result.put("content",rows);return result;
 }

 public Object page(String entity,String where,Map<String,Object> params,int page,int size){return page(entity,where,params,page,size,false);}
 Object page(String entity,String where,Map<String,Object> params,int page,int size,boolean profiles){page=Math.max(0,page);size=Math.max(1,Math.min(100,size));String w=where.isEmpty()?"":" where "+where;javax.persistence.Query count=em.createQuery("select count(e) from "+entity+" e"+w);javax.persistence.Query rows=em.createQuery("from "+entity+w+" order by createdAt desc,id");params.forEach((k,v)->{count.setParameter(k,v);rows.setParameter(k,v);});List<?> content=rows.setFirstResult(Math.min(page,100000)*size).setMaxResults(size).getResultList();if(profiles){List<Object> safe=new ArrayList<>();for(Object p:content)safe.add(view((PartnerProfile)p));content=safe;}return Map.of("content",content,"totalElements",count.getSingleResult(),"number",page,"size",size);}
 String adminActor(){return "admin:"+SecurityContextHolder.getContext().getAuthentication().getName();}
 void audit(String id,String actor,String action,String reason){PartnerAudit a=new PartnerAudit();a.partnerId=id;a.actor=actor;a.action=action;a.reason=reason;em.persist(a);}
 public Object decision(String id,long version,String status,String reason){PartnerProfile p=lock(id);version(p,version);reason=text(reason,3,1000,"review reason");if(!Set.of("APPROVED","REJECTED","SUSPENDED","NEEDS_INFO").contains(Objects.toString(status,"")))throw error(400,"Choose a valid partner status.");if(!"APPROVED".equals(status)){for(PartnerCode c:em.createQuery("from PartnerCode where partnerId=:p and active=true",PartnerCode.class).setParameter("p",id).getResultList()){c.active=false;c.validTo=Instant.now();c.retirementSource="ADMIN";}cancelReservations(id,"Partner status changed.");}p.status=status;p.reviewReason=reason;p.reviewedBy=adminActor();if("APPROVED".equals(status))p.approvedAt=Instant.now();if("SUSPENDED".equals(status))p.suspendedAt=Instant.now();audit(id,adminActor(),"STATUS_"+status,reason);em.flush();return detail(id);}
 public Object commission(String id,long version,int bps,int discountBps,String reason){PartnerProfile p=lock(id);version(p,version);reason=text(reason,3,1000,"reason");if(bps<0||bps>10000||discountBps<1||discountBps>9999)throw error(400,"Check commission and discount percentages.");if(discountBps!=p.discountBps&&em.createQuery("select count(c) from PartnerCode c where c.partnerId=:p and c.active=true",Long.class).setParameter("p",id).getSingleResult()>0)throw error(409,"Retire the current Stripe codes before changing the learner discount.");reason=reason+"; previous commission bps="+p.commissionBps+"; previous discount bps="+p.discountBps;p.commissionBps=bps;p.discountBps=discountBps;audit(id,adminActor(),"RATES_CHANGED",reason+"; commission bps="+bps+"; discount bps="+discountBps);em.flush();return detail(id);}
 public Object verifyPayout(String id,long version,String reason){PartnerProfile p=lock(id);version(p,version);reason=text(reason,3,1000,"verification reason");p.payoutVerified=true;audit(id,adminActor(),"PAYOUT_METHOD_VERIFIED",reason);em.flush();return detail(id);}
 public Object payoutDetails(String id,String reason){PartnerPayout payout=em.find(PartnerPayout.class,id);if(payout==null)throw error(404,"Payout not found.");reason=text(reason,3,1000,"access reason");audit(payout.partnerId,adminActor(),"PAYOUT_DETAILS_ACCESSED",reason);try{return json.readTree(crypto.decrypt(payout.destinationEncrypted));}catch(Exception e){throw error(503,"Unable to decrypt payout details.");}}
 public Object profilePayoutDetails(String id,String reason){PartnerProfile p=lock(id);reason=text(reason,3,1000,"access reason");audit(id,adminActor(),"PAYOUT_DETAILS_ACCESSED",reason);try{return json.readTree(crypto.decrypt(p.payoutEncrypted));}catch(Exception e){throw error(503,"Unable to decrypt payout details.");}}
 public Object adminRequest(String id,String key){return requestPayout(lock(id),key);}
 public Object settle(String id,long version,String action,String reference,Instant paidAt,String reason,boolean confirmed){PartnerPayout pay=em.find(PartnerPayout.class,id);if(pay==null)throw error(404,"Payout not found.");PartnerProfile p=lock(pay.partnerId);em.refresh(pay);version(pay,version);reason=text(reason,3,1000,"reason");if(!Set.of("REQUESTED","APPROVED").contains(pay.status))throw error(409,"Payout is already closed.");if("cancel".equals(action)){cancel(pay,reason);}else if("approve".equals(action)){if(!"APPROVED".equals(p.status)||!p.payoutVerified)throw error(409,"Check partner approval and payout verification.");pay.status="APPROVED";}else if("mark-paid".equals(action)){if(!"APPROVED".equals(pay.status)||!confirmed||!p.payoutVerified||!"APPROVED".equals(p.status))throw error(409,"Approve this request and confirm the external transfer first.");reference=text(reference,3,160,"transfer reference");if(paidAt==null||paidAt.isAfter(Instant.now())||paidAt.isBefore(pay.createdAt))throw error(400,"Choose a valid transfer date.");List<PartnerEntry> entries=em.createQuery("from PartnerEntry where payoutId=:p and status='RESERVED'",PartnerEntry.class).setParameter("p",id).getResultList();if(entries.stream().mapToLong(e->e.amountCents).sum()!=pay.amountCents||pay.amountCents<5000)throw error(409,"Reserved funds do not match the payout.");if(((Number)balances(p.id).get("availableCents")).longValue()<0)throw error(409,"Resolve negative carry-forward before payment.");entries.forEach(e->e.status="SETTLED");pay.status="MARKED_PAID";pay.externalReference=reference;pay.paidAt=paidAt;}else throw error(400,"Unknown payout action.");pay.adminActor=adminActor();pay.reason=reason;audit(p.id,adminActor(),"PAYOUT_"+action.toUpperCase(Locale.ROOT),reason);em.flush();return pay;}
 void cancelReservations(String id,String reason){for(PartnerPayout p:em.createQuery("from PartnerPayout where partnerId=:p and status in ('REQUESTED','APPROVED')",PartnerPayout.class).setParameter("p",id).getResultList())cancel(p,reason);}
 void cancel(PartnerPayout p,String reason){for(PartnerEntry e:em.createQuery("from PartnerEntry where payoutId=:p and status='RESERVED'",PartnerEntry.class).setParameter("p",p.id).getResultList()){e.payoutId=null;e.status="OPEN";}p.status="CANCELLED";p.reason=reason;}
}
