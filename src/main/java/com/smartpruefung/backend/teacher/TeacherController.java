package com.smartpruefung.backend.teacher;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.*;
import java.time.Instant;
import java.util.*;
@RestController
public class TeacherController {
 private final TeacherService service; private final TeacherPayments payments;
 @PersistenceContext EntityManager em;
 public TeacherController(TeacherService service,TeacherPayments payments){this.service=service;this.payments=payments;}
 public static class ClassInput {public String name;}
 public static class JoinInput {public boolean consent;}
 public static class AssignInput {public String examCode;public Instant dueAt;public List<String> studentIds;}
 public static class OfferInput {public String classId;public String studentId;}
 public static class BuyInput {
  public java.math.BigDecimal quantity;
  public int quantity(){if(quantity==null)throw new IllegalArgumentException("Quantity is required.");try{return quantity.intValueExact();}catch(ArithmeticException ex){throw new IllegalArgumentException("Choose a whole number of seats between 1 and 1000.");}}
 }
 public static class StudentBuyInput {public String plan;}
 public static class StatusInput {public String status;}
 @PostMapping("/api/teacher/apply") public Object apply(@RequestHeader("Authorization") String a) throws Exception {return service.apply(a);}
 @GetMapping("/api/teacher/me") public Object me(@RequestHeader("Authorization") String a) throws Exception {return service.me(a);}
 @GetMapping("/api/teacher/summary") public Object summary(@RequestHeader("Authorization") String a){return service.summary(a);}
 @GetMapping("/api/teacher/classes") public Object classes(@RequestHeader("Authorization") String a) throws Exception {return service.classes(a);}
 @PostMapping("/api/teacher/classes") public Object createClass(@RequestHeader("Authorization") String a,@RequestBody ClassInput b) throws Exception {return service.createClass(a,b.name);}
 @GetMapping("/api/teacher/classes/{id}") public Object classDetail(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.owned(a,id,false);}
 @PatchMapping("/api/teacher/classes/{id}") public Object renameClass(@RequestHeader("Authorization") String a,@PathVariable String id,@RequestBody ClassInput b) throws Exception {return service.updateClass(a,id,b.name,false);}
 @PostMapping("/api/teacher/classes/{id}/archive") public Object archiveClass(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.updateClass(a,id,null,true);}
 @PostMapping("/api/teacher/classes/{id}/invites") public Object createInvite(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.invite(a,id,false);}
 @PostMapping("/api/teacher/classes/{id}/invites/rotate") public Object rotateInvite(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.invite(a,id,false);}
 @DeleteMapping("/api/teacher/classes/{id}/invites") public Object revokeInvite(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.invite(a,id,true);}
 @GetMapping("/api/teacher/classes/{id}/students") public Object roster(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.roster(a,id);}
 @DeleteMapping("/api/teacher/classes/{id}/students/{student}") public Object removeStudent(@RequestHeader("Authorization") String a,@PathVariable String id,@PathVariable String student) throws Exception {service.leave(a,id,student);return Map.of("removed",true);}
 @GetMapping("/api/teacher/exams") public Object examChoices(@RequestHeader("Authorization") String a) throws Exception {return service.examChoices(a);}
 @GetMapping("/api/teacher/classes/{id}/assignments") public Object assignments(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.assignments(a,id);}
 @PostMapping("/api/teacher/classes/{id}/assignments") public Object assignExam(@RequestHeader("Authorization") String a,@PathVariable String id,@RequestBody AssignInput b) throws Exception {return service.assign(a,id,b.examCode,b.dueAt,b.studentIds);}
 @GetMapping("/api/teacher/classes/{id}/performance") public Object performance(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.performance(a,id);}
 @GetMapping("/api/classes/invites/{token}/preview") public Object previewInvite(@RequestHeader("Authorization") String a,@PathVariable String token) throws Exception {return service.preview(a,token);}
 @PostMapping("/api/classes/invites/{token}/join") public Object joinClass(@RequestHeader("Authorization") String a,@PathVariable String token,@RequestBody JoinInput b) throws Exception {return service.join(a,token,b.consent);}
 @GetMapping("/api/student/classes") public Object studentClasses(@RequestHeader("Authorization") String a) throws Exception {return service.studentClasses(a);}
 @PostMapping("/api/student/classes/{id}/leave") public Object leaveClass(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {service.leave(a,id,null);return Map.of("left",true);}
 @GetMapping("/api/student/assignments") public Object studentAssignments(@RequestHeader("Authorization") String a) throws Exception {return service.studentAssignments(a);}
 @PostMapping("/api/student/assignments/{id}/start") public Object startAssignment(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.startAssignment(a,id);}
 @GetMapping("/api/student/seat-offers") public Object studentOffers(@RequestHeader("Authorization") String a) throws Exception {return service.studentOffers(a);}
 @PostMapping("/api/student/seat-offers/{id}/accept") public Object acceptSeat(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.resolveOffer(a,id,"ACCEPTED");}
 @PostMapping("/api/student/seat-offers/{id}/decline") public Object declineSeat(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.resolveOffer(a,id,"DECLINED");}
 @GetMapping("/api/teacher/seats/pricing") public Object pricing(@RequestHeader("Authorization") String a){service.teacher(a);return Map.of("currency","EUR","tiers",SeatPricing.schedule(),"maximumQuantity",1000);}
 @GetMapping("/api/teacher/seats/orders/{id}") public Object orderState(@RequestHeader("Authorization") String a,@PathVariable String id){return service.orderState(a,id);}
 @GetMapping("/api/teacher/seats/quote") public Object quote(@RequestHeader("Authorization") String a,@RequestParam int quantity) throws Exception {service.teacher(a);return SeatPricing.quote(quantity);}
 @PostMapping("/api/teacher/seats/checkout") public Object checkout(@RequestHeader("Authorization") String a,@RequestBody BuyInput b) throws Exception {return payments.checkout(a,b.quantity());}
 @GetMapping("/api/teacher/seats/wallet") public Object wallet(@RequestHeader("Authorization") String a) throws Exception {return service.wallet(a);}
 @GetMapping("/api/teacher/seats/orders") public Object orders(@RequestHeader("Authorization") String a) throws Exception {return service.orders(a);}
 @PostMapping("/api/teacher/seats/offer") public Object offerSeat(@RequestHeader("Authorization") String a,@RequestBody OfferInput b) throws Exception {return service.offer(a,b.classId,b.studentId);}
 @PostMapping("/api/teacher/seats/offers/{id}/revoke") public Object revokeSeat(@RequestHeader("Authorization") String a,@PathVariable String id) throws Exception {return service.resolveOffer(a,id,"REVOKED");}
 @PostMapping("/api/student/access/checkout") public Object studentCheckout(@RequestHeader("Authorization") String a,@RequestBody StudentBuyInput b) throws Exception {return payments.studentCheckout(a,b.plan);}
 @GetMapping("/api/admin/teacher/profiles") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Object profiles(){return em.createQuery("from TeacherProfile",TeacherProfile.class).getResultList();}
 @PostMapping("/api/admin/teacher/profiles/{id}/status") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Object status(@RequestHeader("Authorization") String a,@PathVariable String id,@RequestBody StatusInput b){
  if(!List.of("ACTIVE","SUSPENDED").contains(b.status))throw TeacherService.error(400,"Use ACTIVE or SUSPENDED.");
  TeacherProfile p=service.require(TeacherProfile.class,id);p.status=b.status;service.audit("admin:"+org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName(),"TEACHER_"+b.status,id);return p;
 }
 @GetMapping("/api/admin/teacher/orders") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Object supportOrders(){return em.createQuery("from TeacherSeatOrder order by createdAt desc",TeacherSeatOrder.class).getResultList();}
 @GetMapping("/api/admin/teacher/orders/{id}/seats") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Object supportSeats(@PathVariable String id){return em.createQuery("from TeacherSeat where orderId=:id",TeacherSeat.class).setParameter("id",id).getResultList();}
}
