package com.smartpruefung.backend.Admin.Console.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpruefung.backend.Admin.Console.DTO.AdminRequests;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.PromoCode.PromoCode;
import com.smartpruefung.backend.User.User;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromoRedemptionService {
  @PersistenceContext private EntityManager em;
  private final PlanCatalog plans;
  private final AdminManagementService management;
  private final ObjectMapper json;

  public AdminPromoRedemptionService(
      PlanCatalog plans, AdminManagementService management, ObjectMapper json) {
    this.plans = plans;
    this.management = management;
    this.json = json;
  }

  // The existing client sends JSON.stringify(code), while some integrations send a plain body.
  public String normalizeCode(String raw) {
    if (raw == null) return "";
    String code = raw.trim();
    if (code.startsWith("\"")) {
      try {
        var node = json.readTree(code);
        if (!node.isTextual()) return "";
        code = node.asText().trim();
      } catch (Exception invalid) {
        return "";
      }
    }
    return code.length() <= 100 ? code.toUpperCase(Locale.ROOT) : "";
  }

  @Transactional
  public boolean redeem(String code, long userId) {
    code = normalizeCode(code);
    if (!code.matches("[A-Z0-9_-]{4,100}")) return false;
    // Serialize claims for this customer, including concurrent different-code claims.
    User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
    if (user == null) return false;
    em.refresh(user);
    AdminUserState state = em.find(AdminUserState.class, userId);
    if (state != null && !state.isEnabled()) return false;
    var found =
        em.createQuery(
                "select p from PromoCode p where upper(p.code)=:code and p.type='admin'",
                PromoCode.class)
            .setParameter("code", code)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();
    if (found.size() != 1) return false;
    PromoCode promo = found.get(0);
    em.refresh(promo);
    if (!Boolean.TRUE.equals(promo.getIsActive()) || !"active".equals(promo.getStatus()))
      return false;
    if (em.createQuery(
                "select count(r) from AdminPromoRedemption r where r.code=:code and"
                    + " r.userUuid=:user",
                Long.class)
            .setParameter("code", code)
            .setParameter("user", user.getUuid())
            .getSingleResult()
        > 0) return false;
    PlanSettings plan = plans.get(promo.getPlan());
    if (!plan.isEnabled()) return false;
    long version = 0;
    if (user.getSubscription() != null) {
      em.lock(user.getSubscription(), LockModeType.PESSIMISTIC_WRITE);
      em.refresh(user.getSubscription());
      AdminSubscriptionState subscriptionState =
          em.find(AdminSubscriptionState.class, user.getSubscription().getId());
      if (subscriptionState != null) {
        em.refresh(subscriptionState);
        version = subscriptionState.getVersion();
      }
    }
    // saveAccess validates against a millisecond-resolution Timestamp.
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    AdminRequests.AccessInput grant = new AdminRequests.AccessInput();
    grant.setPlan(plan.getCode());
    grant.setStatus("active");
    grant.setQuota(plan.getQuota());
    grant.setVersion(version);
    grant.setStartDate(Timestamp.from(now));
    grant.setEndDate(
        plan.getAccessDays() == null
            ? null
            : Timestamp.from(now.plus(plan.getAccessDays(), ChronoUnit.DAYS)));
    grant.setReason("Customer redeemed promo " + code);
    management.saveAccess(userId, grant);
    AdminPromoRedemption claim = new AdminPromoRedemption();
    claim.setId(UUID.randomUUID().toString());
    claim.setCode(code);
    claim.setUserUuid(user.getUuid());
    claim.setPromoId(promo.getId());
    claim.setPlan(plan.getCode());
    claim.setRedeemedAt(now);
    em.persist(claim);
    em.flush();
    return true;
  }
}
