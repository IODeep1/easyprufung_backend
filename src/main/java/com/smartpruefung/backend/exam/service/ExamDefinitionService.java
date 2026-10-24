package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.domain.ExamDefinition;
import com.smartpruefung.backend.exam.dto.ExamSummaryView;
import com.smartpruefung.backend.exam.exception.ResourceNotFoundException;
import com.smartpruefung.backend.exam.repository.ExamDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamDefinitionService {
    private final ExamDefinitionRepository repository;

    public ExamDefinitionService(ExamDefinitionRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<ExamSummaryView> listActive() {
        return repository.findByActiveTrueOrderByProviderAscLevelAscCodeAsc().stream().map(d ->
                new ExamSummaryView(d.getCode(), d.getTitle(), d.getProvider(), d.getLevel(),
                        d.getDefinitionVersion(), d.getMaximumScore(), d.getPassPercentage())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamDefinition resolve(com.smartpruefung.backend.exam.dto.StartExamRequest request) {
        ExamDefinition definition = (request.getExamCode() == null || request.getExamCode().isBlank()
                ? repository.findFirstByProviderAndLevelAndActiveTrueOrderByDefinitionVersionDesc(
                        request.getProvider(), request.getLevel())
                : repository.findFirstByProviderAndLevelAndCodeAndActiveTrueOrderByDefinitionVersionDesc(
                        request.getProvider(), request.getLevel(), request.getExamCode()))
                .orElseThrow(() -> new ResourceNotFoundException("No active exam definition matches the request"));

        /*
         * Initialize the small definition graph for callers and AI workers.
         * Exam starts join the surrounding selection transaction; workers only
         * read already initialized definition fields.
         */
        definition.getSections().forEach(
                section -> section.getParts().size()
        );

        return definition;
    }
}
