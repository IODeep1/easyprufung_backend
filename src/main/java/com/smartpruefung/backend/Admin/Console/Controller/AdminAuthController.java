package com.smartpruefung.backend.Admin.Console.Controller;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminAuthController {
  private final AdminAuthService service;

  public AdminAuthController(AdminAuthService service) {
    this.service = service;
  }

  @PostMapping("/public/admin/session")
  public Map<String, Object> login(
      @Valid @RequestBody AdminRequests.Login input, HttpServletRequest req) {
    return service.login(input, req.getRemoteAddr());
  }

  @GetMapping("/api/admin/v1/session")
  public Object me(org.springframework.security.core.Authentication auth) {
    return auth.getDetails();
  }

  @DeleteMapping("/api/admin/v1/session")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader("Authorization") String header) {
    service.logout(header.substring(7));
  }
}
