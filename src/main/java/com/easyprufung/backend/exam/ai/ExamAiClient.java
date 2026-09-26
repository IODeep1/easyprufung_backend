package com.easyprufung.backend.exam.ai;

public interface ExamAiClient {
    AiGeneratedExerciseResponse generateExercise(AiExerciseGenerationRequest request);
    AiWritingEvaluationResponse evaluateWriting(AiWritingEvaluationRequest request);
}
