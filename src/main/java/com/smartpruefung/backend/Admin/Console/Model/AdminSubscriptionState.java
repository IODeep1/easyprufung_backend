package com.smartpruefung.backend.Admin.Console.Model;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "admin_subscription_state")
@Data
public class AdminSubscriptionState {
  @Id private long id;
  private long version;
  private java.time.Instant touchedAt;
}
