package com.smartpruefung.backend.Admin.Console.Model;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.time.Instant;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(
    name = "admin_audit",
    indexes = @Index(name = "idx_admin_audit_created", columnList = "createdAt"))
@Data
public class AdminAudit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private String actor;

  @Column(nullable = false)
  private String action;

  @Column(nullable = false)
  private String entityType;

  private String entityId;

  @Lob
  @Column(columnDefinition = "longtext")
  private String beforeJson;

  @Lob
  @Column(columnDefinition = "longtext")
  private String afterJson;

  @Column(nullable = false, length = 1000)
  private String reason;
}
