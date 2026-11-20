package com.smartpruefung.backend.Admin.Console.Service;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanCatalog {
  @PersistenceContext private EntityManager em;
  @org.springframework.beans.factory.annotation.Autowired private PlanSettingsRepository repository;
  private final Environment env;

  public PlanCatalog(Environment env) {
    this.env = env;
  }

  public static final Set<String> CODES = Set.of("free", "b1", "b1_unlimited", "tester");

  @Transactional(readOnly = true)
  public PlanSettings get(String code) {
    if (!CODES.contains(code)) throw new IllegalArgumentException("Unknown plan");
    PlanSettings found = repository.findById(code).orElse(null);
    if (found != null) return found;
    PlanSettings p = new PlanSettings();
    p.setCode(code);
    p.setEnabled(true);
    p.setTitle(
        Map.of(
                "free",
                "Free practice",
                "b1",
                "B1 · 10 exams",
                "b1_unlimited",
                "B1 · Unlimited",
                "tester",
                "Tester access")
            .get(code));
    p.setQuota(code.equals("free") ? 1 : code.equals("b1") ? 10 : code.equals("tester") ? 2 : 0);
    p.setAccessDays(code.equals("free") ? null : code.equals("tester") ? 7 : 60);
    p.setPriceId(
        code.equals("b1")
            ? env.getProperty("smartpruefung.stripe.priceidb1")
            : code.equals("b1_unlimited")
                ? env.getProperty("smartpruefung.stripe.priceidb1unlimited")
                : null);
    return p;
  }

  @Transactional
  public void seed() {
    for (String code : CODES)
      if (repository.findById(code).orElse(null) == null) repository.save(get(code));
  }
}
