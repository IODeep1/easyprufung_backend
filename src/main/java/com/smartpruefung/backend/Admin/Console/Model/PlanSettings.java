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
@Table(name = "admin_plan_settings")
@Data
public class PlanSettings {
  @Id private String code;
  @Version private long version;
  private String title;
  private int quota;
  private Integer accessDays;
  private String priceId;
  private boolean enabled = true;
}
