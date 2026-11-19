package com.smartpruefung.backend.Admin.Console.Service;

import static com.smartpruefung.backend.Admin.Console.Service.AdminManagementService.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.service.*;
import java.math.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminExamService {
  @PersistenceContext private EntityManager em;
  private final AdminManagementService admin;
  private final AdminExerciseTemplateService templates;
  private final ObjectMapper json;

  public AdminExamService(
      AdminManagementService admin, AdminExerciseTemplateService templates, ObjectMapper json) {
    this.admin = admin;
    this.templates = templates;
    this.json = json;
  }

  private <T> T get(Class<T> c, UUID id) {
    T v = em.find(c, id);
    if (v == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    return v;
  }

  private ExamDefinition mutable(UUID id) {
    ExamDefinition e = get(ExamDefinition.class, id);
    em.lock(e, LockModeType.PESSIMISTIC_WRITE);
    em.refresh(e);
    return e;
  }

  private long attempts(ExamDefinition e) {
    return em.createQuery(
            "select count(s) from ExamSession s where s.examDefinition=:e", Long.class)
        .setParameter("e", e)
        .getSingleResult();
  }

  private void version(BaseEntity entity, long expected) {
    require(entity.getEntityVersion() == expected, "Record changed. Reload before saving");
  }

  public Map<String, Object> view(ExamDefinition e) {
    List<Object> sections = new ArrayList<>();
    for (SectionDefinition s : e.getSections()) {
      List<Object> parts = new ArrayList<>();
      for (PartDefinition p : s.getParts())
        parts.add(
            row(
                "id",
                p.getId(),
                "partKey",
                p.getPartKey(),
                "title",
                p.getTitle(),
                "orderIndex",
                p.getOrderIndex(),
                "contentSource",
                p.getContentSource(),
                "evaluationMode",
                p.getEvaluationMode(),
                "questionType",
                p.getQuestionType(),
                "firstQuestionNumber",
                p.getFirstQuestionNumber(),
                "questionCount",
                p.getQuestionCount(),
                "pointsPerQuestion",
                p.getPointsPerQuestion(),
                "maximumScore",
                p.getMaximumScore(),
                "durationSeconds",
                p.getDurationSeconds(),
                "minimumWords",
                p.getMinimumWords(),
                "maximumWords",
                p.getMaximumWords(),
                "answerPattern",
                p.getAnswerPattern(),
                "generationInstructions",
                p.getGenerationInstructions(),
                "evaluationRubricJson",
                p.getEvaluationRubricJson()));
      sections.add(
          row(
              "id",
              s.getId(),
              "sectionKey",
              s.getSectionKey(),
              "title",
              s.getTitle(),
              "orderIndex",
              s.getOrderIndex(),
              "durationSeconds",
              s.getDurationSeconds(),
              "timingGroup",
              s.getTimingGroup(),
              "parts",
              parts));
    }
    return row(
        "id",
        e.getId(),
        "entityVersion",
        e.getEntityVersion(),
        "code",
        e.getCode(),
        "provider",
        e.getProvider(),
        "level",
        e.getLevel(),
        "title",
        e.getTitle(),
        "definitionVersion",
        e.getDefinitionVersion(),
        "active",
        e.isActive(),
        "maximumScore",
        e.getMaximumScore(),
        "passPercentage",
        e.getPassPercentage(),
        "scoreRounding",
        e.getScoreRounding(),
        "sections",
        sections,
        "attempts",
        attempts(e));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(UUID id) {
    return view(get(ExamDefinition.class, id));
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> list(String q, String active, int page, int size) {
    PageRequest p = paging(page, size);
    String filter = " where (lower(e.title) like :q or lower(e.code) like :q)";
    if (!active.isBlank()) {
      if (!Set.of("true", "false").contains(active))
        throw new IllegalArgumentException("Invalid publish filter");
      filter += " and e.active=" + active;
    }
    String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
    var rows =
        em.createQuery(
                "select e from ExamDefinition e"
                    + filter
                    + " order by e.code,e.definitionVersion desc",
                ExamDefinition.class)
            .setParameter("q", pattern)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
    long total =
        em.createQuery("select count(e) from ExamDefinition e" + filter, Long.class)
            .setParameter("q", pattern)
            .getSingleResult();
    return new PageImpl<>(
        rows.stream().map(this::view).collect(java.util.stream.Collectors.toList()), p, total);
  }

  private void validate(AdminRequests.ExamInput in) {
    RoundingMode.valueOf(in.getScoreRounding());
    Set<String> sections = new HashSet<>(), parts = new HashSet<>();
    Set<Integer> numbers = new HashSet<>();
    BigDecimal score = BigDecimal.ZERO;
    Set<Integer> sectionOrder = new HashSet<>();
    for (var s : in.getSections()) {
      if (!sections.add(s.getSectionKey()) || !sectionOrder.add(s.getOrderIndex()))
        throw new IllegalArgumentException("Section keys and order must be unique");
      Set<Integer> partOrder = new HashSet<>();
      for (var p : s.getParts()) {
        if (!parts.add(p.getPartKey()) || !partOrder.add(p.getOrderIndex()))
          throw new IllegalArgumentException("Part keys and per-section order must be unique");
        for (int n = p.getFirstQuestionNumber();
            n < p.getFirstQuestionNumber() + p.getQuestionCount();
            n++)
          if (!numbers.add(n)) throw new IllegalArgumentException("Question numbers overlap");
        if (p.getMinimumWords() != null
            && p.getMaximumWords() != null
            && p.getMinimumWords() > p.getMaximumWords())
          throw new IllegalArgumentException("Invalid word bounds");
        if (p.getAnswerPattern() != null) java.util.regex.Pattern.compile(p.getAnswerPattern());
        if (p.getEvaluationRubricJson() != null && !p.getEvaluationRubricJson().isBlank())
          try {
            json.readTree(p.getEvaluationRubricJson());
          } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid rubric JSON");
          }
        if (p.getPointsPerQuestion()
                .multiply(BigDecimal.valueOf(p.getQuestionCount()))
                .compareTo(p.getMaximumScore())
            != 0)
          throw new IllegalArgumentException(
              "Points per question times question count must equal the part maximum");
        if ((p.getQuestionType() == QuestionType.FREE_TEXT)
            != (p.getEvaluationMode() == EvaluationMode.AI_WRITTEN))
          throw new IllegalArgumentException(
              "FREE_TEXT and AI_WRITTEN must be configured together");
        if ((p.getContentSource() == ContentSource.AUDIO_PREDEFINED)
            != (p.getEvaluationMode() == EvaluationMode.AUDIO_OBJECTIVE))
          throw new IllegalArgumentException(
              "Audio predefined content requires AUDIO_OBJECTIVE evaluation");
        score = score.add(p.getMaximumScore());
      }
    }
    if (score.compareTo(in.getMaximumScore()) != 0)
      throw new IllegalArgumentException("Part maximum scores must sum to the exam maximum score");
  }

  public Map<String, Object> save(UUID id, long expected, AdminRequests.ExamInput in) {
    validate(in);
    ExamDefinition e = id == null ? new ExamDefinition() : mutable(id);
    Object before = id == null ? null : view(e);
    if (id != null) {
      version(e, expected);
      require(!e.isActive(), "Unpublish this draft before editing");
      require(attempts(e) == 0, "This version has attempts. Clone a new version before editing");
    }
    require(
        em.createQuery(
                    "select count(e) from ExamDefinition e where e.code=:c and"
                        + " e.definitionVersion=:v and (:id is null or e.id<>:id)",
                    Long.class)
                .setParameter("c", in.getCode())
                .setParameter("v", in.getDefinitionVersion())
                .setParameter("id", id)
                .getSingleResult()
            == 0,
        "Code and version already exist");
    BeanUtils.copyProperties(in, e, "sections", "reason");
    e.setActive(false);
    // Reuse sections and parts by key so existing templates stay attached.
    Map<String, SectionDefinition> oldSections = new HashMap<>();
    for (var s : e.getSections()) oldSections.put(s.getSectionKey(), s);
    Set<String> keepSections = new HashSet<>();
    for (var si : in.getSections()) {
      keepSections.add(si.getSectionKey());
      SectionDefinition s = oldSections.get(si.getSectionKey());
      if (s == null) {
        s = new SectionDefinition();
        e.addSection(s);
      }
      BeanUtils.copyProperties(si, s, "parts");
      Map<String, PartDefinition> oldParts = new HashMap<>();
      for (var p : s.getParts()) oldParts.put(p.getPartKey(), p);
      Set<String> keepParts = new HashSet<>();
      for (var pi : si.getParts()) {
        keepParts.add(pi.getPartKey());
        PartDefinition p = oldParts.get(pi.getPartKey());
        if (p != null && templateCount(p) > 0)
          require(
              p.getQuestionCount() == pi.getQuestionCount()
                  && p.getQuestionType() == pi.getQuestionType()
                  && p.getContentSource() == pi.getContentSource()
                  && p.getMaximumScore().compareTo(pi.getMaximumScore()) == 0,
              "Remove templates before changing their part's content type, count or score");
        if (p == null) {
          p = new PartDefinition();
          s.addPart(p);
        }
        BeanUtils.copyProperties(pi, p);
      }
      for (var p : new ArrayList<>(s.getParts()))
        if (!keepParts.contains(p.getPartKey())) {
          require(templateCount(p) == 0, "Remove templates before removing a part");
          s.getParts().remove(p);
        }
    }
    for (var s : new ArrayList<>(e.getSections()))
      if (!keepSections.contains(s.getSectionKey())) {
        for (var p : s.getParts())
          require(templateCount(p) == 0, "Remove templates before removing a section");
        e.getSections().remove(s);
      }
    if (id == null) em.persist(e);
    em.flush();
    audit("SAVE", e, before, in.getReason());
    return view(e);
  }

  private long templateCount(PartDefinition p) {
    return em.createQuery(
            "select count(t) from ExerciseTemplate t where t.partDefinition=:p", Long.class)
        .setParameter("p", p)
        .getSingleResult();
  }

  private void audit(String action, ExamDefinition e, Object before, String reason) {
    admin.audit(action, "exam", e.getId(), before, view(e), reason);
  }

  public Map<String, Object> publish(UUID id, long expected, boolean active, String reason) {
    ExamDefinition e = get(ExamDefinition.class, id);
    for (var locked :
        em.createQuery(
                "select d from ExamDefinition d where d.code=:code order by d.id",
                ExamDefinition.class)
            .setParameter("code", e.getCode())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList()) em.refresh(locked);
    version(e, expected);
    Object before = view(e);
    if (active) {
      require(!e.getSections().isEmpty(), "An exam needs sections");
      for (var s : e.getSections())
        for (var p : s.getParts()) {
          if (p.getContentSource() == ContentSource.AI_GENERATED) {
            require(
                p.getGenerationInstructions() != null && !p.getGenerationInstructions().isBlank(),
                "AI parts require generation instructions");
            if (p.getEvaluationMode() == EvaluationMode.AI_WRITTEN)
              require(
                  p.getEvaluationRubricJson() != null && !p.getEvaluationRubricJson().isBlank(),
                  "AI writing requires a rubric");
          } else
            require(
                em.createQuery(
                            "select count(t) from ExerciseTemplate t where t.partDefinition=:p and"
                                + " t.active=true",
                            Long.class)
                        .setParameter("p", p)
                        .getSingleResult()
                    > 0,
                "Every predefined part needs an active template");
        }
      // Only one live version per provider/level/code; preserve other unrelated exams.
      for (ExamDefinition previous :
          em.createQuery(
                  "select e from ExamDefinition e where e.code=:code and e.provider=:provider and"
                      + " e.level=:level and e.active=true and e.id<>:id",
                  ExamDefinition.class)
              .setParameter("code", e.getCode())
              .setParameter("provider", e.getProvider())
              .setParameter("level", e.getLevel())
              .setParameter("id", e.getId())
              .getResultList()) {
        Object old = view(previous);
        previous.setActive(false);
        audit("UNPUBLISH", previous, old, reason);
      }
    }
    e.setActive(active);
    em.flush();
    audit(active ? "PUBLISH" : "UNPUBLISH", e, before, reason);
    return view(e);
  }

  public void delete(UUID id, long expected, String reason) {
    ExamDefinition e = mutable(id);
    version(e, expected);
    require(!e.isActive(), "Unpublish before deletion");
    require(
        attempts(e) == 0,
        "An exam with attempts cannot be deleted. Unpublish it to preserve history");
    Object before = view(e);
    for (var s : e.getSections())
      for (var p : s.getParts())
        for (var t :
            em.createQuery(
                    "select t from ExerciseTemplate t where t.partDefinition=:p",
                    ExerciseTemplate.class)
                .setParameter("p", p)
                .getResultList()) em.remove(t);
    em.flush();
    em.remove(e);
    em.flush();
    admin.audit("DELETE", "exam", id, before, null, reason);
  }

  public Map<String, Object> clone(UUID id, String reason) {
    ExamDefinition source = get(ExamDefinition.class, id);
    ExamDefinition e = new ExamDefinition();
    BeanUtils.copyProperties(source, e, "id", "entityVersion", "sections", "active");
    e.setActive(false);
    int next =
        em.createQuery(
                    "select max(e.definitionVersion) from ExamDefinition e where e.code=:code",
                    Integer.class)
                .setParameter("code", source.getCode())
                .getSingleResult()
            + 1;
    e.setDefinitionVersion(next);
    for (var section : source.getSections()) {
      SectionDefinition s = new SectionDefinition();
      BeanUtils.copyProperties(section, s, "id", "entityVersion", "examDefinition", "parts");
      e.addSection(s);
      for (var part : section.getParts()) {
        PartDefinition p = new PartDefinition();
        BeanUtils.copyProperties(part, p, "id", "entityVersion", "sectionDefinition");
        s.addPart(p);
      }
    }
    em.persist(e);
    em.flush();
    for (int a = 0; a < source.getSections().size(); a++)
      for (int b = 0; b < source.getSections().get(a).getParts().size(); b++) {
        PartDefinition old = source.getSections().get(a).getParts().get(b),
            target = e.getSections().get(a).getParts().get(b);
        for (var existing :
            em.createQuery(
                    "select t from ExerciseTemplate t where t.partDefinition=:p",
                    ExerciseTemplate.class)
                .setParameter("p", old)
                .getResultList()) {
          ExerciseTemplate t = new ExerciseTemplate();
          BeanUtils.copyProperties(
              existing, t, "id", "entityVersion", "partDefinition", "questions");
          t.setPartDefinition(target);
          for (var q : existing.getQuestions()) {
            QuestionTemplate nq = new QuestionTemplate();
            BeanUtils.copyProperties(q, nq, "id", "entityVersion", "exerciseTemplate");
            t.addQuestion(nq);
          }
          em.persist(t);
        }
      }
    em.flush();
    audit("CLONE", e, null, reason);
    return view(e);
  }

  public Map<String, Object> templateView(ExerciseTemplate t) {
    List<Object> questions = new ArrayList<>();
    for (var q : t.getQuestions())
      try {
        questions.add(
            row(
                "number",
                q.getExternalNumber(),
                "stimulus",
                q.getStimulus(),
                "prompt",
                q.getPrompt(),
                "type",
                q.getQuestionType(),
                "options",
                json.readTree(q.getOptionsJson()),
                "correctAnswer",
                json.readTree(q.getCorrectAnswerJson()),
                "maximumScore",
                q.getMaximumScore(),
                "explanation",
                q.getExplanation()));
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    return row(
        "id",
        t.getId(),
        "entityVersion",
        t.getEntityVersion(),
        "partId",
        t.getPartDefinition().getId(),
        "examId",
        t.getPartDefinition().getSectionDefinition().getExamDefinition().getId(),
        "exam",
        t.getPartDefinition().getSectionDefinition().getExamDefinition().getTitle(),
        "partKey",
        t.getPartDefinition().getPartKey(),
        "templateKey",
        t.getTemplateKey(),
        "instructions",
        t.getInstructions(),
        "content",
        t.getContent(),
        "audioUrl",
        t.getAudioUrl(),
        "audioContentType",
        t.getAudioContentType(),
        "audioPlayLimit",
        t.getAudioPlayLimit(),
        "active",
        t.isActive(),
        "questions",
        questions);
  }

  @Transactional(readOnly = true)
  public Page<Map<String, Object>> templates(String q, int page, int size) {
    var p = paging(page, size);
    String where = " where lower(t.templateKey) like :q or lower(t.partDefinition.partKey) like :q";
    String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
    List<ExerciseTemplate> rows =
        em.createQuery(
                "select t from ExerciseTemplate t" + where + " order by t.templateKey",
                ExerciseTemplate.class)
            .setParameter("q", pattern)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
    long count =
        em.createQuery("select count(t) from ExerciseTemplate t" + where, Long.class)
            .setParameter("q", pattern)
            .getSingleResult();
    return new PageImpl<>(
        rows.stream().map(this::templateView).collect(java.util.stream.Collectors.toList()),
        p,
        count);
  }

  public Map<String, Object> saveTemplate(
      UUID partId, UUID id, long expected, CreateTemplateRequest input, String reason) {
    PartDefinition part = get(PartDefinition.class, partId);
    require(
        !part.getSectionDefinition().getExamDefinition().isActive(),
        "Unpublish the exam before changing templates");
    Object before = null;
    if (id != null) {
      var t = get(ExerciseTemplate.class, id);
      version(t, expected);
      before = templateView(t);
    }
    UUID saved = templates.saveByPartId(partId, input, id);
    em.flush();
    Map<String, Object> result = templateView(get(ExerciseTemplate.class, saved));
    admin.audit(id == null ? "CREATE" : "UPDATE", "template", saved, before, result, reason);
    return result;
  }

  public void deleteTemplate(UUID id, long expected, String reason) {
    var t = get(ExerciseTemplate.class, id);
    version(t, expected);
    require(
        !t.getPartDefinition().getSectionDefinition().getExamDefinition().isActive(),
        "Unpublish the exam before deleting templates");
    Object before = templateView(t);
    em.remove(t);
    admin.audit("DELETE", "template", id, before, null, reason);
  }

  public Map<String, Object> toggleTemplate(UUID id, long expected, boolean active, String reason) {
    var t = get(ExerciseTemplate.class, id);
    version(t, expected);
    require(
        !t.getPartDefinition().getSectionDefinition().getExamDefinition().isActive(),
        "Unpublish the exam before changing templates");
    Object before = templateView(t);
    t.setActive(active);
    em.flush();
    var result = templateView(t);
    admin.audit("UPDATE", "template", id, before, result, reason);
    return result;
  }
}
