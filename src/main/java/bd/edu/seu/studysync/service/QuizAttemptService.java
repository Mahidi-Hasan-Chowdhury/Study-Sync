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
        int fullyCorrectCount = 0;  // 3/3 or MCQ correct
        int partialCount = 0;       // 1/3 or 2/3
        int wrongCount = 0;          // 0/3 or MCQ wrong
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

                // Grade the CQ answer using AI (returns points 0-3)
                Map<String, Object> cqGrading = gradeCqAnswer(
                    question.getQuestion(),
                    textAnswer,
                    question.getCorrectAnswer(),
                    question.getAnswerExplanation()
                );

                int pointsEarned = (Integer) cqGrading.get("points");
                int maxPoints = 3; // CQ questions are out of 3 points
                String aiFeedback = (String) cqGrading.get("feedback");

                // Add to total (normalized for percentage calculation)
                totalScore += (pointsEarned / (double) maxPoints);

                userAnswer = new UserAnswer();
                userAnswer.setQuestionNumber(questionNum);
                userAnswer.setQuestionType("CQ");
                userAnswer.setQuestion(question.getQuestion());
                userAnswer.setTextAnswer(textAnswer);
                userAnswer.setExpectedAnswer(question.getCorrectAnswer());
                userAnswer.setPointsEarned(pointsEarned);
                userAnswer.setMaxPoints(maxPoints);
                userAnswer.setAiFeedback(aiFeedback);
                userAnswer.setCorrectAnswer(question.getCorrectAnswer()); // For reference

                // Count based on points earned
                if (pointsEarned >= 3) {
                    fullyCorrectCount++;
                    userAnswer.setCorrect(true);
                } else if (pointsEarned >= 1) {
                    partialCount++;
                    userAnswer.setCorrect(false); // Not fully correct, but partial credit
                } else {
                    wrongCount++;
                    userAnswer.setCorrect(false);
                }

            } else {
                // Handle MCQ question
                String selected = userAnswers.getOrDefault(String.valueOf(questionNum), "");
                String correct = question.getCorrectAnswer();

                boolean isCorrect = selected.equals(correct);
                if (isCorrect) {
                    fullyCorrectCount++;
                    totalScore += 1.0;
                } else {
                    wrongCount++;
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

        // Calculate total marks
        int totalMarksEarned = 0;
        int totalMarksPossible = 0;
        for (UserAnswer answer : answers) {
            totalMarksEarned += answer.getPointsEarned() != null ? answer.getPointsEarned() : 0;
            totalMarksPossible += answer.getMaxPoints() != null ? answer.getMaxPoints() : 1;
        }

        // Calculate score percentage (based on marks)
        double scorePercentage = totalMarksPossible > 0 ? (totalMarksEarned * 100.0) / totalMarksPossible : 0;

        // Create QuizAttempt object
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quiz.getId());
        attempt.setPdfFileName(quiz.getPdfFileName());
        attempt.setDifficulty(quiz.getDifficulty());
        attempt.setQuestionCount(quiz.getQuestionCount());
        attempt.setTimeLimitSeconds(quiz.getTimeLimitSeconds());
        attempt.setUserAnswers(answers);
        attempt.setTotalQuestions(quiz.getQuestions().size());
        attempt.setCorrectAnswers(fullyCorrectCount);
        attempt.setPartialAnswers(partialCount);
        attempt.setWrongAnswers(wrongCount);
        attempt.setScorePercentage(Math.round(scorePercentage * 100.0) / 100.0);
        attempt.setTotalMarksEarned(totalMarksEarned);
        attempt.setTotalMarksPossible(totalMarksPossible);
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
     * Returns a map with "points" (0-3) and "feedback"
     */
    private Map<String, Object> gradeCqAnswer(String question, String userAnswer, String expectedAnswer, String explanation) {
        Map<String, Object> result = new HashMap<>();

        // Check for empty answer - give 0 points immediately
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            result.put("points", 0);
            result.put("feedback", "No answer provided.");
            return result;
        }

        try {
            String prompt = """
                    You are an expert grader. Evaluate the student's answer to the following question.

                    GRADING PHILOSOPHY: Be generous. Give full marks for any answer that demonstrates understanding of the core concept.

                    IMPORTANT CONTEXT: The student was ONLY given access to the source PDF material. They should NOT be expected to know concepts beyond what was in that material.

                    QUESTION: %s

                    STUDENT'S ANSWER: %s

                    MODEL ANSWER (from source material): %s

                    GRADING RUBRIC (0-3 points):
                    3 points = CORRECT: Student demonstrates understanding of the main concept. Even if brief or missing details, give 3 points if core idea is right.
                    2 points = PARTIAL: Answer has some correct elements but misses the core concept.
                    1 point = ATTEMPTED: Barely relevant or mostly incorrect.
                    0 points = WRONG: Completely incorrect, irrelevant, or off-topic.

                    GRADING GUIDANCE: %s

                    IMPORTANT: If the student's answer demonstrates they understood the main concept, give 3 points. Don't penalize for missing details or simple language.

                    Output ONLY a JSON object in this exact format:
                    {
                      "points": 3,
                      "feedback": "Brief specific feedback explaining the grade"
                    }

                    Do NOT include any explanation outside the JSON. Just the JSON object.
                    """.formatted(question, userAnswer, expectedAnswer, explanation != null ? explanation : "Grade based on core understanding - be extremely generous with full marks.");

            String aiResponse = chatModel.call(prompt).trim();

            // Debug: log the AI response
            System.out.println("AI Response for grading: " + aiResponse);

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

            // Extract points
            try {
                if (cleaned.contains("\"points\"")) {
                    int pointsStart = cleaned.indexOf("\"points\"") + 9;
                    int pointsEnd = cleaned.indexOf(',', pointsStart);
                    if (pointsEnd == -1) pointsEnd = cleaned.indexOf('}', pointsStart);
                    String pointsStr = cleaned.substring(pointsStart, pointsEnd).trim();
                    // Remove quotes and any whitespace
                    pointsStr = pointsStr.replaceAll("[\" ]", "").trim();
                    int points = Integer.parseInt(pointsStr);
                    // Ensure points is in valid range (0-3)
                    points = Math.max(0, Math.min(3, points));
                    result.put("points", points);
                } else {
                    result.put("points", 2); // Default middle score
                }
            } catch (Exception e) {
                result.put("points", 2);
            }

            // Extract feedback with better parsing
            try {
                int feedbackKeyPos = cleaned.indexOf("\"feedback\"");
                if (feedbackKeyPos != -1) {
                    // Find the colon after "feedback"
                    int colonPos = cleaned.indexOf(':', feedbackKeyPos);
                    if (colonPos != -1) {
                        // Find the opening quote after the colon (skip whitespace and colon)
                        int searchStart = colonPos + 1;
                        while (searchStart < cleaned.length() && cleaned.charAt(searchStart) != '"') {
                            searchStart++;
                        }
                        if (searchStart < cleaned.length() && cleaned.charAt(searchStart) == '"') {
                            int valueStart = searchStart + 1;
                            // Find the closing quote
                            int valueEnd = cleaned.indexOf('"', valueStart);
                            if (valueEnd > valueStart) {
                                String feedback = cleaned.substring(valueStart, valueEnd);
                                if (!feedback.isEmpty()) {
                                    result.put("feedback", feedback);
                                } else {
                                    result.put("feedback", "Empty feedback received.");
                                }
                            } else {
                                result.put("feedback", "Could not find feedback end quote.");
                            }
                        } else {
                            result.put("feedback", "Could not find feedback start quote.");
                        }
                    } else {
                        result.put("feedback", "Could not find feedback colon.");
                    }
                } else {
                    result.put("feedback", "No feedback field in AI response.");
                }
            } catch (Exception e) {
                result.put("feedback", "Error parsing feedback: " + e.getMessage());
            }

        } catch (Exception e) {
            // Fallback to simple scoring if AI grading fails (generous approach)
            int fallbackPoints = 3; // Default to full marks
            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                fallbackPoints = 0; // Only give 0 if completely empty
            } else if (userAnswer.length() < 10) {
                fallbackPoints = 1; // Minimal attempt
            }
            result.put("points", fallbackPoints);

            // Default feedback based on points (0-3 scale, generous)
            String defaultFeedback = switch (fallbackPoints) {
                case 3 -> "Great! You demonstrated understanding of the concept.";
                case 1 -> "Attempted - please review the model answer for more details.";
                case 0 -> "Please provide an answer to earn credit.";
                default -> "Good effort!";
            };
            result.put("feedback", defaultFeedback + " (AI grading unavailable)");
        }

        // Ensure feedback exists, add default based on points if missing
        Object feedbackObj = result.get("feedback");
        String feedback = (feedbackObj != null) ? feedbackObj.toString() : "";
        if (feedback.isEmpty()) {
            Object pointsObj = result.get("points");
            int points = (pointsObj != null) ? (Integer) pointsObj : 3;

            String defaultFeedback = switch (points) {
                case 3 -> "Great! You demonstrated understanding of the concept.";
                case 2 -> "Good effort - review the model answer for more details.";
                case 1 -> "Attempted - please review the model answer.";
                case 0 -> "Please provide an answer to earn credit.";
                default -> "Good effort!";
            };
            result.put("feedback", defaultFeedback);
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