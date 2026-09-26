package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.domain.*;
import com.easyprufung.backend.exam.dto.CreateTemplateRequest;
import com.easyprufung.backend.exam.exception.*;
import com.easyprufung.backend.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ExerciseTemplateService {
    private final PartDefinitionRepository parts;
    private final ExerciseTemplateRepository templates;
    private final JsonSupport json;

    public ExerciseTemplateService(PartDefinitionRepository parts, ExerciseTemplateRepository templates, JsonSupport json) {
        this.parts = parts; this.templates = templates; this.json = json;
    }

    @Transactional
    public UUID create(String examCode, String partKey, CreateTemplateRequest request) {
        PartDefinition part = parts
                .findBySectionDefinitionExamDefinitionCodeAndPartKeyAndSectionDefinitionExamDefinitionActiveTrue(examCode, partKey)
                .orElseThrow(() -> new ResourceNotFoundException("Active exam part not found: " + examCode + "/" + partKey));
        if (part.getContentSource() == ContentSource.AI_GENERATED) {
            throw new ExamConfigurationException("AI-generated parts do not accept predefined templates");
        }
        if (request.getQuestions().size() != part.getQuestionCount()) {
            throw new IllegalArgumentException("Expected " + part.getQuestionCount() + " questions");
        }
        if (part.getContentSource() == ContentSource.AUDIO_PREDEFINED
                && (request.getAudioUrl() == null || request.getAudioUrl().isBlank())) {
            throw new IllegalArgumentException("audioUrl is required for an audio part");
        }
        BigDecimal score = request.getQuestions().stream().map(CreateTemplateRequest.TemplateQuestionRequest::getMaximumScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (score.compareTo(part.getMaximumScore()) != 0) {
            throw new IllegalArgumentException("Template maximum score must equal part maximum score " + part.getMaximumScore());
        }
        Set<String> numbers = new HashSet<>();
        ExerciseTemplate template = new ExerciseTemplate();
        template.setPartDefinition(part);
        template.setTemplateKey(request.getTemplateKey());
        template.setInstructions(request.getInstructions());
        template.setContent(request.getContent());
        template.setAudioUrl(request.getAudioUrl());
        template.setAudioContentType(request.getAudioContentType());
        template.setAudioPlayLimit(request.getAudioPlayLimit());
        int order = 0;
        for (var q : request.getQuestions()) {
            if (!numbers.add(q.getNumber())) throw new IllegalArgumentException("Duplicate question number " + q.getNumber());
            if (q.getType() != part.getQuestionType()) throw new IllegalArgumentException("Question type must be " + part.getQuestionType());
            if (q.getType() != QuestionType.FREE_TEXT) {
                if (q.getOptions().isEmpty()) throw new IllegalArgumentException("Question " + q.getNumber() + " has no options");
                Set<String> optionKeys = q.getOptions().stream().map(o -> o.getKey().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
                if (optionKeys.size() != q.getOptions().size()) throw new IllegalArgumentException("Duplicate option key in question " + q.getNumber());
                if (q.getCorrectAnswer().getAcceptedOptionKeys().isEmpty()) throw new IllegalArgumentException("Question " + q.getNumber() + " has no correct option key");
                if (q.getCorrectAnswer().getAcceptedOptionKeys().stream().map(k -> k.toLowerCase(Locale.ROOT)).anyMatch(k -> !optionKeys.contains(k))) {
                    throw new IllegalArgumentException("Correct answer refers to an unknown option in question " + q.getNumber());
                }
            }
            QuestionTemplate entity = new QuestionTemplate();
            entity.setOrderIndex(order++);
            entity.setExternalNumber(q.getNumber());
            entity.setPrompt(q.getPrompt());
            entity.setQuestionType(q.getType());
            entity.setOptionsJson(json.write(q.getOptions()));
            entity.setCorrectAnswerJson(json.write(q.getCorrectAnswer()));
            entity.setMaximumScore(q.getMaximumScore());
            entity.setExplanation(q.getExplanation());
            template.addQuestion(entity);
        }
        return templates.save(template).getId();
    }
}
