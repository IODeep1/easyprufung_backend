package com.smartpruefung.backend.activity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/v1/activity")
@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public class AdminActivityController {
 private final ActivityService service;
 public AdminActivityController(ActivityService service){this.service=service;}
 @GetMapping("/summary")
 public Object summary(@RequestParam(defaultValue="") String from,@RequestParam(defaultValue="") String to,
  @RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String user){
  return service.summary(from,to,type,user);
 }
 @GetMapping
 public Object list(@RequestParam(defaultValue="") String from,@RequestParam(defaultValue="") String to,
  @RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String user,
  @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
  @RequestParam(defaultValue="false") boolean grouped){
  return service.list(from,to,type,user,page,size,grouped);
 }
}
