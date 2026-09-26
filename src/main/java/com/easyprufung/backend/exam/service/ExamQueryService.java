package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.exception.ResourceNotFoundException;
import com.easyprufung.backend.exam.mapper.ExamViewMapper;
import com.easyprufung.backend.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExamQueryService {
    private final ExamSessionRepository sessions;
    private final ExamResultRepository results;
    private final ExamViewMapper mapper;

    public ExamQueryService(ExamSessionRepository sessions, ExamResultRepository results, ExamViewMapper mapper) {
        this.sessions = sessions; this.results = results; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ExamSessionView getSession(UUID id) {
        return mapper.toView(sessions.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam session not found: " + id)));
    }

    @Transactional(readOnly = true)
    public ExamResultView getResult(UUID sessionId) {
        return mapper.toView(results.findByExamSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found: " + sessionId)));
    }
}
