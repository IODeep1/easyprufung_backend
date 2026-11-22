package com.smartpruefung.backend.Admin.Console.Controller;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {AdminApiController.class, AdminAuthController.class, AdminTeacherController.class})
@org.springframework.core.annotation.Order(-1)
public class AdminErrors {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> input(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "Invalid input" : e.getMessage()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatus())
        .body(Map.of("message", e.getReason() == null ? "Request failed" : e.getReason()));
  }

  @ExceptionHandler({
    ObjectOptimisticLockingFailureException.class,
    javax.persistence.OptimisticLockException.class
  })
  public ResponseEntity<?> version(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Record changed. Reload before saving"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> integrity(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "This change conflicts with an existing record or reference"));
  }
}
