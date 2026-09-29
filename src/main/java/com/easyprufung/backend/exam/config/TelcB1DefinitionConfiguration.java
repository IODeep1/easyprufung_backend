package com.easyprufung.backend.exam.config;

import com.easyprufung.backend.exam.domain.CefrLevel;
import com.easyprufung.backend.exam.domain.ContentSource;
import com.easyprufung.backend.exam.domain.EvaluationMode;
import com.easyprufung.backend.exam.domain.ExamDefinition;
import com.easyprufung.backend.exam.domain.ExamProvider;
import com.easyprufung.backend.exam.domain.PartDefinition;
import com.easyprufung.backend.exam.domain.QuestionType;
import com.easyprufung.backend.exam.domain.SectionDefinition;
import com.easyprufung.backend.exam.repository.ExamDefinitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TelcB1DefinitionConfiguration implements ApplicationRunner {

    public static final String CODE =
            "TELC_DEUTSCH_B1_WRITTEN";

    /*
     * Increase this whenever the exam definition or generation
     * instructions change.
     */
    private static final int DEFINITION_VERSION = 1;

    private final ExamDefinitionRepository repository;

    public TelcB1DefinitionConfiguration(
            ExamDefinitionRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsByCodeAndDefinitionVersion(CODE, DEFINITION_VERSION)) return;
        ExamDefinition exam = new ExamDefinition();
        exam.setCode(CODE);
        exam.setProvider(ExamProvider.TELC);
        exam.setLevel(CefrLevel.B1);
        exam.setTitle(
                "telc Deutsch B1 — Schriftliche Prüfung"
        );
        exam.setDefinitionVersion(DEFINITION_VERSION);
        exam.setActive(true);
        exam.setMaximumScore(bd("225"));
        exam.setPassPercentage(bd("60"));
        exam.setScoreRounding("HALF_UP");

        addReadingSection(exam);
        addLanguageSection(exam);
        addListeningSection(exam);
        addWritingSection(exam);

        repository.save(exam);
    }

    private void addReadingSection(ExamDefinition exam) {
        SectionDefinition reading = section(
                "LESEVERSTEHEN",
                "Leseverstehen",
                1,
                5400,
                "LESEN_SPRACHE_90"
        );

        reading.addPart(part(
                "LESEN_1",
                "Teil 1",
                1,
                ContentSource.AI_GENERATED,
                EvaluationMode.OBJECTIVE,
                QuestionType.MATCHING,
                1,
                5,
                "5",
                "25",
                readingPart1Instructions()
        ));

        reading.addPart(part(
                "LESEN_2",
                "Teil 2",
                2,
                ContentSource.AI_GENERATED,
                EvaluationMode.OBJECTIVE,
                QuestionType.SINGLE_CHOICE,
                6,
                5,
                "5",
                "25",
                readingPart2Instructions()
        ));

        reading.addPart(part(
                "LESEN_3",
                "Teil 3",
                3,
                ContentSource.AI_GENERATED,
                EvaluationMode.OBJECTIVE,
                QuestionType.MATCHING,
                11,
                10,
                "2.5",
                "25",
                readingPart3Instructions()
        ));

        exam.addSection(reading);
    }

    private void addLanguageSection(ExamDefinition exam) {
        SectionDefinition language = section(
                "SPRACHBAUSTEINE",
                "Sprachbausteine",
                2,
                5400,
                "LESEN_SPRACHE_90"
        );

        language.addPart(part(
                "SPRACHE_1",
                "Teil 1",
                1,
                ContentSource.AI_GENERATED,
                EvaluationMode.OBJECTIVE,
                QuestionType.CLOZE_CHOICE,
                21,
                10,
                "1.5",
                "15",
                languagePart1Instructions()
        ));

        language.addPart(part(
                "SPRACHE_2",
                "Teil 2",
                2,
                ContentSource.AI_GENERATED,
                EvaluationMode.OBJECTIVE,
                QuestionType.CLOZE_CHOICE,
                31,
                10,
                "1.5",
                "15",
                languagePart2Instructions()
        ));

        exam.addSection(language);
    }

    private void addListeningSection(ExamDefinition exam) {
        SectionDefinition listening = section(
                "HOERVERSTEHEN",
                "Hörverstehen",
                3,
                1800,
                "HOEREN_30"
        );

        listening.addPart(part(
                "HOEREN_1",
                "Teil 1",
                1,
                ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE,
                QuestionType.TRUE_FALSE,
                41,
                5,
                "5",
                "25",
                null
        ));

        listening.addPart(part(
                "HOEREN_2",
                "Teil 2",
                2,
                ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE,
                QuestionType.TRUE_FALSE,
                46,
                10,
                "2.5",
                "25",
                null
        ));

        listening.addPart(part(
                "HOEREN_3",
                "Teil 3",
                3,
                ContentSource.AUDIO_PREDEFINED,
                EvaluationMode.AUDIO_OBJECTIVE,
                QuestionType.TRUE_FALSE,
                56,
                5,
                "5",
                "25",
                null
        ));

        exam.addSection(listening);
    }

    private void addWritingSection(ExamDefinition exam) {
        SectionDefinition writing = section(
                "SCHRIFTLICHER_AUSDRUCK",
                "Schriftlicher Ausdruck",
                4,
                1800,
                "SCHREIBEN_30"
        );

        PartDefinition writingPart = part(
                "SCHREIBEN_EMAIL",
                "Informelle oder halbformelle E-Mail",
                1,
                ContentSource.AI_GENERATED,
                EvaluationMode.AI_WRITTEN,
                QuestionType.FREE_TEXT,
                61,
                1,
                "45",
                "45",
                writingInstructions()
        );

        /*
         * The official evaluation is based on task achievement,
         * communicative design and formal correctness, rather than a
         * strict word-count band.
         */
        writingPart.setMinimumWords(null);
        writingPart.setMaximumWords(null);

        writingPart.setEvaluationRubricJson(
                writingRubricJson()
        );

        writing.addPart(writingPart);
        exam.addSection(writing);
    }

    private String readingPart1Instructions() {
        return """
                Create an original TELC Deutsch B1-style Leseverstehen Teil 1 exercise.

                ASSESSMENT PURPOSE
                Test global reading comprehension. The candidate must identify the main topic
                and communicative focus of five short texts, not isolated words or minor details.

                REQUIRED STRUCTURE
                - Return exactly five MATCHING questions numbered 1 through 5.
                - Return exactly ten shared headings with keys a through j.
                - Every question must contain the same ten heading options in the same order.
                - Put the complete text belonging to each question in that question's stimulus.
                - Set exercise-level content to null.
                - Use prompt only for a short task instruction.
                - Each question must have exactly one acceptedOptionKey.
                - Each correct heading may be used only once.
                - Five headings are correct and five are unused distractors.

                TEXT REQUIREMENTS
                - Write five independent German texts of approximately 80-120 words each.
                - Texts should resemble short newspaper, magazine or public-information texts.
                - Use realistic B1 topics such as work, education, travel, health, housing,
                  family, leisure, mobility, consumer matters or everyday technology.
                - Each text must have one clear main focus but may contain two or three
                  supporting details.
                - Use natural standard German and realistic names, places, dates and prices.
                - Do not require specialist or cultural background knowledge.

                B1 LANGUAGE PROFILE
                - Primarily frequent vocabulary with a limited amount of context-understandable
                  less frequent vocabulary.
                - Use a natural mixture of main and subordinate clauses.
                - Permitted structures include weil, obwohl, wenn, dass, damit, relative
                  clauses, common passive constructions and common past forms.
                - Avoid academic abstraction, specialized terminology and C1-style idioms.

                HEADING AND DISTRACTOR QUALITY
                - Headings should be concise and resemble authentic press headings.
                - Correct headings must paraphrase the overall message.
                - Do not copy a distinctive phrase from the text into the correct heading.
                - Distractors must be plausible and thematically related.
                - At least one distractor for each text should match the general topic but fail
                  because its central claim, target group or purpose is different.
                - No two headings may be equally defensible.

                OUTPUT QUALITY CHECK
                Before returning JSON, silently verify that all five mappings are unique,
                unambiguous and solvable through global comprehension.
                Explanations must be concise German explanations of why the heading matches.
                All material must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String readingPart2Instructions() {
        return """
                Create an original TELC Deutsch B1-style Leseverstehen Teil 2 exercise.

                ASSESSMENT PURPOSE
                Test detailed reading comprehension of one coherent informational text.

                REQUIRED STRUCTURE
                - Put one shared German reading text in exercise-level content.
                - Return exactly five SINGLE_CHOICE questions numbered 6 through 10.
                - Do not duplicate the shared text in question stimuli.
                - Every question must have exactly three options: a, b and c.
                - Every question must have exactly one acceptedOptionKey.
                - Every question has maximumScore 5.

                SOURCE TEXT
                - Write approximately 380-500 words.
                - Use a realistic newspaper, magazine, local-news, consumer-information or
                  general-interest article.
                - Use an everyday B1 topic such as employment, education, social projects,
                  health, mobility, environment, volunteering, housing, family or leisure.
                - The text should contain several people, reasons, developments, conditions,
                  comparisons, dates, quantities or consequences.
                - Give the text a natural title.
                - The information must be internally consistent and self-contained.
                - Do not require outside knowledge.

                QUESTIONS
                - Ask about explicitly stated details, reasons, intentions, consequences,
                  relationships or clearly supported conclusions.
                - Questions must follow the order in which the relevant information appears
                  in the text.
                - Correct options must normally paraphrase the text.
                - Avoid copying complete sentences from the text.
                - Distractors must be based on information mentioned in the text but altered
                  in one meaningful way, such as person, reason, time, amount or result.
                - A distractor must not become correct through an alternative interpretation.
                - Do not use trick questions, double negatives or “all of the above”.
                - Distribute correct keys across a, b and c without an obvious pattern.

                B1 DIFFICULTY
                - Use natural B1 syntax and vocabulary.
                - Include some subordinate and relative clauses, but keep reference chains clear.
                - Make the questions require careful reading rather than advanced vocabulary.

                OUTPUT QUALITY CHECK
                Silently solve every question from the completed text before returning JSON.
                Confirm that exactly one option is supported by the text.
                Provide a short German explanation referring to the relevant information.
                All material must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String readingPart3Instructions() {
        return """
                Create an original TELC Deutsch B1-style Leseverstehen Teil 3 exercise.

                ASSESSMENT PURPOSE
                Test selective reading: the candidate must compare precise needs with practical
                information in advertisements and public notices.

                REQUIRED STRUCTURE
                - Return exactly ten MATCHING questions numbered 11 through 20.
                - Put the ten situations in the individual question stimulus fields.
                - Put exactly twelve notices in exercise-level content.
                - Label the notices a) through l).
                - Separate every notice with a blank line.
                - Every question must contain the same option set a-l plus x.
                - Option x must mean that no notice fits.
                - Every question must have exactly one acceptedOptionKey.
                - A notice may be the correct answer for no more than one situation.
                - One or two situations should correctly use x.
                - Several notices must remain unused.

                SITUATIONS
                - Each situation should be approximately 20-40 words.
                - State a realistic need and at least two relevant conditions.
                - Conditions may involve time, location, age, price, experience, availability,
                  target group, service type or a required feature.
                - Situations must be understandable without specialist knowledge.

                NOTICES
                - Each notice should be approximately 35-80 words.
                - Use authentic everyday formats: advertisements, course announcements,
                  opening-hours notices, travel offers, services, events, rentals, clubs,
                  advice centers or classified ads.
                - Include realistic restrictions and details.
                - Use short headings, abbreviations and compact advertisement language where
                  natural, while remaining understandable at B1.
                - Every correct match must satisfy all essential conditions in its situation.

                DISTRACTOR QUALITY
                - Include near matches that satisfy one condition but fail another.
                - Typical contrasts should involve weekday versus weekend, adults versus
                  children, buying versus renting, beginner versus advanced, or telephone
                  service versus in-person service.
                - Do not make a match obvious through identical rare wording.
                - No situation may have two fully suitable notices.

                OUTPUT QUALITY CHECK
                Silently compare every situation with all twelve notices.
                Verify uniqueness and confirm that x is used only when no notice satisfies
                all essential requirements.
                Explanations must name the decisive matching or conflicting detail.
                All material must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String languagePart1Instructions() {
        return """
                Create an original TELC Deutsch B1-style Sprachbausteine Teil 1 exercise.

                ASSESSMENT PURPOSE
                Test grammatical accuracy in a coherent everyday text.

                REQUIRED STRUCTURE
                - Put one complete German text in exercise-level content.
                - Return exactly ten CLOZE_CHOICE questions numbered 21 through 30.
                - Insert every marker [21] through [30] exactly once in the content.
                - Keep each marker at the exact position of the missing word or phrase.
                - Every question must have exactly three options with keys a, b and c.
                - Every question must have exactly one acceptedOptionKey.
                - Do not place alternatives or solutions directly in the content.
                - Every question has maximumScore 1.5.

                TEXT TYPE
                - Write a coherent personal or semi-formal email or letter of approximately
                  150-210 words.
                - Include a realistic greeting, connected body paragraphs and closing.
                - Use familiar topics such as a trip, course, work experience, move, invitation,
                  appointment, problem, request or personal news.
                - The text must remain natural after all correct answers are inserted.

                GRAMMAR COVERAGE
                Across the ten gaps, include a balanced selection of:
                - conjunctions and connectors;
                - prepositions and required cases;
                - articles, pronouns and adjective endings;
                - verb tense, auxiliary or modal forms;
                - word order and subordinate-clause structures;
                - relative pronouns or common pronominal forms;
                - common fixed grammatical combinations.

                OPTION QUALITY
                - All three options should belong to the same relevant grammatical category
                  whenever possible.
                - Wrong options must be plausible learner errors at A2-B1.
                - Exactly one option must fit both grammar and meaning.
                - Do not test obscure exceptions or specialist vocabulary.
                - Distribute correct keys approximately evenly across a, b and c.
                - Do not create an obvious answer-key pattern.

                OUTPUT QUALITY CHECK
                Silently insert all correct answers and read the entire completed text.
                Then test each distractor and verify why it is incorrect.
                Explanations must briefly identify the relevant grammar rule in German.
                All material must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String languagePart2Instructions() {
        return """
                Create an original TELC Deutsch B1-style Sprachbausteine Teil 2 exercise.

                ASSESSMENT PURPOSE
                Test lexical choice and common functional language in a coherent everyday text.

                REQUIRED STRUCTURE
                - Put one complete German text in exercise-level content.
                - Return exactly ten CLOZE_CHOICE questions numbered 31 through 40.
                - Insert every marker [31] through [40] exactly once in the content.
                - Create one shared word bank containing exactly fifteen options a through o.
                - Repeat the identical fifteen options, with identical keys and text, in every
                  question because the JSON model stores options per question.
                - Ten options are correct and five are distractors.
                - Every correct option may be used only once.
                - Every question must have exactly one acceptedOptionKey.
                - Do not place the word bank or solutions directly in exercise content.
                - Every question has maximumScore 1.5.

                TEXT TYPE
                - Write a coherent semi-formal or formal everyday email or letter of
                  approximately 170-230 words.
                - A short advertisement, announcement or situation may appear before the email
                  when it naturally provides the reason for writing.
                - Use realistic topics such as requesting information, accommodation, courses,
                  travel, appointments, services, complaints or event organization.
                - Include an appropriate greeting and closing.

                WORD BANK
                - Use common B1 words and short fixed expressions.
                - Include a realistic mixture of connectors, adverbs, prepositions, particles,
                  modal forms and functional expressions.
                - Store option text without artificial punctuation.
                - Each correct word must fit naturally in only one gap.
                - The five unused options must be plausible in the topic but impossible in all
                  gaps because of meaning, syntax or collocation.
                - Avoid multiple inflected forms that create more than one valid solution.

                OUTPUT QUALITY CHECK
                Silently complete the entire text using the ten intended options.
                Confirm that every option is used no more than once and that no alternative
                assignment produces another natural, grammatically correct solution.
                Explanations must identify the decisive meaning, collocation or grammatical fit.
                All material must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String writingInstructions() {
        return """
                Create an original TELC Deutsch B1-style Schriftlicher Ausdruck task.

                ASSESSMENT PURPOSE
                The candidate writes one personal or semi-formal email in response to a
                realistic communicative situation.

                REQUIRED STRUCTURE
                - Return exactly one FREE_TEXT question numbered 61.
                - Use an empty options array.
                - Use empty acceptedOptionKeys and acceptedTexts arrays.
                - Put the complete incoming email or source situation in exercise-level content.
                - Put the candidate task and exactly four content points in question.prompt.
                - Do not provide a sample answer.
                - Do not reveal evaluation criteria in the candidate-facing task.
                - The question has maximumScore 45.

                SOURCE EMAIL
                - Write approximately 80-130 words in natural German.
                - Use a realistic personal or semi-formal sender and recipient relationship.
                - The sender should provide context and ask for information, advice, a reaction
                  or help.
                - Use a familiar B1 topic such as visiting, travel, moving, a celebration,
                  a course, work, housing, health, leisure or organizing an activity.
                - Include enough information for the candidate to respond naturally.
                - Avoid controversial, sensitive or specialist topics.

                FOUR CONTENT POINTS
                - Provide exactly four clearly separated bullet points.
                - Every point must require meaningful content, not only yes or no.
                - The points should require a mixture of describing, explaining, suggesting,
                  giving reasons, responding or requesting.
                - The points must be related to the source email but not duplicate one another.
                - They may be answered in any sensible order.

                CANDIDATE INSTRUCTIONS
                Tell the candidate to:
                - respond to the incoming email;
                - address all four content points;
                - choose a sensible order;
                - use an appropriate subject, greeting, introduction and closing;
                - maintain the correct personal or semi-formal register.

                B1 CALIBRATION
                The task must be achievable in approximately 30 minutes by a B1 candidate.
                It should invite connected language, common connectors, reasons and descriptions
                without requiring advanced argumentation.

                All content must be original and must not reproduce published TELC tasks.
                """.strip();
    }

    private String writingRubricJson() {
        return """
                {
                  "scale": "TELC_B1_WRITING",
                  "maximumScore": 45,
                  "scoringMultiplier": 3,
                  "allowedCriterionScores": [15, 9, 3, 0],
                  "criteria": [
                    {
                      "key": "task_achievement",
                      "title": "Aufgabenbewältigung",
                      "maximumScore": 15,
                      "bands": [
                        {
                          "score": 15,
                          "band": "A",
                          "description": "All four content points are addressed appropriately and intelligibly."
                        },
                        {
                          "score": 9,
                          "band": "B",
                          "description": "Three content points are addressed appropriately and intelligibly."
                        },
                        {
                          "score": 3,
                          "band": "C",
                          "description": "Two content points are addressed appropriately and intelligibly."
                        },
                        {
                          "score": 0,
                          "band": "D",
                          "description": "Only one or none of the content points is addressed appropriately."
                        }
                      ]
                    },
                    {
                      "key": "communicative_design",
                      "title": "Kommunikative Gestaltung",
                      "maximumScore": 15,
                      "bands": [
                        {
                          "score": 15,
                          "band": "A",
                          "description": "The email is coherent, appropriately structured, uses a consistent suitable register and a sufficient B1 range of connectors and vocabulary."
                        },
                        {
                          "score": 9,
                          "band": "B",
                          "description": "The email is generally coherent and appropriate, with some limitations in structure, register, connectors or vocabulary."
                        },
                        {
                          "score": 3,
                          "band": "C",
                          "description": "The email uses mostly simple, weakly connected language and shows significant problems with structure, register or recipient orientation."
                        },
                        {
                          "score": 0,
                          "band": "D",
                          "description": "The text lacks meaningful coherence or communicative appropriateness."
                        }
                      ]
                    },
                    {
                      "key": "formal_correctness",
                      "title": "Formale Richtigkeit",
                      "maximumScore": 15,
                      "bands": [
                        {
                          "score": 15,
                          "band": "A",
                          "description": "Grammar, spelling and punctuation are generally well controlled; occasional errors do not disturb understanding."
                        },
                        {
                          "score": 9,
                          "band": "B",
                          "description": "Systematic errors occur, but the message remains predominantly clear and easy to understand."
                        },
                        {
                          "score": 3,
                          "band": "C",
                          "description": "Frequent elementary errors sometimes slow or impair understanding, although the main message can still be understood."
                        },
                        {
                          "score": 0,
                          "band": "D",
                          "description": "Errors are so extensive that the text can only be understood in fragments."
                        }
                      ]
                    }
                  ],
                  "rules": {
                    "offTopicAllCriteriaScore": 0,
                    "wrongSituationTaskAchievementScore": 0,
                    "evaluateAllFourContentPoints": true,
                    "evaluateRegister": true,
                    "evaluateRecipientOrientation": true,
                    "evaluateGreetingAndClosing": true,
                    "evaluateCohesionAndCoherence": true,
                    "comprehensibilityHasPriorityForCorrectness": true,
                    "doNotAutomaticallyPenalizeWordCount": true,
                    "totalMustEqualCriterionSum": true
                  }
                }
                """.strip();
    }

    private SectionDefinition section(
            String key,
            String title,
            int order,
            int duration,
            String timingGroup
    ) {
        SectionDefinition section =
                new SectionDefinition();

        section.setSectionKey(key);
        section.setTitle(title);
        section.setOrderIndex(order);
        section.setDurationSeconds(duration);
        section.setTimingGroup(timingGroup);

        return section;
    }

    private PartDefinition part(
            String key,
            String title,
            int order,
            ContentSource source,
            EvaluationMode evaluation,
            QuestionType type,
            int first,
            int count,
            String pointsEach,
            String maximum,
            String instructions
    ) {
        PartDefinition part = new PartDefinition();

        part.setPartKey(key);
        part.setTitle(title);
        part.setOrderIndex(order);
        part.setContentSource(source);
        part.setEvaluationMode(evaluation);
        part.setQuestionType(type);
        part.setFirstQuestionNumber(first);
        part.setQuestionCount(count);
        part.setPointsPerQuestion(bd(pointsEach));
        part.setMaximumScore(bd(maximum));

        part.setAnswerPattern(
                type == QuestionType.FREE_TEXT
                        ? null
                        : "^[A-Za-z0-9_+\\-]+$"
        );

        part.setGenerationInstructions(instructions);

        return part;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
