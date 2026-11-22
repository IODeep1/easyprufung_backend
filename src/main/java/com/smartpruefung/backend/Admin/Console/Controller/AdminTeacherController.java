package com.smartpruefung.backend.Admin.Console.Controller;

import com.smartpruefung.backend.teacher.AdminTeacherService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/teacher-mode")
@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public class AdminTeacherController {
 private final AdminTeacherService service;
 public AdminTeacherController(AdminTeacherService service){this.service=service;}
 public static class Change {public String status;public String reason;public Boolean supportReview;}
 @GetMapping("/summary") public Object summary(){return service.summary();}
 @GetMapping("/teachers") public Object teachers(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.profiles(q,status,page,size);}
 @GetMapping("/teachers/{id}") public Object teacher(@PathVariable String id){return service.profile(id);}
 @PutMapping("/teachers/{id}/status") @PreAuthorize("hasRole('ADMIN')") public Object status(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change input){return service.status(id,input.status,version,input.reason);}
 @GetMapping("/classes") public Object classes(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String teacherId,@RequestParam(defaultValue="") String archived,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.classes(q,teacherId,archived,page,size);}
 @GetMapping("/classes/{id}/members") public Object members(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.members(id,page,size);}
 @GetMapping("/classes/{id}/assignments") public Object assignments(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.assignments(id,page,size);}
 @PostMapping("/classes/{id}/archive") @PreAuthorize("hasRole('ADMIN')") public Object archive(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change input){return service.archive(id,version,input.reason);}
 @GetMapping("/orders") public Object orders(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String teacherId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String review,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.orders(q,teacherId,status,review,page,size);}
 @GetMapping("/orders/{id}/seats") public Object seats(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.seats(id,page,size);}
 @GetMapping("/orders/{id}/offers") public Object offers(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.offers(id,page,size);}
 @PutMapping("/orders/{id}/review") @PreAuthorize("hasRole('ADMIN')") public Object review(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change input){if(input.supportReview==null)throw new IllegalArgumentException("Choose a support review status");return service.review(id,input.supportReview,version,input.reason);}
 @GetMapping("/activity") public Object activity(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.activity(q,page,size);}
}
