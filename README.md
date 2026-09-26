# main-backend

# EasyPrüfung exam engine

Spring Boot backend module for configurable German mock exams. The first seeded definition is the written portion of **telc Deutsch B1**. The engine itself contains no TELC-specific branching: providers, levels, sections, parts, question types, content sources, timing, numbering, scoring, and evaluation modes are data.

The project uses Java 15 and Spring Boot 2.7.18.

## Architecture

There are three deliberately separate persistence lifecycles:

1. **Definition and templates** — versioned exam structure plus administrator-provided audio/predefined content.
2. **Session snapshot** — generated/copied exercises and answer keys frozen when a candidate starts an exam. Later template edits cannot change an active attempt.
3. **Submission and result** — candidate answers, per-question evaluation, per-section totals, overall result, and feedback.

`ContentSource` determines where an exercise comes from:

- `AI_GENERATED`: OpenAI creates the exercise through a strongly typed JSON contract.
- `PREDEFINED`: the engine selects an active stored template.
- `AUDIO_PREDEFINED`: the engine selects stored audio, questions, options, and keys; AI is never called.

`EvaluationMode` is independent from content source:

- `OBJECTIVE`: exact local evaluation.
- `AUDIO_OBJECTIVE`: exact local evaluation with audio integrity checks.
- `AI_WRITTEN`: rubric-based structured AI evaluation.

This separation supports combinations such as an AI-generated multiple-choice exercise that is still scored deterministically by the backend.

## Project structure

```text
src/main/java/com/easyprufung/backend
├── EasyPrufungApplication.java
└── exam
    ├── ai          # provider-neutral contract plus OpenAI Responses API adapter
    ├── api         # candidate and template-administration REST endpoints
    ├── config      # AI client configuration and TELC B1 definition seed
    ├── domain      # definition, template, session, answer and result entities
    ├── dto         # public API contracts (answer keys excluded from session views)
    ├── exception   # domain/API errors
    ├── mapper      # safe entity-to-API projections
    ├── repository  # Spring Data repositories
    └── service     # generation, parsing, submission, scoring and evaluators
src/main/resources
├── application.yml
└── schemas         # strict JSON Schema documents for AI responses
examples            # predefined audio-template request example
```

## Seeded TELC B1 definition

| Section | Part | Questions | Type | Max. points | Source / evaluator |
|---|---:|---:|---|---:|---|
| Leseverstehen | 1 | 1–5 | matching | 25 | AI / local |
| Leseverstehen | 2 | 6–10 | single choice | 25 | AI / local |
| Leseverstehen | 3 | 11–20 | matching | 25 | AI / local |
| Sprachbausteine | 1 | 21–30 | cloze choice | 15 | AI / local |
| Sprachbausteine | 2 | 31–40 | cloze matching | 15 | AI / local |
| Hörverstehen | 1 | 41–45 | true/false | 25 | predefined audio / local |
| Hörverstehen | 2 | 46–55 | true/false | 25 | predefined audio / local |
| Hörverstehen | 3 | 56–60 | true/false | 25 | predefined audio / local |
| Schriftlicher Ausdruck | E-Mail | 61 | free text | 45 | AI / AI rubric |

The configured written maximum is 225 points and the pass threshold is 60%. Reading and language elements share one 90-minute timing group; listening and writing add 30 minutes each. The writing rubric stores the three TELC B1 criteria independently of the evaluator implementation.

This module intentionally does not include the oral subtest because it was outside the requested structure. Add it as another section/part configuration without changing the engine.

## Running

```bash
mvn clean package
mvn spring-boot:run
```

H2 is the default development database. For PostgreSQL:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/easyprufung'
export DB_USERNAME='easyprufung'
export DB_PASSWORD='change-me'
export JPA_DDL_AUTO='validate'
```

For production, manage the same entity schema with your normal migration tool and use `JPA_DDL_AUTO=validate`.

## OpenAI configuration

The provider adapter calls `POST /v1/responses` and requests strict Structured Outputs. Exercise generation and writing evaluation use separate JSON Schemas, then deserialize and validate the returned JSON as the existing Java AI DTOs.

Enable OpenAI and select the model entirely through configuration:

```bash
export OPENAI_ENABLED=true
export OPENAI_API_KEY='your-api-key'
export OPENAI_MODEL='gpt-4o-mini'
```

The matching `application.yml` settings are:

```yaml
easyprufung:
  ai:
    enabled: ${OPENAI_ENABLED:false}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    api-key: ${OPENAI_API_KEY:}
    model: ${OPENAI_MODEL:gpt-4o-mini}
    responses-path: ${OPENAI_RESPONSES_PATH:/responses}
    organization: ${OPENAI_ORGANIZATION:}
    project: ${OPENAI_PROJECT:}
    max-output-tokens: ${OPENAI_MAX_OUTPUT_TOKENS:4000}
    connect-timeout: ${OPENAI_CONNECT_TIMEOUT:5s}
    read-timeout: ${OPENAI_READ_TIMEOUT:120s}
```

`ExamAiClient` remains provider-neutral; `OpenAiExamAiClient` contains the OpenAI request envelope, authentication headers, output extraction, refusal/incomplete-response handling, and JSON parsing. A future provider can be added as another adapter without changing exam generation or evaluation services.

Requests and responses are strongly typed Java POJOs with Bean Validation. The OpenAI adapter supplies strict response schemas, `schemaVersion` must match, generated question count/type/number/points must match the persisted part definition, and writing scores cannot exceed the configured maximum.

## API flow

List definitions:

```http
GET /api/exams/definitions
```

Before TELC B1 can start, upload at least one active template for each `HOEREN_1`, `HOEREN_2`, and `HOEREN_3` part. Post the example file as follows, changing the audio URL, prompts, and correct keys to your content:

```http
POST /api/admin/exam-definitions/TELC_DEUTSCH_B1_WRITTEN/parts/HOEREN_1/templates
Content-Type: application/json

{ ...examples/telc-b1-hoeren-teil-1-template.json... }
```

Secure `/api/admin/**` in the application's security layer. Authentication/user ownership are intentionally outside this exam-only module; in a full application, derive `userId` from the authenticated principal rather than trusting request JSON.

Start an exam:

```http
POST /api/exams/sessions
Content-Type: application/json

{"userId":"user-123","provider":"TELC","level":"B1","examCode":"TELC_DEUTSCH_B1_WRITTEN"}
```

The response contains all candidate-visible content, stable question numbers, option keys, audio metadata, and timing—but never correct answers.

Submit structured answers and/or the compact TELC-style representation:

```http
POST /api/exams/sessions/{sessionId}/submit
Content-Type: application/json

{
  "compactAnswers": "1f, 2d, 3g, 41+, 42-, 60+",
  "answers": [
    {"questionNumber":"61","selectedOptionKeys":[],"text":"Liebe Anna, ..."}
  ]
}
```

Omitted objective questions score zero. Duplicate or unknown numbers, invalid option keys, mismatched answer types, malformed compact syntax, expired sessions, and invalid AI JSON are rejected. Repeating submission after a successful evaluation is idempotent and returns the stored result.

Retrieve state/result:

```http
GET /api/exams/sessions/{sessionId}
GET /api/exams/sessions/{sessionId}/result
```

## Adding another exam

Create a new versioned `ExamDefinition`, attach ordered `SectionDefinition` and `PartDefinition` rows, and select source/evaluation strategies for each part. Add predefined templates where required. No controller, session, answer, scoring, or result code needs to change.

For example, a Goethe B2 definition can use different section order, durations, numbering, question types, and point values. A new evaluation behavior only requires another `AnswerEvaluator` implementation keyed by a new `EvaluationMode`; existing definitions remain unchanged.

## Important production boundaries

- Store object-storage URLs or opaque media IDs, not audio bytes, in the database. Issue short-lived authorized playback URLs at the delivery boundary if content must be protected.
- Add authentication and verify session ownership before exposing candidate endpoints.
- Replace `ddl-auto=update` with migrations.
- Add retry/idempotency, rate limiting, observability, and a human-review policy for high-stakes writing scores before production use.
- Version every definition and rubric. Never mutate a version used by completed sessions; activate a new version instead.
- Ensure you have rights to reproduce exam content and branding. The seed contains structure and scoring metadata only, not copyrighted TELC exercise content.
