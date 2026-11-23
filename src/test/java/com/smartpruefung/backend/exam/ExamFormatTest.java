package com.smartpruefung.backend.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpruefung.backend.activity.ActivityService;
import com.smartpruefung.backend.exam.ai.ExamAiClient;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.guest.*;
import com.smartpruefung.backend.exam.mapper.ExamViewMapper;
import com.smartpruefung.backend.exam.repository.*;
import com.smartpruefung.backend.exam.service.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ExamFormatTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ExamViewMapper mapper = new ExamViewMapper(new JsonSupport(json));

    @Test void oldRequestsAndSessionsDefaultToDigital() throws Exception {
        assertEquals(ExamFormat.DIGITAL, json.readValue("{}", StartExamRequest.class).getFormat());
        assertEquals(ExamFormat.DIGITAL, new StartExamRequest("u", ExamProvider.TELC, CefrLevel.B1, "B1").getFormat());
        ExamSession session = session();
        assertEquals(ExamFormat.DIGITAL, mapper.toView(session).getFormat());
        assertEquals(ExamFormat.DIGITAL, mapper.toSummary(session).getFormat());
        session.setFormat(ExamFormat.PAPER);
        assertEquals(ExamFormat.PAPER, mapper.toView(session).getFormat());
        assertEquals(ExamFormat.PAPER, mapper.toSummary(session).getFormat());
        assertEquals(ExamFormat.PAPER, json.readValue("{\"format\":\"PAPER\"}", StartExamRequest.class).getFormat());
        assertThrows(Exception.class, () -> json.readValue("{\"format\":\"UNKNOWN\"}", StartExamRequest.class));
    }

    @Test void guestStartBindsBothFormatsAndRejectsInvalidInput() throws Exception {
        GuestExamService service = mock(GuestExamService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new GuestExamController(service)).build();
        mvc.perform(post("/public/exams/guest/start").header("X-Guest-Token", "token")).andExpect(status().isOk());
        mvc.perform(post("/public/exams/guest/start?format=PAPER").header("X-Guest-Token", "token")).andExpect(status().isOk());
        mvc.perform(post("/public/exams/guest/start?format=INVALID").header("X-Guest-Token", "token")).andExpect(status().isBadRequest());
        verify(service).start("token", ExamFormat.DIGITAL);
        verify(service).start("token", ExamFormat.PAPER);
        verifyNoMoreInteractions(service);
    }

    @Test void generationPersistsChoiceAndRetainsExistingActiveFormat() {
        for (ExamFormat format : ExamFormat.values()) {
            ExamSessionRepository sessions = mock(ExamSessionRepository.class);
            ExamDefinitionRepository definitions = mock(ExamDefinitionRepository.class);
            ExamDefinitionService definitionService = mock(ExamDefinitionService.class);
            ExerciseTemplateRepository templates = mock(ExerciseTemplateRepository.class);
            ExamDefinition definition = session().getExamDefinition();
            SectionDefinition section = new SectionDefinition(); section.setId(UUID.randomUUID());
            section.setSectionKey("SCHREIBEN"); section.setTitle("Schreiben"); section.setDurationSeconds(1800); definition.addSection(section);
            PartDefinition part = new PartDefinition(); part.setId(UUID.randomUUID()); part.setPartKey("SCHREIBEN_EMAIL");
            part.setTitle("E-Mail"); part.setContentSource(ContentSource.PREDEFINED); part.setQuestionType(QuestionType.FREE_TEXT);
            part.setQuestionCount(1); part.setPointsPerQuestion(BigDecimal.ONE); section.addPart(part);
            ExerciseTemplate template = new ExerciseTemplate(); template.setContent("Übungsaufgabe");
            QuestionTemplate question = new QuestionTemplate(); question.setExternalNumber("61"); question.setPrompt("Schreiben Sie eine E-Mail.");
            question.setQuestionType(QuestionType.FREE_TEXT); question.setMaximumScore(BigDecimal.ONE); question.setCorrectAnswerJson("{}"); template.addQuestion(question);
            when(definitionService.resolve(any())).thenReturn(definition);
            when(definitions.lockForStart(definition.getId())).thenReturn(Optional.of(definition));
            when(templates.findByPartDefinitionIdAndActiveTrueOrderByTemplateKeyAsc(part.getId())).thenReturn(List.of(template));
            when(sessions.save(any())).thenAnswer(call -> { ExamSession s = call.getArgument(0); s.setId(UUID.randomUUID()); return s; });
            JsonSupport support = new JsonSupport(json);
            ExamGenerationService generation = new ExamGenerationService(definitionService, templates, sessions, mock(ExamAiClient.class), support, mapper,
                    Runnable::run, definitions, mock(SharedExerciseRepository.class), mock(ExerciseInstanceRepository.class),
                    mock(SharedExerciseStore.class), new ExerciseFingerprint(support));
            ReflectionTestUtils.setField(generation, "activity", mock(ActivityService.class));
            ExamSessionView view = generation.start(new StartExamRequest("u", ExamProvider.TELC, CefrLevel.B1, "B1", format));
            assertEquals(format, view.getFormat());
            assertEquals(1800, Duration.between(view.getStartedAt(), view.getExpiresAt()).getSeconds());
            var captured = org.mockito.ArgumentCaptor.forClass(ExamSession.class);
            verify(sessions).save(captured.capture());
            assertEquals(format, captured.getValue().getFormat());
            when(sessions.findFirstByUserIdAndExamDefinitionIdAndStatusOrderByCreatedAtDesc("u", definition.getId(), SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(captured.getValue()));
            ExamFormat opposite = format == ExamFormat.PAPER ? ExamFormat.DIGITAL : ExamFormat.PAPER;
            assertEquals(format, generation.start(new StartExamRequest("u", ExamProvider.TELC, CefrLevel.B1, "B1", opposite)).getFormat());
            verify(sessions, times(1)).save(any());
        }
    }

    private ExamSession session() {
        ExamDefinition definition = new ExamDefinition(); definition.setId(UUID.randomUUID()); definition.setCode("B1");
        definition.setTitle("TELC Deutsch B1"); definition.setProvider(ExamProvider.TELC); definition.setLevel(CefrLevel.B1);
        ExamSession session = new ExamSession(); session.setId(UUID.randomUUID()); session.setExamDefinition(definition);
        return session;
    }
}
