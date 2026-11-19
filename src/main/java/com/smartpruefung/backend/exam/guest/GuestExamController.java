package com.smartpruefung.backend.exam.guest;

import com.smartpruefung.backend.exam.dto.ExamProgressView;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/public/exams/guest")
public class GuestExamController {
 private final GuestExamService guests;
 public GuestExamController(GuestExamService guests){this.guests=guests;}
 @PostMapping public Object create(HttpServletRequest request){return guests.create(request.getRemoteAddr());}
 @PostMapping("/start") public Object start(@RequestHeader("X-Guest-Token") String token){return guests.start(token);}
 @GetMapping public Object state(@RequestHeader("X-Guest-Token") String token){return guests.state(token);}
 @PutMapping("/progress") public Object progress(@RequestHeader("X-Guest-Token") String token,@Valid @RequestBody ExamProgressView draft){return guests.save(token,draft);}
 @PostMapping("/finish") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void finish(@RequestHeader("X-Guest-Token") String token){guests.finish(token);}
}
