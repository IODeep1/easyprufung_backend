package com.smartpruefung.backend.Admin.Console.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpruefung.backend.Admin.Admin;
import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Role;
import com.smartpruefung.backend.Contact.Contact;
import com.smartpruefung.backend.PromoCode.PromoCode;
import com.smartpruefung.backend.Security.AESEncryption;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.User.Service.SubscriptionService;
import com.smartpruefung.backend.User.Service.UserPreferences;
import com.smartpruefung.backend.User.Subscription;
import com.smartpruefung.backend.User.User;
import com.smartpruefung.backend.exam.domain.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminManagementService {
  @PersistenceContext private EntityManager em;

  @org.springframework.beans.factory.annotation.Autowired
  private AdminAuditRepository auditRepository;

  private final UsersRepository users;
  private final SubscriptionService access;
  private final PlanCatalog plans;
  private final ObjectMapper json;

  public AdminManagementService(
      UsersRepository users, SubscriptionService access, PlanCatalog plans, ObjectMapper json) {
    this.users = users;
    this.access = access;
    this.plans = plans;
    this.json = json;
  }

  public static Map<String, Object> row(Object... values) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) m.put(values[i].toString(), values[i + 1]);
    return m;
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  public static void reason(String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 1000)
      throw new IllegalArgumentException("A reason of 1–1000 characters is required");
  }

  private String actor() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  private String encode(Object data) {
    try {
      return json.writeValueAsString(data);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void audit(
      String action, String type, Object id, Object before, Object after, String reason) {
    reason(reason);
    AdminAudit a = new AdminAudit();
    a.setActor(actor());
    a.setAction(action);
    a.setEntityType(type);
    a.setEntityId(String.valueOf(id));
    a.setBeforeJson(encode(before));
    a.setAfterJson(encode(after));
    a.setReason(reason);
    a.setCreatedAt(Instant.now());
    auditRepository.save(a);
  }

  private Timestamp now() {
    return new Timestamp(System.currentTimeMillis());
  }

  private <T> T found(Class<T> type, Object id) {
    T v = em.find(type, id);
    if (v == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    return v;
  }

  public User user(long id) {
    return found(User.class, id);
  }

  public static PageRequest paging(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Page must be >=0 and size 1–100");
    return PageRequest.of(page, size);
  }

  public AdminUserState userState(User user) {
    AdminUserState state = em.find(AdminUserState.class, user.getId());
    if (state == null) {
      state = new AdminUserState();
      state.setId(user.getId());
    }
    return state;
  }

  private AdminUserState touchUser(User user) {
    AdminUserState state =
        em.find(AdminUserState.class, user.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (state == null) {
      state = new AdminUserState();
      state.setId(user.getId());
      em.persist(state);
    }
    state.setVersion(state.getVersion() + 1);
    state.setTouchedAt(Instant.now());
    return state;
  }

  private long accessVersion(Subscription access) {
    AdminSubscriptionState state = em.find(AdminSubscriptionState.class, access.getId());
    return state == null ? 0 : state.getVersion();
  }

  private void touchAccess(Subscription access) {
    AdminSubscriptionState state =
        em.find(AdminSubscriptionState.class, access.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (state == null) {
      state = new AdminSubscriptionState();
      state.setId(access.getId());
      em.persist(state);
    }
    state.setVersion(state.getVersion() + 1);
    state.setTouchedAt(Instant.now());
  }

  private AdminCredential credential(Admin admin) {
    AdminCredential state = em.find(AdminCredential.class, admin.getId());
    if (state == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Administrator has no dashboard credentials");
    return state;
  }

  private String legacyPassword(String password) {
    if (password == null || !com.smartpruefung.backend.Utils.Parser.validatePassword(password))
      throw new IllegalArgumentException(
          "Use at least 8 characters with uppercase, lowercase, a number and a special character");
    return AESEncryption.encrypt(password);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> userView(User u) {
    return row(
        "id",
        u.getId(),
        "uuid",
        u.getUuid(),
        "version",
        userState(u).getVersion(),
        "firstname",
        u.getFirstname(),
        "lastname",
        u.getLastname(),
        "email",
        u.getEmail(),
        "enabled",
        userState(u).isEnabled(),
        "preferredLanguage",
        u.getPreferredLanguage(),
        "source",
        u.getSource(),
        "acquisitionSource",
        u.getAcquisitionSource(),
        "createdDate",
        u.getCreatedDate(),
        "updatedDate",
        u.getUpdatedDate(),
        "subscription",
        u.getSubscription() == null ? null : subscriptionView(u.getSubscription()));
  }

  public Map<String, Object> subscriptionView(Subscription s) {
    return row(
        "id",
        s.getId(),
        "version",
        accessVersion(s),
        "uuid",
        s.getUuid(),
        "customerEmail",
        s.getCustomerEmail(),
        "customerId",
        s.getCustomerId(),
        "plan",
        s.getPlan(),
        "type",
        s.getType(),
        "status",
        effectiveStatus(s),
        "storedStatus",
        s.getStatus(),
        "isActive",
        s.getIsActive(),
        "quota",
        s.getQuota(),
        "priceId",
        s.getPriceId(),
        "startDate",
        s.getStartDate(),
        "endDate",
        s.getEndDate(),
        "createdDate",
        s.getCreatedDate(),
        "updatedDate",
        s.getUpdatedDate());
  }

  private String effectiveStatus(Subscription s) {
    if (s.getEndDate() != null && s.getEndDate().before(now())) return "expired";
    return Boolean.TRUE.equals(s.getIsActive())
        ? "active"
        : "expired".equals(s.getStatus()) ? "expired" : "suspended";
  }

  private String email(String value) {
    String e = value.trim().toLowerCase(Locale.ROOT);
    if (!com.smartpruefung.backend.Utils.Parser.isValidEmailAddress(e))
      throw new IllegalArgumentException("Invalid email");
    return e;
  }

  private <T> Page<T> page(
      String select,
      String from,
      String where,
      Map<String, Object> parameters,
      Class<T> type,
      int page,
      int size) {
    PageRequest pageable = paging(page, size);
    TypedQuery<T> q = em.createQuery(select + from + where + " order by e.id desc", type);
    TypedQuery<Long> count = em.createQuery("select count(e) " + from + where, Long.class);
    parameters.forEach(
        (k, v) -> {
          q.setParameter(k, v);
          count.setParameter(k, v);
        });
    return new PageImpl<>(
        q.setFirstResult(page * size).setMaxResults(size).getResultList(),
        pageable,
        count.getSingleResult());
  }

  private String search(String q) {
    if (q.length() > 200) throw new IllegalArgumentException("Search is too long");
    return "%"
        + q.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> users(String q, String enabled, int page, int size) {
    String where =
        " where (lower(e.email) like :q escape '!' or lower(e.firstname) like :q escape '!' or"
            + " lower(e.lastname) like :q escape '!')";
    if (!enabled.isBlank()) {
      if (!Set.of("true", "false").contains(enabled))
        throw new IllegalArgumentException("Invalid account filter");
      where +=
          enabled.equals("true")
              ? " and not exists (select t.id from AdminUserState t where t.id=e.id and"
                  + " t.enabled=false)"
              : " and exists (select t.id from AdminUserState t where t.id=e.id and"
                  + " t.enabled=false)";
    }
    return page("select e ", "from User e", where, Map.of("q", search(q)), User.class, page, size)
        .map(this::userView);
  }

  public Map<String, Object> saveUser(Long id, AdminRequests.UserInput in) {
    String address = email(in.getEmail());
    User duplicate = users.findByEmail(address);
    require(duplicate == null || (id != null && duplicate.getId() == id), "Email already exists");
    User u = id == null ? new User() : user(id);
    if (id != null) {
      em.lock(u, LockModeType.PESSIMISTIC_WRITE);
      em.refresh(u);
    }
    if (id != null)
      require(userState(u).getVersion() == in.getVersion(), "User changed. Reload before saving");
    Object before = id == null ? null : userView(u);
    if (id == null) {
      if (in.getPassword() == null) throw new IllegalArgumentException("Password is required");
      u.setUuid(UUID.randomUUID().toString());
      u.setCreatedDate(now());
      u.setSource("admin");
      u.setAcquisitionOnboardingCompleted(true);
      u.setSubscription(access.createFreeSubscription(address));
    }
    if (in.getPassword() != null && !in.getPassword().isBlank()) {
      u.setPassword(legacyPassword(in.getPassword()));
      if (id != null)
        touchUser(u)
            .setValidAfter(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      u.setResetPasswordToken(null);
      u.setResetPasswordTokenCreationDate(null);
    }
    if (!address.equals(u.getEmail()) || !Objects.equals(userState(u).isEnabled(), in.getEnabled()))
      if (id != null)
        touchUser(u)
            .setValidAfter(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    u.setEmail(address);
    u.setFirstname(in.getFirstname().trim());
    u.setLastname(in.getLastname().trim());
    u.setPreferredLanguage(UserPreferences.language(in.getPreferredLanguage()));

    u.setUpdatedDate(now());
    // Subscription billing email is independent of the account login email.
    if (id == null) em.persist(u);
    em.flush();
    touchUser(u).setEnabled(in.getEnabled());
    em.flush();
    audit(id == null ? "CREATE" : "UPDATE", "user", u.getId(), before, userView(u), in.getReason());
    return userView(u);
  }

  public void deleteUser(long id, String reason) {
    User u = user(id);
    Object before = userView(u);
    List<ExamSession> sessions =
        em.createQuery("select s from ExamSession s where s.userId=:u", ExamSession.class)
            .setParameter("u", u.getUuid())
            .getResultList();
    for (ExamSession s : sessions) {
      em.createQuery("delete from UserAnswer a where a.examSession=:s")
          .setParameter("s", s)
          .executeUpdate();
      em.createQuery("delete from ExamResult r where r.examSession=:s")
          .setParameter("s", s)
          .executeUpdate();
      em.remove(s);
    }
    em.flush();
    em.remove(u);
    em.flush();
    audit("DELETE", "user", id, before, null, reason);
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> subscriptions(
      String q, String status, String plan, int page, int size) {
    String where = " where lower(e.customerEmail) like :q escape '!'";
    Map<String, Object> p = new HashMap<>();
    p.put("q", search(q));
    if (!plan.isBlank()) {
      where += " and e.plan=:plan";
      p.put("plan", plan);
    }
    if (!status.isBlank()) {
      p.put("now", now());
      switch (status) {
        case "active":
          where += " and e.isActive=true and (e.endDate is null or e.endDate>=:now)";
          break;
        case "expired":
          where += " and (e.status='expired' or e.endDate<:now)";
          break;
        case "suspended":
          where +=
              " and (e.isActive=false or e.isActive is null) and (e.status is null or"
                  + " e.status<>'expired') and (e.endDate is null or e.endDate>=:now)";
          break;
        default:
          throw new IllegalArgumentException("Invalid status");
      }
    }
    return page("select e ", "from Subscription e", where, p, Subscription.class, page, size)
        .map(
            s -> {
              Map<String, Object> m = subscriptionView(s);
              List<Long> owner =
                  em.createQuery("select u.id from User u where u.subscription.id=:id", Long.class)
                      .setParameter("id", s.getId())
                      .setMaxResults(1)
                      .getResultList();
              m.put("userId", owner.isEmpty() ? null : owner.get(0));
              return m;
            });
  }

  public Map<String, Object> saveAccess(long userId, AdminRequests.AccessInput in) {
    User u = user(userId);
    PlanSettings plan = plans.get(in.getPlan());
    require(plan.isEnabled(), "Plan is disabled for new grants");
    Subscription s = u.getSubscription();
    Object before = s == null ? null : subscriptionView(s);
    if (s == null) {
      s = access.createFreeSubscription(u.getEmail());
      u.setSubscription(s);
    } else require(accessVersion(s) == in.getVersion(), "Access changed. Reload before saving");
    if (in.getStartDate() != null
        && in.getEndDate() != null
        && !in.getEndDate().after(in.getStartDate()))
      throw new IllegalArgumentException("End must be after start");
    if ("active".equals(in.getStatus())
        && in.getStartDate() != null
        && in.getStartDate().after(now()))
      throw new IllegalArgumentException("Active access cannot start in the future");
    if ("active".equals(in.getStatus()) && in.getEndDate() != null && in.getEndDate().before(now()))
      throw new IllegalArgumentException("Active access cannot end in the past");
    em.lock(s, LockModeType.PESSIMISTIC_WRITE);
    s.setPlan(in.getPlan());
    s.setType(
        in.getPlan().equals("free")
            ? "free"
            : in.getPlan().equals("tester") ? "tester" : "one_time");
    s.setQuota(in.getPlan().equals("b1_unlimited") ? 0 : in.getQuota());
    s.setStatus(in.getStatus());
    s.setIsActive("active".equals(in.getStatus()));
    s.setStartDate(in.getStartDate());
    s.setEndDate(in.getEndDate());
    s.setUpdatedDate(now());
    em.flush();
    touchAccess(s);
    em.flush();
    audit("UPDATE", "subscription", s.getId(), before, subscriptionView(s), in.getReason());
    return subscriptionView(s);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> history(long id, int page, int size) {
    paging(page, size);
    String key = String.valueOf(id);
    var entries =
        em.createQuery(
                "select e from AdminAudit e where e.entityType='subscription' and e.entityId=:id"
                    + " order by e.id desc",
                AdminAudit.class)
            .setParameter("id", key)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
    long count =
        em.createQuery(
                "select count(e) from AdminAudit e where e.entityType='subscription' and"
                    + " e.entityId=:id",
                Long.class)
            .setParameter("id", key)
            .getSingleResult();
    List<Object> content = new ArrayList<>();
    for (var entry : entries) {
      try {
        content.add(
            row(
                "revision",
                entry.getId(),
                "at",
                entry.getCreatedAt(),
                "operation",
                entry.getAction(),
                "snapshot",
                json.readValue(entry.getAfterJson(), Map.class)));
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
    return row(
        "content",
        content,
        "totalElements",
        count,
        "number",
        page,
        "size",
        size,
        "note",
        "History records dashboard access changes from installation onward; existing"
            + " Stripe/quota/expiry logic is unchanged.");
  }

  @Transactional(readOnly = true)
  public List<PlanSettings> plans() {
    return em.createQuery("select p from PlanSettings p order by p.code", PlanSettings.class)
        .getResultList();
  }

  public PlanSettings savePlan(String code, AdminRequests.PlanInput in) {
    PlanSettings p = found(PlanSettings.class, code);
    require(p.getVersion() == in.getVersion(), "Plan changed. Reload before saving");
    Object before = json.valueToTree(p);
    if (Set.of("b1", "b1_unlimited", "tester").contains(code) && in.getAccessDays() == null)
      throw new IllegalArgumentException("Paid and tester plans need an access duration");
    if (Set.of("b1", "b1_unlimited").contains(code)) {
      if (in.getPriceId() == null || !in.getPriceId().startsWith("price_"))
        throw new IllegalArgumentException("Use an existing Stripe price_ identifier");
      String other = code.equals("b1") ? "b1_unlimited" : "b1";
      require(
          !in.getPriceId().equals(plans.get(other).getPriceId()),
          "Paid tiers must use different Stripe Price IDs");
    }
    p.setTitle(in.getTitle());
    p.setQuota(code.equals("b1_unlimited") ? 0 : in.getQuota());
    p.setAccessDays(in.getAccessDays());
    p.setPriceId(in.getPriceId());
    p.setEnabled(in.isEnabled());
    em.flush();
    audit("UPDATE", "plan", code, before, p, in.getReason());
    return p;
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> sessions(String q, String status, int page, int size) {
    String where =
        " where (lower(e.userId) like :q escape '!' or lower(e.examDefinition.title) like :q escape"
            + " '!' or exists (select u.id from User u where u.uuid=e.userId and lower(u.email)"
            + " like :q escape '!'))";
    Map<String, Object> p = new HashMap<>();
    p.put("q", search(q));
    if (!status.isBlank()) {
      where += " and e.status=:status";
      p.put("status", SessionStatus.valueOf(status));
    }
    return page("select e ", "from ExamSession e", where, p, ExamSession.class, page, size)
        .map(
            s -> {
              List<String> email =
                  em.createQuery("select u.email from User u where u.uuid=:uuid", String.class)
                      .setParameter("uuid", s.getUserId())
                      .setMaxResults(1)
                      .getResultList();
              List<ExamResult> result =
                  em.createQuery(
                          "select r from ExamResult r where r.examSession=:s", ExamResult.class)
                      .setParameter("s", s)
                      .getResultList();
              return row(
                  "id",
                  s.getId(),
                  "userId",
                  s.getUserId(),
                  "email",
                  email.isEmpty() ? null : email.get(0),
                  "exam",
                  s.getExamDefinition().getTitle(),
                  "status",
                  s.getStatus(),
                  "createdAt",
                  s.getCreatedAt(),
                  "submittedAt",
                  s.getSubmittedAt(),
                  "percentage",
                  result.isEmpty() ? null : result.get(0).getPercentage(),
                  "passed",
                  result.isEmpty() ? null : result.get(0).isPassed());
            });
  }

  public Map<String, Object> adminView(Admin a) {
    return row(
        "id",
        a.getId(),
        "version",
        credential(a).getVersion(),
        "firstname",
        a.getFirstname(),
        "lastname",
        a.getLastname(),
        "email",
        a.getEmail(),
        "enabled",
        credential(a).isEnabled(),
        "roles",
        a.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList()),
        "createdDate",
        a.getCreatedDate());
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> admins(int page, int size) {
    return page(
            "select e ",
            "from Admin e",
            " where exists (select c.id from AdminCredential c where c.id=e.id)",
            Map.of(),
            Admin.class,
            page,
            size)
        .map(this::adminView);
  }

  public Map<String, Object> saveAdmin(Long id, AdminRequests.AdminInput in) {
    String address = email(in.getEmail());
    Admin a = id == null ? new Admin() : found(Admin.class, id);
    AdminCredential c = id == null ? new AdminCredential() : credential(a);
    if (id != null)
      require(c.getVersion() == in.getVersion(), "Administrator changed. Reload before saving");
    Object before = id == null ? null : adminView(a);
    if (a.getEmail() != null && a.getEmail().equals(actor()))
      require(
          in.getEnabled() && in.getRoles().contains("SuperAdmin"),
          "You cannot disable yourself or remove your SuperAdmin role");
    require(
        em.createQuery(
                    "select count(a) from Admin a where lower(a.email)=:email and a.id<>:id",
                    Long.class)
                .setParameter("email", address)
                .setParameter("id", id == null ? -1L : id)
                .getSingleResult()
            == 0,
        "Email exists");
    require(Set.of("SuperAdmin", "Admin", "Viewer").containsAll(in.getRoles()), "Unknown role");
    if (id == null) {
      if (in.getPassword() == null || in.getPassword().isBlank())
        throw new IllegalArgumentException("Password is required");
      a.setCreatedDate(now());
      a.setPassword(AESEncryption.encrypt(UUID.randomUUID().toString() + "Aa9!"));
    }
    if (in.getPassword() != null && !in.getPassword().isBlank()) {
      if (in.getPassword().length() < 12)
        throw new IllegalArgumentException("Admin passwords require 12 characters");
      c.setPasswordHash(AdminPasswords.encode(in.getPassword()));
    }
    a.setFirstname(in.getFirstname());
    a.setLastname(in.getLastname());
    a.setEmail(address);
    a.setUpdatedDate(now());
    a.getRoles().clear();
    for (String role : in.getRoles())
      a.addRole(
          em.createQuery("select r from Role r where r.name=:name", Role.class)
              .setParameter("name", role)
              .getSingleResult());
    if (id == null) {
      em.persist(a);
      em.flush();
      c.setId(a.getId());
      em.persist(c);
    }
    c.setEnabled(in.getEnabled());
    c.setVersion(c.getVersion() + 1);
    c.setValidAfter(Instant.now());
    c.setTouchedAt(Instant.now());
    em.flush();
    audit(
        id == null ? "CREATE" : "UPDATE", "admin", a.getId(), before, adminView(a), in.getReason());
    return adminView(a);
  }

  @Transactional(readOnly = true)
  public Page<Contact> contacts(String q, int page, int size) {
    return page(
        "select e ",
        "from Contact e",
        " where lower(e.email) like :q escape '!' or lower(e.subject) like :q escape '!'",
        Map.of("q", search(q)),
        Contact.class,
        page,
        size);
  }

  public void deleteContact(long id, String reason) {
    Contact c = found(Contact.class, id);
    audit("DELETE", "contact", id, c, null, reason);
    em.remove(c);
  }

  @Transactional(readOnly = true)
  public Page<PromoCode> promos(String q, int page, int size) {
    return page(
        "select e ",
        "from PromoCode e",
        " where lower(e.code) like :q escape '!'",
        Map.of("q", search(q)),
        PromoCode.class,
        page,
        size);
  }

  public PromoCode savePromo(Long id, AdminRequests.PromoInput in) {
    plans.get(in.getPlan());
    String normalizedCode = in.getCode().trim().toUpperCase(Locale.ROOT);
    require(
        !Set.of("STARTER4NEXT", "AICO2025").contains(normalizedCode),
        "This code is reserved for the existing promotion");
    require(
        em.createQuery(
                    "select count(p) from PromoCode p where lower(p.code)=:code and p.id<>:id",
                    Long.class)
                .setParameter("code", normalizedCode.toLowerCase(Locale.ROOT))
                .setParameter("id", id == null ? -1L : id)
                .getSingleResult()
            == 0,
        "Code already exists");
    PromoCode p = id == null ? new PromoCode() : found(PromoCode.class, id);
    Object before = id == null ? null : json.valueToTree(p);
    if (id != null) {
      em.lock(p, LockModeType.PESSIMISTIC_WRITE);
      em.refresh(p);
      require("admin".equals(p.getType()), "Legacy promo records are read-only");
      if (!normalizedCode.equalsIgnoreCase(p.getCode())) {
        require(
            em.createQuery(
                        "select count(r) from AdminPromoRedemption r where r.code=:code",
                        Long.class)
                    .setParameter("code", p.getCode().toUpperCase(Locale.ROOT))
                    .getSingleResult()
                == 0,
            "Redeemed codes cannot be renamed; create another code");
      }
    }
    p.setCode(normalizedCode);
    p.setPlan(in.getPlan());
    p.setIsActive(in.isActive());
    p.setStatus(in.isActive() ? "active" : "disabled");
    p.setType("admin");
    p.setUpdatedDate(now());
    if (id == null) {
      p.setCreatedDate(now());
      em.persist(p);
    }
    em.flush();
    audit(id == null ? "CREATE" : "UPDATE", "promo", p.getId(), before, p, in.getReason());
    return p;
  }

  public void deletePromo(long id, String reason) {
    PromoCode p = found(PromoCode.class, id);
    em.lock(p, LockModeType.PESSIMISTIC_WRITE);
    em.refresh(p);
    audit("DELETE", "promo", id, p, null, reason);
    em.remove(p);
  }

  @Transactional(readOnly = true)
  public Page<AdminAudit> audits(String q, int page, int size) {
    return page(
        "select e ",
        "from AdminAudit e",
        " where lower(e.actor) like :q escape '!' or lower(e.entityType) like :q escape '!' or"
            + " lower(e.entityId) like :q escape '!'",
        Map.of("q", search(q)),
        AdminAudit.class,
        page,
        size);
  }
}
