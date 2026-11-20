package com.smartpruefung.backend.Admin.Console.Controller;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import com.smartpruefung.backend.exam.dto.CreateTemplateRequest;
import java.util.*;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1")
@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public class AdminApiController {
  private final AdminManagementService service;
  private final AdminAnalyticsService analytics;
  private final AdminExamService exams;

  private Object paged(org.springframework.data.domain.Page<?> page) {
    return AdminManagementService.row(
        "content",
        page.getContent(),
        "totalElements",
        page.getTotalElements(),
        "totalPages",
        page.getTotalPages(),
        "number",
        page.getNumber(),
        "size",
        page.getSize());
  }

  private String why(String value) {
    return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
  }

  public AdminApiController(
      AdminManagementService service, AdminAnalyticsService analytics, AdminExamService exams) {
    this.service = service;
    this.analytics = analytics;
    this.exams = exams;
  }

  @GetMapping("/dashboard")
  public Object dashboard(@RequestParam(defaultValue = "30") int days) {
    return analytics.dashboard(days);
  }

  @GetMapping("/users")
  public Object users(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String enabled,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.users(q, enabled, page, size));
  }

  @GetMapping("/users/{id}")
  public Object user(@PathVariable long id) {
    return service.userView(service.user(id));
  }

  @PostMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createUser(@Valid @RequestBody AdminRequests.UserInput input) {
    return service.saveUser(null, input);
  }

  @PutMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Object updateUser(
      @PathVariable long id, @Valid @RequestBody AdminRequests.UserInput input) {
    return service.saveUser(id, input);
  }

  @DeleteMapping("/users/{id}")
  @PreAuthorize("hasRole('SUPERADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable long id, @RequestHeader("X-Reason") String reason) {
    service.deleteUser(id, why(reason));
  }

  @GetMapping("/subscriptions")
  public Object subscriptions(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String plan,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.subscriptions(q, status, plan, page, size));
  }

  @PutMapping("/users/{id}/subscription")
  @PreAuthorize("hasRole('ADMIN')")
  public Object access(@PathVariable long id, @Valid @RequestBody AdminRequests.AccessInput input) {
    return service.saveAccess(id, input);
  }

  @GetMapping("/subscriptions/{id}/history")
  public Object history(
      @PathVariable long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.history(id, page, size);
  }

  @GetMapping("/plans")
  public Object plans() {
    return service.plans();
  }

  @PutMapping("/plans/{code}")
  @PreAuthorize("hasRole('SUPERADMIN')")
  public Object plan(@PathVariable String code, @Valid @RequestBody AdminRequests.PlanInput input) {
    return service.savePlan(code, input);
  }

  @GetMapping("/exams")
  public Object exams(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(exams.list(q, active, page, size));
  }

  @GetMapping("/exams/{id}")
  public Object exam(@PathVariable UUID id) {
    return exams.detail(id);
  }

  @PostMapping("/exams")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createExam(@Valid @RequestBody AdminRequests.ExamInput input) {
    return exams.save(null, 0, input);
  }

  @PutMapping("/exams/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Object saveExam(
      @PathVariable UUID id,
      @RequestHeader("If-Match") long version,
      @Valid @RequestBody AdminRequests.ExamInput input) {
    return exams.save(id, version, input);
  }

  @PostMapping("/exams/{id}/clone")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object cloneExam(@PathVariable UUID id, @RequestHeader("X-Reason") String reason) {
    return exams.clone(id, why(reason));
  }

  @PutMapping("/exams/{id}/publication")
  @PreAuthorize("hasRole('ADMIN')")
  public Object publish(
      @PathVariable UUID id,
      @RequestParam boolean active,
      @RequestHeader("If-Match") long version,
      @RequestHeader("X-Reason") String reason) {
    return exams.publish(id, version, active, why(reason));
  }

  @DeleteMapping("/exams/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteExam(
      @PathVariable UUID id,
      @RequestHeader("If-Match") long version,
      @RequestHeader("X-Reason") String reason) {
    exams.delete(id, version, why(reason));
  }

  @GetMapping("/templates")
  public Object templates(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(exams.templates(q, page, size));
  }

  @PostMapping("/parts/{partId}/templates")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createTemplate(
      @PathVariable UUID partId,
      @Valid @RequestBody CreateTemplateRequest input,
      @RequestHeader("X-Reason") String reason) {
    return exams.saveTemplate(partId, null, 0, input, why(reason));
  }

  @PutMapping("/parts/{partId}/templates/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Object updateTemplate(
      @PathVariable UUID partId,
      @PathVariable UUID id,
      @RequestHeader("If-Match") long version,
      @Valid @RequestBody CreateTemplateRequest input,
      @RequestHeader("X-Reason") String reason) {
    return exams.saveTemplate(partId, id, version, input, why(reason));
  }

  @PutMapping("/templates/{id}/publication")
  @PreAuthorize("hasRole('ADMIN')")
  public Object toggleTemplate(
      @PathVariable UUID id,
      @RequestParam boolean active,
      @RequestHeader("If-Match") long version,
      @RequestHeader("X-Reason") String reason) {
    return exams.toggleTemplate(id, version, active, why(reason));
  }

  @DeleteMapping("/templates/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTemplate(
      @PathVariable UUID id,
      @RequestHeader("If-Match") long version,
      @RequestHeader("X-Reason") String reason) {
    exams.deleteTemplate(id, version, why(reason));
  }

  @GetMapping("/sessions")
  public Object sessions(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.sessions(q, status, page, size));
  }

  @GetMapping("/contacts")
  public Object contacts(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.contacts(q, page, size));
  }

  @DeleteMapping("/contacts/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteContact(@PathVariable long id, @RequestHeader("X-Reason") String reason) {
    service.deleteContact(id, why(reason));
  }

  @GetMapping("/promos")
  public Object promos(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.promos(q, page, size));
  }

  @PostMapping("/promos")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createPromo(@Valid @RequestBody AdminRequests.PromoInput input) {
    return service.savePromo(null, input);
  }

  @PutMapping("/promos/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Object savePromo(
      @PathVariable long id, @Valid @RequestBody AdminRequests.PromoInput input) {
    return service.savePromo(id, input);
  }

  @DeleteMapping("/promos/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePromo(@PathVariable long id, @RequestHeader("X-Reason") String reason) {
    service.deletePromo(id, why(reason));
  }

  @GetMapping("/admins")
  @PreAuthorize("hasRole('SUPERADMIN')")
  public Object admins(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return paged(service.admins(page, size));
  }

  @PostMapping("/admins")
  @PreAuthorize("hasRole('SUPERADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Object createAdmin(@Valid @RequestBody AdminRequests.AdminInput input) {
    return service.saveAdmin(null, input);
  }

  @PutMapping("/admins/{id}")
  @PreAuthorize("hasRole('SUPERADMIN')")
  public Object saveAdmin(
      @PathVariable long id, @Valid @RequestBody AdminRequests.AdminInput input) {
    return service.saveAdmin(id, input);
  }

  @GetMapping("/audit")
  public Object audit(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paged(service.audits(q, page, size));
  }
}
