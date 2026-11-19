package com.smartpruefung.backend.Admin.Console.Service;

import com.smartpruefung.backend.Admin.Admin;
import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Repository.AdminsRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {
  @PersistenceContext private EntityManager em;
  private final AdminsRepository admins;
  private final AdminSessionRepository sessions;
  private final AdminCredentialRepository credentials;
  private final SecureRandom random = new SecureRandom();
  private final ConcurrentHashMap<String, List<Instant>> attempts = new ConcurrentHashMap<>();
  private static final String DUMMY = AdminPasswords.encode("TimingDefense-Only-493!");

  public AdminAuthService(
      AdminsRepository admins,
      AdminSessionRepository sessions,
      AdminCredentialRepository credentials) {
    this.admins = admins;
    this.sessions = sessions;
    this.credentials = credentials;
  }

  public static String hash(String token) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private synchronized void rateLimit(String key) {
    Instant cutoff = Instant.now().minusSeconds(900);
    attempts.entrySet().removeIf(e -> e.getValue().stream().noneMatch(t -> t.isAfter(cutoff)));
    if (attempts.size() > 10000 && !attempts.containsKey(key))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
    var list = attempts.computeIfAbsent(key, k -> new ArrayList<>());
    list.removeIf(t -> t.isBefore(cutoff));
    if (list.size() >= (key.startsWith("ip:") ? 80 : 8))
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Retry after 15 minutes");
    list.add(Instant.now());
  }

  @Transactional
  public Map<String, Object> login(AdminRequests.Login input, String address) {
    String email = input.getEmail().trim().toLowerCase(Locale.ROOT);
    rateLimit("ip:" + address);
    rateLimit("email:" + email);
    var found =
        em.createQuery("select a from Admin a where lower(a.email)=:email", Admin.class)
            .setParameter("email", email)
            .setMaxResults(1)
            .getResultList();
    Admin a = found.isEmpty() ? null : found.get(0);
    AdminCredential c = a == null ? null : credentials.findById(a.getId()).orElse(null);
    if (c == null
        || !c.isEnabled()
        || !AdminPasswords.matches(input.getPassword(), c.getPasswordHash())) {
      AdminPasswords.matches(input.getPassword(), DUMMY);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid dashboard credentials");
    }
    if (a.getRoles().stream()
        .noneMatch(r -> Set.of("Admin", "SuperAdmin", "Viewer").contains(r.getName())))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No dashboard role assigned");
    attempts.remove("email:" + email);
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = "spadm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    AdminSession s = new AdminSession();
    s.setTokenHash(hash(token));
    s.setAdmin(a);
    s.setCreatedAt(Instant.now());
    s.setExpiresAt(Instant.now().plusSeconds(3600));
    sessions.save(s);
    sessions.deleteExpired(Instant.now());
    var result = identity(a);
    result.put("token", token);
    result.put("expiresAt", s.getExpiresAt());
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> authenticate(String token) {
    AdminSession s = sessions.findById(hash(token)).orElse(null);
    if (s == null || s.getExpiresAt().isBefore(Instant.now())) return null;
    Admin a = s.getAdmin();
    AdminCredential c = credentials.findById(a.getId()).orElse(null);
    if (c == null
        || !c.isEnabled()
        || (c.getValidAfter() != null && !s.getCreatedAt().isAfter(c.getValidAfter()))) return null;
    return identity(a);
  }

  private Map<String, Object> identity(Admin a) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", a.getId());
    result.put("email", a.getEmail());
    result.put("name", a.getFirstname() + " " + a.getLastname());
    result.put(
        "roles",
        a.getRoles().stream()
            .map(r -> r.getName())
            .sorted()
            .collect(java.util.stream.Collectors.toList()));
    return result;
  }

  @Transactional
  public void logout(String token) {
    sessions.findById(hash(token)).ifPresent(sessions::delete);
  }
}
