package com.smartpruefung.backend.teacher;

import static com.smartpruefung.backend.Admin.Console.Service.AdminManagementService.row;
import com.smartpruefung.backend.Admin.Console.Service.AdminManagementService;
import com.smartpruefung.backend.User.User;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import javax.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Explicit support projections: never expose invitation secrets or personal exam content. */
@Service @Transactional(readOnly=true)
public class AdminTeacherService {
 @PersistenceContext private EntityManager em;
 private final AdminManagementService admin;
 private final com.smartpruefung.backend.User.Repository.UsersRepository users;
 public AdminTeacherService(AdminManagementService admin,com.smartpruefung.backend.User.Repository.UsersRepository users){this.admin=admin;this.users=users;}
 private <T> T found(Class<T> type,String id){T value=em.find(type,id);if(value==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");return value;}
 private void choice(String value,String... allowed){if(!value.isEmpty()&&!Arrays.asList(allowed).contains(value))throw new IllegalArgumentException("Invalid filter value");}
 private String search(String q){if(q.length()>150)throw new IllegalArgumentException("Search must be at most 150 characters");return "%"+q.toLowerCase(Locale.ROOT).trim()+"%";}
 private String personSearch(){return "(lower(coalesce(u.email,'')) like :q or lower(coalesce(u.firstname,'')) like :q or lower(coalesce(u.lastname,'')) like :q)";}
 private Map<String,Object> person(User u,String uuid){return u==null?row("userId",uuid,"name",uuid!=null&&uuid.startsWith("admin:")?"Administrator":"Deleted account","email",uuid!=null&&uuid.startsWith("admin:")?uuid.substring(6):"","accountId",null):row("userId",uuid,"accountId",u.getId(),"name",(Objects.toString(u.getFirstname(),"")+" "+Objects.toString(u.getLastname(),"")).trim(),"email",u.getEmail());}
 private void bind(Query query,Map<String,Object> params){params.forEach(query::setParameter);}
 private Map<String,Object> page(String select,String from,String where,String order,Map<String,Object> params,int page,int size,Function<Object[],Map<String,Object>> map){
  AdminManagementService.paging(page,size);
  long offset=(long)page*size;if(offset>Integer.MAX_VALUE)throw new IllegalArgumentException("Page is too large");
  TypedQuery<Object[]> q=em.createQuery("select "+select+" from "+from+" where "+where+" order by "+order,Object[].class);bind(q,params);
  TypedQuery<Long> count=em.createQuery("select count(*) from "+from+" where "+where,Long.class);bind(count,params);
  List<Map<String,Object>> content=new ArrayList<>();for(Object[] value:q.setFirstResult((int)offset).setMaxResults(size).getResultList())content.add(map.apply(value));
  return row("content",content,"totalElements",count.getSingleResult(),"number",page,"size",size);
 }
 private long count(String hql){return em.createQuery(hql,Long.class).getSingleResult();}
 public Map<String,Object> summary(){
  Number paid=em.createQuery("select coalesce(sum(o.totalCents),0) from TeacherSeatOrder o where o.status='PAID' and lower(o.currency)='eur'",Number.class).getSingleResult();
  long live=em.createQuery("select count(s) from TeacherSeat s where s.state='ACTIVATED' and s.expiresAt>:now",Long.class).setParameter("now",Instant.now()).getSingleResult();
  return row("teachers",count("select count(p) from TeacherProfile p"),"activeTeachers",count("select count(p) from TeacherProfile p where p.status='ACTIVE'"),"pendingTeachers",count("select count(p) from TeacherProfile p where p.status='PENDING'"),"suspendedTeachers",count("select count(p) from TeacherProfile p where p.status='SUSPENDED'"),"activeClasses",count("select count(c) from TeacherClass c where c.archived=false"),"learners",count("select count(distinct m.studentId) from ClassMembership m, TeacherClass c where m.classId=c.id and m.active=true and c.archived=false"),"availableSeats",count("select count(s) from TeacherSeat s where s.state='AVAILABLE'"),"reservedSeats",count("select count(s) from TeacherSeat s where s.state='RESERVED'"),"activatedSeats",count("select count(s) from TeacherSeat s where s.state='ACTIVATED'"),"activeSeats",live,"paidOrderTotalCents",paid.longValue(),"reviewOrders",count("select count(o) from TeacherSeatOrder o where o.supportReview=true"),"generatedAt",Instant.now());
 }
 private Map<String,Object> profile(TeacherProfile p,User u){Map<String,Object> r=person(u,p.userId);r.putAll(row("id",p.id,"status",p.status,"version",p.version,"createdAt",p.createdAt));return r;}
 private Map<String,Long> grouped(String hql,Collection<String> ids){Map<String,Long> out=new HashMap<>();if(!ids.isEmpty())for(Object[] r:em.createQuery(hql,Object[].class).setParameter("ids",ids).getResultList())out.put(String.valueOf(r[0]),((Number)r[1]).longValue());return out;}
 @SuppressWarnings("unchecked") public Map<String,Object> profiles(String q,String status,int page,int size){
  choice(status,"ACTIVE","PENDING","SUSPENDED");Map<String,Object> params=row("q",search(q));String where=personSearch()+" or p.userId like :q or p.id like :q";where="("+where+")";if(!status.isEmpty()){where+=" and p.status=:status";params.put("status",status);}
  Map<String,Object> result=page("p,u","TeacherProfile p left join User u on u.uuid=p.userId",where,"p.createdAt desc,p.id",params,page,size,r->profile((TeacherProfile)r[0],(User)r[1]));
  List<Map<String,Object>> rows=(List<Map<String,Object>>)result.get("content");List<String> ids=new ArrayList<>();rows.forEach(r->ids.add((String)r.get("userId")));
  Map<String,Long> classes=grouped("select c.teacherId,count(c) from TeacherClass c where c.teacherId in :ids and c.archived=false group by c.teacherId",ids);
  Map<String,Long> learners=grouped("select c.teacherId,count(distinct m.studentId) from ClassMembership m, TeacherClass c where c.id=m.classId and c.teacherId in :ids and c.archived=false and m.active=true group by c.teacherId",ids);
  Map<String,Map<String,Long>> seats=new HashMap<>();if(!ids.isEmpty())for(Object[] v:em.createQuery("select s.teacherId,s.state,count(s) from TeacherSeat s where s.teacherId in :ids group by s.teacherId,s.state",Object[].class).setParameter("ids",ids).getResultList())seats.computeIfAbsent((String)v[0],key->new HashMap<>()).put((String)v[1],((Number)v[2]).longValue());
  for(Map<String,Object> r:rows){String id=(String)r.get("userId");Map<String,Long> wallet=seats.getOrDefault(id,Map.of());r.putAll(row("classes",classes.getOrDefault(id,0L),"learners",learners.getOrDefault(id,0L),"available",wallet.getOrDefault("AVAILABLE",0L),"reserved",wallet.getOrDefault("RESERVED",0L),"activated",wallet.getOrDefault("ACTIVATED",0L)));}return result;
 }
 public Map<String,Object> profile(String id){TeacherProfile p=found(TeacherProfile.class,id);User u=users.findByUUID(p.userId);Map<String,Object> r=profile(p,u);Map<String,Long> wallet=new LinkedHashMap<>();for(Object[] v:em.createQuery("select s.state,count(s) from TeacherSeat s where s.teacherId=:u group by s.state",Object[].class).setParameter("u",p.userId).getResultList())wallet.put((String)v[0],((Number)v[1]).longValue());r.put("seats",wallet);return r;}
 private Map<String,Object> classView(TeacherClass c,User u){Map<String,Object> r=row("id",c.id,"name",c.name,"teacherId",c.teacherId,"teacher",person(u,c.teacherId),"archived",c.archived,"version",c.version,"createdAt",c.createdAt,"invitationActive",!c.archived&&c.inviteHash!=null&&c.inviteExpiresAt!=null&&c.inviteExpiresAt.isAfter(Instant.now()),"inviteExpiresAt",c.inviteExpiresAt);return r;}
 @SuppressWarnings("unchecked") public Map<String,Object> classes(String q,String teacherId,String archived,int page,int size){
  choice(archived,"true","false");Map<String,Object> params=row("q",search(q));String where="(lower(c.name) like :q or c.id like :q or "+personSearch()+")";if(!teacherId.isEmpty()){where+=" and c.teacherId=:teacher";params.put("teacher",teacherId);}if(!archived.isEmpty()){where+=" and c.archived=:archived";params.put("archived",Boolean.parseBoolean(archived));}
  Map<String,Object> result=page("c,u","TeacherClass c left join User u on u.uuid=c.teacherId",where,"c.createdAt desc,c.id",params,page,size,r->classView((TeacherClass)r[0],(User)r[1]));List<Map<String,Object>> rows=(List<Map<String,Object>>)result.get("content");List<String> ids=new ArrayList<>();rows.forEach(r->ids.add((String)r.get("id")));
  Map<String,Long> members=grouped("select m.classId,count(m) from ClassMembership m where m.classId in :ids and m.active=true group by m.classId",ids),assignments=grouped("select a.classId,count(a) from ClassAssignment a where a.classId in :ids group by a.classId",ids);for(Map<String,Object> r:rows)r.putAll(row("members",members.getOrDefault(r.get("id"),0L),"assignments",assignments.getOrDefault(r.get("id"),0L)));return result;
 }
 public Map<String,Object> members(String id,int page,int size){found(TeacherClass.class,id);return page("m,u","ClassMembership m left join User u on u.uuid=m.studentId","m.classId=:id","m.createdAt desc,m.id",row("id",id),page,size,r->{ClassMembership m=(ClassMembership)r[0];Map<String,Object> v=person((User)r[1],m.studentId);v.putAll(row("id",m.id,"active",m.active,"consentedAt",m.consentedAt,"leftAt",m.leftAt,"consentVersion",m.consentVersion));return v;});}
 public Map<String,Object> assignments(String id,int page,int size){found(TeacherClass.class,id);Map<String,Object> result=page("a,a.id","ClassAssignment a","a.classId=:id","a.createdAt desc,a.id",row("id",id),page,size,r->{ClassAssignment a=(ClassAssignment)r[0];return row("id",a.id,"examCode",a.examCode,"dueAt",a.dueAt,"createdAt",a.createdAt);});return result;}
 private Map<String,Object> orderView(TeacherSeatOrder o,User u){return row("id",o.id,"teacherId",o.teacherId,"teacher",person(u,o.teacherId),"quantity",o.quantity,"unitPriceCents",o.unitPriceCents,"totalCents",o.totalCents,"currency",o.currency,"status",o.status,"supportReview",o.supportReview,"version",o.version,"stripeSessionId",o.stripeSessionId,"paymentIntentId",o.paymentIntentId,"stripePriceId",o.stripePriceId,"pricingTier",o.pricingTier,"stripeCustomerId",o.stripeCustomerId,"createdAt",o.createdAt);}
 public Map<String,Object> orders(String q,String teacherId,String status,String review,int page,int size){
  choice(status,"PENDING","PAID","FAILED","EXPIRED","REFUNDED","DISPUTED","SUPPORT_REVIEW");choice(review,"true","false");Map<String,Object> params=row("q",search(q));String where="(o.id like :q or o.stripeSessionId like :q or o.paymentIntentId like :q or "+personSearch()+")";if(!teacherId.isEmpty()){where+=" and o.teacherId=:teacher";params.put("teacher",teacherId);}if(!status.isEmpty()){where+=" and o.status=:status";params.put("status",status);}if(!review.isEmpty()){where+=" and o.supportReview=:review";params.put("review",Boolean.parseBoolean(review));}
  return page("o,u","TeacherSeatOrder o left join User u on u.uuid=o.teacherId",where,"o.createdAt desc,o.id",params,page,size,r->orderView((TeacherSeatOrder)r[0],(User)r[1]));
 }
 public Map<String,Object> seats(String id,int page,int size){found(TeacherSeatOrder.class,id);return page("s,u","TeacherSeat s left join User u on u.uuid=s.studentId","s.orderId=:id","s.seatNumber,s.id",row("id",id),page,size,r->{TeacherSeat s=(TeacherSeat)r[0];String effective="ACTIVATED".equals(s.state)&&s.expiresAt!=null&&!s.expiresAt.isAfter(Instant.now())?"EXPIRED":s.state;return row("id",s.id,"seatNumber",s.seatNumber,"state",effective,"storedState",s.state,"student",s.studentId==null?null:person((User)r[1],s.studentId),"activatedAt",s.activatedAt,"expiresAt",s.expiresAt);});}
 public Map<String,Object> offers(String id,int page,int size){found(TeacherSeatOrder.class,id);return page("o,c.name,u","SeatOffer o join TeacherSeat s on s.id=o.seatId left join TeacherClass c on c.id=o.classId left join User u on u.uuid=o.studentId","s.orderId=:id","o.createdAt desc,o.id",row("id",id),page,size,r->{SeatOffer o=(SeatOffer)r[0];return row("id",o.id,"seatId",o.seatId,"classId",o.classId,"className",r[1],"student",person((User)r[2],o.studentId),"state","PENDING".equals(o.state)&&!o.expiresAt.isAfter(Instant.now())?"EXPIRED":o.state,"createdAt",o.createdAt,"expiresAt",o.expiresAt,"resolvedAt",o.resolvedAt);});}
 public Map<String,Object> activity(String q,int page,int size){return page("e,u","TeacherAuditEvent e left join User u on u.uuid=e.actorId","(lower(e.action) like :q or lower(e.actorId) like :q or e.resourceId like :q or "+personSearch()+")","e.createdAt desc,e.id",row("q",search(q)),page,size,r->{TeacherAuditEvent e=(TeacherAuditEvent)r[0];return row("id",e.id,"actorId",e.actorId,"actor",person((User)r[1],e.actorId),"action",e.action,"resourceId",e.resourceId,"createdAt",e.createdAt);});}
 private void teacherAudit(String actor,String action,String id){TeacherAuditEvent e=new TeacherAuditEvent();e.actorId=actor;e.action=action;e.resourceId=id;em.persist(e);}
 private void releaseOffers(String classId){
  for(SeatOffer offer:em.createQuery("from SeatOffer where classId=:id and state='PENDING' order by seatId",SeatOffer.class).setParameter("id",classId).getResultList()){
   TeacherSeat seat=em.find(TeacherSeat.class,offer.seatId,LockModeType.PESSIMISTIC_WRITE);em.refresh(offer);
   if(!"PENDING".equals(offer.state))continue;
   if(seat!=null&&"RESERVED".equals(seat.state)){seat.state="AVAILABLE";seat.studentId=null;}
   offer.state="REVOKED";offer.resolvedAt=Instant.now();
  }
 }
 private <T extends TeacherRecord> T lock(Class<T> type,String id,long version){T record=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(record==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");em.refresh(record);if(record.version!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Record changed. Reload before saving");return record;}
 @Transactional public Map<String,Object> status(String id,String status,long version,String reason){AdminManagementService.reason(reason);if(!Set.of("ACTIVE","SUSPENDED").contains(Objects.toString(status,"")))throw new IllegalArgumentException("Choose ACTIVE or SUSPENDED");TeacherProfile p=lock(TeacherProfile.class,id,version);Map<String,Object> before=profile(p,null);p.status=status;em.flush();Map<String,Object> after=profile(p,null);admin.audit("TEACHER_STATUS_CHANGED","TeacherProfile",id,before,after,reason);teacherAudit("admin:"+SecurityContextHolder.getContext().getAuthentication().getName(),"TEACHER_"+status,id);return profile(id);}
 @Transactional public Map<String,Object> archive(String id,long version,String reason){AdminManagementService.reason(reason);TeacherClass c=lock(TeacherClass.class,id,version);Map<String,Object> before=classView(c,null);c.archived=true;c.inviteHash=null;c.inviteToken=null;c.inviteExpiresAt=null;releaseOffers(id);em.flush();Map<String,Object> after=classView(c,null);admin.audit("TEACHER_CLASS_ARCHIVED","TeacherClass",id,before,after,reason);teacherAudit("admin:"+SecurityContextHolder.getContext().getAuthentication().getName(),"CLASS_ARCHIVED",id);return after;}
 @Transactional public Map<String,Object> review(String id,boolean review,long version,String reason){AdminManagementService.reason(reason);TeacherSeatOrder o=lock(TeacherSeatOrder.class,id,version);Map<String,Object> before=row("supportReview",o.supportReview,"version",o.version);o.supportReview=review;em.flush();admin.audit("TEACHER_ORDER_REVIEW_CHANGED","TeacherSeatOrder",id,before,row("supportReview",o.supportReview,"version",o.version),reason);teacherAudit("admin:"+SecurityContextHolder.getContext().getAuthentication().getName(),review?"ORDER_REVIEW_OPENED":"ORDER_REVIEW_RESOLVED",id);return orderView(o,users.findByUUID(o.teacherId));}
}
