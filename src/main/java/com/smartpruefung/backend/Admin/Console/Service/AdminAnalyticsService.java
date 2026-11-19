package com.smartpruefung.backend.Admin.Console.Service;

import static com.smartpruefung.backend.Admin.Console.Service.AdminManagementService.row;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsService {
  @PersistenceContext private EntityManager em;

  private long count(String q) {
    return em.createQuery(q, Long.class).getSingleResult();
  }

  private long since(String q, Object cutoff) {
    return em.createQuery(q, Long.class).setParameter("cutoff", cutoff).getSingleResult();
  }

  private List<Map<String, Object>> groups(String q) {
    List<Map<String, Object>> output = new ArrayList<>();
    for (Object[] r : em.createQuery(q, Object[].class).getResultList())
      output.add(row("label", r[0] == null ? "unknown" : r[0].toString(), "value", r[1]));
    return output;
  }

  public Map<String, Object> dashboard(int days) {
    if (!Set.of(7, 30, 90).contains(days))
      throw new IllegalArgumentException("Choose a 7, 30 or 90 day window");
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    Instant start = today.minusDays(days - 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Timestamp cutoff = Timestamp.from(start), now = Timestamp.from(Instant.now());
    long totalSessions = count("select count(s) from ExamSession s");
    long completed = count("select count(r) from ExamResult r");
    long passed = count("select count(r) from ExamResult r where r.passed=true");
    long activeSubscriptions =
        em.createQuery(
                "select count(s) from Subscription s where s.isActive=true and (s.endDate is null"
                    + " or s.endDate>=:now)",
                Long.class)
            .setParameter("now", now)
            .getSingleResult();
    long expired =
        em.createQuery(
                "select count(s) from Subscription s where s.status='expired' or s.endDate<:now",
                Long.class)
            .setParameter("now", now)
            .getSingleResult();
    Number average =
        em.createQuery("select avg(r.percentage) from ExamResult r", Number.class)
            .getSingleResult();
    Map<String, Map<String, Object>> trend = new LinkedHashMap<>();
    for (int i = days - 1; i >= 0; i--) {
      String date = today.minusDays(i).toString();
      trend.put(date, row("date", date, "registrations", 0L, "sessions", 0L, "completed", 0L));
    }
    @SuppressWarnings("unchecked")
    List<Object[]> users =
        em.createNativeQuery(
                "select date(created_date),count(*) from users where created_date>=:cutoff group by"
                    + " date(created_date)")
            .setParameter("cutoff", cutoff)
            .getResultList();
    @SuppressWarnings("unchecked")
    List<Object[]> sessions =
        em.createNativeQuery(
                "select date(created_at),count(*) from exam_sessions where created_at>=:cutoff"
                    + " group by date(created_at)")
            .setParameter("cutoff", cutoff)
            .getResultList();
    @SuppressWarnings("unchecked")
    List<Object[]> results =
        em.createNativeQuery(
                "select date(evaluated_at),count(*) from exam_results where evaluated_at>=:cutoff"
                    + " group by date(evaluated_at)")
            .setParameter("cutoff", cutoff)
            .getResultList();
    for (Object[] a : users)
      if (trend.containsKey(a[0].toString())) trend.get(a[0].toString()).put("registrations", a[1]);
    for (Object[] a : sessions)
      if (trend.containsKey(a[0].toString())) trend.get(a[0].toString()).put("sessions", a[1]);
    for (Object[] a : results)
      if (trend.containsKey(a[0].toString())) trend.get(a[0].toString()).put("completed", a[1]);
    return row(
        "generatedAt",
        Instant.now(),
        "days",
        days,
        "timezone",
        "UTC",
        "totalUsers",
        count("select count(u) from User u"),
        "enabledUsers",
        count(
            "select count(u) from User u where not exists (select t.id from AdminUserState t where"
                + " t.id=u.id and t.enabled=false)"),
        "activeUsers",
        since(
            "select count(distinct s.userId) from ExamSession s where s.createdAt>=:cutoff", start),
        "newUsers",
        since("select count(u) from User u where u.createdDate>=:cutoff", cutoff),
        "totalSubscriptions",
        count("select count(s) from Subscription s"),
        "activeSubscriptions",
        activeSubscriptions,
        "expiredSubscriptions",
        expired,
        "totalExams",
        count("select count(e) from ExamDefinition e"),
        "publishedExams",
        count("select count(e) from ExamDefinition e where e.active=true"),
        "totalSessions",
        totalSessions,
        "windowSessions",
        since("select count(s) from ExamSession s where s.createdAt>=:cutoff", start),
        "completedSessions",
        completed,
        "passRate",
        completed == 0 ? 0.0 : 100.0 * passed / completed,
        "averageScore",
        average == null ? 0 : average,
        "completionRate",
        totalSessions == 0 ? 0.0 : 100.0 * completed / totalSessions,
        "templates",
        count("select count(t) from ExerciseTemplate t"),
        "contacts",
        count("select count(c) from Contact c"),
        "trend",
        new ArrayList<>(trend.values()),
        "plans",
        groups("select s.plan,count(s) from Subscription s group by s.plan"),
        "sessionStatuses",
        groups("select s.status,count(s) from ExamSession s group by s.status"),
        "acquisition",
        groups("select u.acquisitionSource,count(u) from User u group by u.acquisitionSource"));
  }
}
