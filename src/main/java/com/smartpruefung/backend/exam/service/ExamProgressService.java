package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.repository.ExamSessionRepository;
import com.smartpruefung.backend.exam.mapper.ExamViewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import java.util.HashSet;

@Service
public class ExamProgressService {
    private final ExamSessionRepository sessions;
    private final JsonSupport json;
    private final ExamViewMapper mapper;
    public ExamProgressService(ExamSessionRepository sessions, JsonSupport json, ExamViewMapper mapper) {
        this.sessions = sessions; this.json = json; this.mapper = mapper;
    }
    @Transactional(readOnly = true)
    public ExamSessionView active(String userId) {
        return sessions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.IN_PROGRESS)
                .map(mapper::toView).orElse(null);
    }
    public void authorize(ExamSession session, String userId) {
        if (!session.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    @Transactional(readOnly = true)
    public void authorize(UUID id, String userId) {
        authorize(sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), userId);
    }
    @Transactional(readOnly = true)
    public ExamProgressView get(UUID id, String userId) {
        ExamSession session = sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        authorize(session, userId);
        return read(session);
    }
    private ExamProgressView read(ExamSession session) {
        return session.getProgressJson() == null ? new ExamProgressView()
                : json.read(session.getProgressJson(), ExamProgressView.class);
    }
    @Transactional
    public ExamProgressView save(UUID id, String userId, ExamProgressView draft) {
        ExamSession session = sessions.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        authorize(session, userId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS || read(session).getRevision() != draft.getRevision())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam changed on another device. Reload to continue.");
        if (draft.getIndex() >= session.getExercises().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var numbers = new HashSet<String>();
        session.getExercises().forEach(e -> e.getQuestions().forEach(q -> numbers.add(q.getExternalNumber())));
        if (!numbers.containsAll(draft.getAnswers().keySet())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        draft.setRevision(draft.getRevision() + 1);
        session.setProgressJson(json.write(draft));
        return draft;
    }
}
