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
    name = "admin_sessions",
    indexes = @Index(name = "idx_admin_session_expiry", columnList = "expiresAt"))
@Data
public class AdminSession {
  @Id
  @Column(length = 64)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private com.smartpruefung.backend.Admin.Admin admin;

  private Instant createdAt;
  private Instant expiresAt;
}
