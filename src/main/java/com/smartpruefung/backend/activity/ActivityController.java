package com.smartpruefung.backend.activity;

import com.smartpruefung.backend.User.Service.UserService;
import com.smartpruefung.backend.Utils.JwtUtils;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {
 private final ActivityService activity;
 private final UserService users;
 public ActivityController(ActivityService activity,UserService users){this.activity=activity;this.users=users;}
 public static class Input {
  @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") public String eventId;
  @NotBlank @Size(max=32) public String type;
  @Size(max=32) public String plan;
  @Size(max=100) @Pattern(regexp="[a-zA-Z0-9/_-]*") public String source;
 }
 @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT)
 public void record(@RequestHeader("Authorization") String authorization,@Valid @RequestBody Input input){
  String email=JwtUtils.getHttpClientConsumer(authorization).email;
  if(email==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  var user=users.getUserByEmail(email);
  if(user==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  activity.browser(input.eventId,user.getUuid(),input.type,input.plan,input.source);
 }
}
