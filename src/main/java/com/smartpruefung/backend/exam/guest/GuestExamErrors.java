package com.smartpruefung.backend.exam.guest;

import com.smartpruefung.backend.exam.api.ExamSessionController;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes={GuestExamController.class,ExamSessionController.class})
@org.springframework.core.annotation.Order(-2)
public class GuestExamErrors {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<?> status(ResponseStatusException error){
  return ResponseEntity.status(error.getStatus()).body(Map.of("message",error.getReason()==null?"Request unavailable":error.getReason()));
 }
}
