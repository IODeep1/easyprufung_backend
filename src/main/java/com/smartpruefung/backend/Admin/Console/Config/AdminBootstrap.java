package com.smartpruefung.backend.Admin.Console.Config;

import com.smartpruefung.backend.Admin.Admin;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import com.smartpruefung.backend.Admin.Repository.*;
import com.smartpruefung.backend.Admin.Role;
import com.smartpruefung.backend.Security.AESEncryption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap {
  private final AdminsRepository admins;
  private final RolesRepository roles;
  private final AdminCredentialRepository credentials;
  private final PlanCatalog plans;

  @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
  private String email;

  @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
  private String password;

  @Value("${ADMIN_BOOTSTRAP_RESET:false}")
  private boolean reset;

  public AdminBootstrap(
      AdminsRepository admins,
      RolesRepository roles,
      AdminCredentialRepository credentials,
      PlanCatalog plans) {
    this.admins = admins;
    this.roles = roles;
    this.credentials = credentials;
    this.plans = plans;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void provision() {
    for (String role : Set.of("SuperAdmin", "Admin", "Viewer"))
      if (roles.findByName(role) == null) roles.save(new Role(role));
    plans.seed();
    if (email.isBlank() || password.isBlank()) return;
    String address = email.trim().toLowerCase(Locale.ROOT);
    Admin a = admins.findByEmail(address);
    if (a != null && credentials.existsById(a.getId()) && !reset) return;
    if (password.length() < 12)
      throw new IllegalArgumentException("Admin bootstrap password requires 12 characters");
    if (a == null) {
      a = new Admin();
      a.setFirstname("Platform");
      a.setLastname("Administrator");
      a.setEmail(address);
      a.setCreatedDate(new Timestamp(System.currentTimeMillis()));
      a.setPassword(AESEncryption.encrypt(UUID.randomUUID().toString() + "Aa9!"));
    }
    a.addRole(roles.findByName("SuperAdmin"));
    a.addRole(roles.findByName("Admin"));
    a.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
    a = admins.saveAndFlush(a);
    AdminCredential c = credentials.findById(a.getId()).orElse(new AdminCredential());
    c.setId(a.getId());
    c.setPasswordHash(AdminPasswords.encode(password));
    c.setEnabled(true);
    c.setVersion(c.getVersion() + 1);
    c.setValidAfter(Instant.now());
    credentials.save(c);
  }
}
