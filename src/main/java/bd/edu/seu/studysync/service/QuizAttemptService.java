package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Question;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizAttempt;
import bd.edu.seu.studysync.model.UserAnswer;
import bd.edu.seu.studysync.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRoomService quizRoomService;
    private final OpenAiChatModel chatModel;


    public QuizAttempt evaluateQuiz(Quiz quiz, Map<String, String> userAnswers, int timeTaken, String userId, String roomId) {
        List<UserAnswer> answers = new ArrayList<>();
        double totalScore = 0.0;
        int mcqCorrectCount = 0;
        int cqCount = 0;

        // Loop through each question and check answer
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Question question = quiz.getQuestions().get(i);
            int questionNum = i + 1;
            String questionType = question.getQuestionType() != null ? question.getQuestionType() : "MCQ";

            UserAnswer userAnswer;

            if ("CQ".equals(questionType)) {
                // Handle Constructed Response question
                String textAnswer = userAnswers.getOrDefault("cq_" + questionNum, "");
                cqCount++;

                // Grade the CQ answer using AI
                Map<String, Object> cqGrading = gradeCqAnswer(
                    question.getQuestion(),
                    textAnswer,
                    question.getCorrectAnswer(),
                    question.getAnswerExplanation()
                );

                double cqScore = (Double) cqGrading.get("score");
                String aiFeedback = (String) cqGrading.get("feedback");

                totalScore += cqScore;

                userAnswer = new UserAnswer();
                userAnswer.setQuestionNumber(questionNum);
                userAnswer.setQuestionType("CQ");
                userAnswer.setQuestion(question.getQuestion());
                userAnswer.setTextAnswer(textAnswer);
                userAnswer.setExpectedAnswer(question.getCorrectAnswer());
                userAnswer.setCqScore(cqScore);
                userAnswer.setAiFeedback(aiFeedback);
                userAnswer.setCorrectAnswer(question.getCorrectAnswer()); // For reference
                userAnswer.setCorrect(cqScore >= 0.6); // Consider correct if score >= 60%

                if (cqScore >= 0.6) {
                    mcqCorrectCount++;
                }

            } else {
                // Handle MCQ question
                String selected = userAnswers.getOrDefault(String.valueOf(questionNum), "");
                String correct = question.getCorrectAnswer();

                boolean isCorrect = selected.equals(correct);
                if (isCorrect) {
                    mcqCorrectCount++;
                    totalScore += 1.0;
                }

                userAnswer = new UserAnswer(
                    questionNum,
                    selected,
                    correct,
                    isCorrect,
                    question.getQuestion()
                );
            }

            answers.add(userAnswer);
        }

        // Calculate score percentage
        double scorePercentage = (totalScore * 100.0) / quiz.getQuestions().size();

        // Create QuizAttempt object
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quiz.getId());
        attempt.setPdfFileName(quiz.getPdfFileName());
        attempt.setDifficulty(quiz.getDifficulty());
        attempt.setQuestionCount(quiz.getQuestionCount());
        attempt.setTimeLimitSeconds(quiz.getTimeLimitSeconds());
        attempt.setUserAnswers(answers);
        attempt.setTotalQuestions(quiz.getQuestions().size());
        attempt.setCorrectAnswers(mcqCorrectCount);
        attempt.setScorePercentage(Math.round(scorePercentage * 100.0) / 100.0);
        attempt.setTimeTakenSeconds(timeTaken);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setUserId(userId);
        attempt.setRoomId(roomId);

        // Save to database
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // Notify QuizRoomService if this validation is part of a room
        if (roomId != null && !roomId.isEmpty()) {
            quizRoomService.updateScore(roomId, userId, savedAttempt.getId(), savedAttempt.getScorePercentage());
        }

        return savedAttempt;
    }

    /**
     * Grade a Constructed Response answer using AI
     * Returns a map with "score" (0.0 to 1.0) and "feedback"
     */
    private Map<String, Object> gradeCqAnswer(String question, String userAnswer, String expectedAnswer, String explanation) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = """
                    You are an expert grader. Evaluate the student's answer to the following question.

                    QUESTION: %s

                    STUDENT'S ANSWER: %s

                    EXPECTED KEY POINTS: %s

                    GRADING CRITERIA:
                    - Award full credit (1.0) if the answer covers all key points accurately
                    - Award partial credit (0.3-0.7) if the answer covers some key points but is incomplete
                    - Award minimal credit (0.1-0.2) if the answer shows some understanding but misses most key points
                    - Award no credit (0.0) if the answer is irrelevant, incorrect, or empty

                    ADDITIONAL GUIDANCE: %s

                    Output ONLY a JSON object in this exact format:
                    {
                      "score": 0.75,
                      "feedback": "Brief feedback explaining the grade"
                    }

                    Do NOT include any explanation outside the JSON. Just the JSON object.
                    """.formatted(question, userAnswer, expectedAnswer, explanation != null ? explanation : "Grade based on how well the answer addresses the question.");

            String aiResponse = chatModel.call(prompt).trim();

            // Parse the JSON response
            String cleaned = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            // Find the JSON object boundaries
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');

            if (start != -1 && end != -1 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            // Simple parsing (for production, use a proper JSON parser)
            // Extract score
            if (cleaned.contains("\"score\"")) {
                int scoreStart = cleaned.indexOf("\"score\"") + 8;
                int scoreEnd = cleaned.indexOf(',', scoreStart);
                if (scoreEnd == -1) scoreEnd = cleaned.indexOf('}', scoreStart);
                String scoreStr = cleaned.substring(scoreStart, scoreEnd).trim().replace("\"", "");
                result.put("score", Double.parseDouble(scoreStr));
            } else {
                result.put("score", 0.5); // Default middle score
            }

            // Extract feedback
            if (cleaned.contains("\"feedback\"")) {
                int fbStart = cleaned.indexOf("\"feedback\"") + 11;
                int fbEnd = cleaned.lastIndexOf('\"');
                if (fbStart < fbEnd) {
                    result.put("feedback", cleaned.substring(fbStart, fbEnd));
                } else {
                    result.put("feedback", "Unable to parse feedback.");
                }
            } else {
                result.put("feedback", "No feedback provided.");
            }

        } catch (Exception e) {
            // Fallback to simple scoring if AI grading fails
            result.put("score", userAnswer.length() > 20 ? 0.5 : 0.2);
            result.put("feedback", "AI grading unavailable. Answer graded based on length.");
        }

        return result;
    }


    public List<QuizAttempt> getAttemptsByUserId(String userId) {
        return quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
    }


    public QuizAttempt getAttemptById(String attemptId) {
        return quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));
    }
}