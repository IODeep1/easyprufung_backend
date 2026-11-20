package com.smartpruefung.backend.Admin.Console.DTO;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import com.smartpruefung.backend.exam.domain.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;

public final class AdminRequests {
  @Data
  public static class Login {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 128)
    private String password;
  }

  @Data
  public static class UserInput {
    @Min(0)
    private long version;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String firstname;

    @NotBlank
    @Size(max = 100)
    private String lastname;

    @Size(max = 72)
    private String password;

    @NotBlank private String preferredLanguage = "en";
    @NotNull private Boolean enabled = true;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class AccessInput {
    @Min(0)
    private long version;

    @NotBlank private String plan;

    @NotBlank
    @Pattern(regexp = "active|expired|suspended")
    private String status;

    @Min(0)
    @Max(1000000)
    private int quota;

    private Timestamp startDate;
    private Timestamp endDate;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class PlanInput {
    @NotBlank
    @Size(max = 120)
    private String title;

    @Min(0)
    @Max(1000000)
    private int quota;

    @Min(1)
    @Max(3650)
    private Integer accessDays;

    @Size(max = 255)
    private String priceId;

    private boolean enabled = true;

    @Min(0)
    private long version;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class AdminInput {
    @Min(0)
    private long version;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String firstname;

    @NotBlank
    @Size(max = 100)
    private String lastname;

    @Size(max = 72)
    private String password;

    @NotNull private Boolean enabled = true;
    @NotEmpty private Set<String> roles;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class PromoInput {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{4,100}")
    private String code;

    @NotBlank private String plan = "tester";
    private boolean active = true;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class ExamInput {
    @NotBlank
    @Pattern(regexp = "[A-Z0-9_]{3,80}")
    private String code;

    @NotNull private ExamProvider provider;
    @NotNull private CefrLevel level;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Min(1)
    private int definitionVersion;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal maximumScore;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal passPercentage;

    @NotBlank private String scoreRounding = "HALF_UP";
    @NotEmpty private List<@Valid SectionInput> sections;

    @NotBlank
    @Size(max = 1000)
    private String reason;
  }

  @Data
  public static class SectionInput {
    @NotBlank
    @Size(max = 60)
    private String sectionKey;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Min(0)
    private int orderIndex;

    @Min(1)
    private Integer durationSeconds;

    @Size(max = 60)
    private String timingGroup;

    @NotEmpty private List<@Valid PartInput> parts;
  }

  @Data
  public static class PartInput {
    @NotBlank
    @Size(max = 60)
    private String partKey;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Min(0)
    private int orderIndex;

    @NotNull private ContentSource contentSource;
    @NotNull private EvaluationMode evaluationMode;
    @NotNull private QuestionType questionType;

    @Min(1)
    private int firstQuestionNumber;

    @Min(1)
    @Max(1000)
    private int questionCount;

    @NotNull
    @DecimalMin("0")
    private BigDecimal pointsPerQuestion;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal maximumScore;

    @Min(1)
    private Integer durationSeconds;

    @Min(0)
    private Integer minimumWords;

    @Min(1)
    private Integer maximumWords;

    @Size(max = 500)
    private String answerPattern;

    @Size(max = 100000)
    private String generationInstructions;

    @Size(max = 100000)
    private String evaluationRubricJson;
  }
}
