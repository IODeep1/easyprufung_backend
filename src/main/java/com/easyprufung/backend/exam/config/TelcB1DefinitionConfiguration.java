package com.easyprufung.backend.exam.config;

import com.easyprufung.backend.exam.domain.*;
import com.easyprufung.backend.exam.repository.ExamDefinitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TelcB1DefinitionConfiguration implements ApplicationRunner {
    public static final String CODE = "TELC_DEUTSCH_B1_WRITTEN";
    private final ExamDefinitionRepository repository;

    public TelcB1DefinitionConfiguration(ExamDefinitionRepository repository) { this.repository = repository; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsByCodeAndDefinitionVersion(CODE, 1)) return;
        ExamDefinition exam = new ExamDefinition();
        exam.setCode(CODE);
        exam.setProvider(ExamProvider.TELC);
        exam.setLevel(CefrLevel.B1);
        exam.setTitle("telc Deutsch B1 — Schriftliche Prüfung");
        exam.setDefinitionVersion(1);
        exam.setActive(true);
        exam.setMaximumScore(bd("225"));
        exam.setPassPercentage(bd("60"));
        exam.setScoreRounding("HALF_UP");

        SectionDefinition reading = section("LESEVERSTEHEN", "Leseverstehen", 1, 5400, "LESEN_SPRACHE_90");
        reading.addPart(part("LESEN_1", "Teil 1", 1, ContentSource.AI_GENERATED, EvaluationMode.OBJECTIVE,
                QuestionType.MATCHING, 1, 5, "5", "25",
                "Create five short authentic B1 texts and ten headings (a-j). Exactly one heading matches each text; distractors must be plausible."));
        reading.addPart(part("LESEN_2", "Teil 2", 2, ContentSource.AI_GENERATED, EvaluationMode.OBJECTIVE,
                QuestionType.SINGLE_CHOICE, 6, 5, "5", "25",
                "Create one B1 informational text and five detail-comprehension questions numbered 6-10, each with options a, b and c."));
        reading.addPart(part("LESEN_3", "Teil 3", 3, ContentSource.AI_GENERATED, EvaluationMode.OBJECTIVE,
                QuestionType.MATCHING, 11, 10, "2.5", "25",
                "Create ten situations and twelve short notices labelled a-l. Match each situation to a notice; allow an explicit no-match option where appropriate."));
        exam.addSection(reading);

        SectionDefinition language = section("SPRACHBAUSTEINE", "Sprachbausteine", 2, 5400, "LESEN_SPRACHE_90");
        language.addPart(part("SPRACHE_1", "Teil 1", 1, ContentSource.AI_GENERATED, EvaluationMode.OBJECTIVE,
                QuestionType.CLOZE_CHOICE, 21, 10, "1.5", "15",
                "Create one coherent B1 text with gaps 21-30. Give exactly three grammatical choices a, b and c for every gap."));
        language.addPart(part("SPRACHE_2", "Teil 2", 2, ContentSource.AI_GENERATED, EvaluationMode.OBJECTIVE,
                QuestionType.CLOZE_CHOICE, 31, 10, "1.5", "15",
                "Create one coherent B1 text with gaps 31-40 and a shared word bank a-o. Each key may be used at most once."));
        exam.addSection(language);

        SectionDefinition listening = section("HOERVERSTEHEN", "Hörverstehen", 3, 1800, "HOEREN_30");
        listening.addPart(part("HOEREN_1", "Teil 1", 1, ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE, QuestionType.TRUE_FALSE, 41, 5, "5", "25", null));
        listening.addPart(part("HOEREN_2", "Teil 2", 2, ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE, QuestionType.TRUE_FALSE, 46, 10, "2.5", "25", null));
        listening.addPart(part("HOEREN_3", "Teil 3", 3, ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE, QuestionType.TRUE_FALSE, 56, 5, "5", "25", null));
        exam.addSection(listening);

        SectionDefinition writing = section("SCHRIFTLICHER_AUSDRUCK", "Schriftlicher Ausdruck", 4, 1800, "SCHREIBEN_30");
        PartDefinition writingPart = part("SCHREIBEN_EMAIL", "Informelle oder halbformelle E-Mail", 1,
                ContentSource.AI_GENERATED, EvaluationMode.AI_WRITTEN, QuestionType.FREE_TEXT,
                61, 1, "45", "45",
                "Create a B1 task requiring an informal or semi-formal email. Include a realistic source email or situation and exactly four content points. Return one FREE_TEXT question numbered 61 with empty options and empty accepted answers.");
        writingPart.setMinimumWords(80);
        writingPart.setMaximumWords(180);
        writingPart.setEvaluationRubricJson(
                "{\"scale\":\"TELC_B1_WRITING\",\"maximumScore\":45,\"criteria\":[" +
                        "{\"key\":\"task_achievement\",\"title\":\"Aufgabenbewältigung\",\"maximumScore\":15,\"bands\":[15,9,3,0]}," +
                        "{\"key\":\"communicative_design\",\"title\":\"Kommunikative Gestaltung\",\"maximumScore\":15,\"bands\":[15,9,3,0]}," +
                        "{\"key\":\"formal_correctness\",\"title\":\"Formale Richtigkeit\",\"maximumScore\":15,\"bands\":[15,9,3,0]}" +
                        "],\"rules\":{\"offTopicScore\":0,\"evaluateRegister\":true,\"evaluateAllFourContentPoints\":true}}"
        );
        writing.addPart(writingPart);
        exam.addSection(writing);
        repository.save(exam);
    }

    private SectionDefinition section(String key, String title, int order, int duration, String timingGroup) {
        SectionDefinition section = new SectionDefinition();
        section.setSectionKey(key); section.setTitle(title); section.setOrderIndex(order);
        section.setDurationSeconds(duration); section.setTimingGroup(timingGroup);
        return section;
    }

    private PartDefinition part(String key, String title, int order, ContentSource source,
                                EvaluationMode evaluation, QuestionType type, int first, int count,
                                String pointsEach, String maximum, String instructions) {
        PartDefinition part = new PartDefinition();
        part.setPartKey(key); part.setTitle(title); part.setOrderIndex(order);
        part.setContentSource(source); part.setEvaluationMode(evaluation); part.setQuestionType(type);
        part.setFirstQuestionNumber(first); part.setQuestionCount(count);
        part.setPointsPerQuestion(bd(pointsEach)); part.setMaximumScore(bd(maximum));
        part.setAnswerPattern(type == QuestionType.FREE_TEXT ? null : "^[A-Za-z0-9_+\\-]+$");
        part.setGenerationInstructions(instructions);
        return part;
    }

    private BigDecimal bd(String value) { return new BigDecimal(value); }
}
