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
@Table(name = "admin_credentials")
@Data
public class AdminCredential {
  @Id private long id;

  @Column(nullable = false)
  private String passwordHash;

  private boolean enabled = true;
  private long version;
  private Instant validAfter;
  private Instant touchedAt;
}
