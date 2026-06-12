package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.LiveQuizAttempt;
import bd.edu.seu.studysync.model.LiveQuizSession;
import bd.edu.seu.studysync.model.Question;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.repository.LiveQuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing live quiz attempts
 *
 * Handles:
 * - Creating new quiz attempts
 * - Saving answers during quiz
 * - Submitting completed quizzes
 * - Grading submissions
 * - Retrieving results and leaderboards
 */
@Service
@RequiredArgsConstructor
public class LiveQuizAttemptService {

    private final LiveQuizAttemptRepository attemptRepository;
    private final LiveQuizSessionService sessionService;
    private final QuizAiService quizAiService;
    private final ClassroomService classroomService;

    // ==================== CREATE ====================

    /**
     * Start a new quiz attempt for a student
     *
     * @param sessionId Live quiz session ID
     * @param studentId Student attempting the quiz
     * @return Created attempt
     */
    public LiveQuizAttempt startAttempt(String sessionId, String studentId) {
        // Get session
        LiveQuizSession session = sessionService.getSessionById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Validate session is live or lobby is open
        if (session.getStatus() != LiveQuizSession.SessionStatus.LIVE &&
            session.getStatus() != LiveQuizSession.SessionStatus.LOBBY_OPEN) {
            throw new RuntimeException("Quiz is not currently active");
        }

        // Validate student is member of classroom
        if (!classroomService.isStudentInClassroom(session.getClassroomId(), studentId)) {
            throw new RuntimeException("Only classroom members can participate");
        }

        // Check if late entry is allowed
        if (session.getStatus() == LiveQuizSession.SessionStatus.LIVE &&
            !session.isLateEntryAllowed()) {
            throw new RuntimeException("Quiz has already started and late entry is not allowed");
        }

        // Check max attempts
        long completedAttempts = attemptRepository.countCompletedAttempts(sessionId, studentId);
        if (completedAttempts >= session.getMaxAttempts()) {
            throw new RuntimeException("You have used all allowed attempts for this quiz");
        }

        // Get quiz to know question count
        Quiz quiz = quizAiService.getQuizById(session.getQuizId());
        if (quiz == null) {
            throw new RuntimeException("Quiz not found");
        }

        // Check for existing in-progress attempt
        Optional<LiveQuizAttempt> existing = attemptRepository.findBySessionIdAndStudentIdAndStatus(
                sessionId, studentId, LiveQuizAttempt.AttemptStatus.IN_PROGRESS);

        if (existing.isPresent()) {
            return existing.get(); // Return existing attempt
        }

        // Create new attempt
        LiveQuizAttempt attempt = new LiveQuizAttempt(
                sessionId,
                session.getQuizId(),
                studentId,
                quiz.getQuestionCount()
        );

        return attemptRepository.save(attempt);
    }

    // ==================== UPDATE ====================

    /**
     * Save an answer for a question (auto-save during quiz)
     *
     * @param attemptId Attempt ID
     * @param questionId Question ID (index as string)
     * @param answer Student's answer
     * @return Updated attempt
     */
    public LiveQuizAttempt saveAnswer(String attemptId, String questionId, String answer) {
        LiveQuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Can only save if in progress
        if (attempt.getStatus() != LiveQuizAttempt.AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot save answers for a submitted attempt");
        }

        attempt.addAnswer(questionId, answer);
        return attemptRepository.save(attempt);
    }

    /**
     * Submit the quiz for grading
     *
     * @param attemptId Attempt ID
     * @return Graded attempt
     */
    public LiveQuizAttempt submitQuiz(String attemptId) {
        LiveQuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Validate status
        if (attempt.getStatus() != LiveQuizAttempt.AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Quiz has already been submitted");
        }

        // Mark as submitted
        attempt.markSubmitted();

        // Grade the quiz
        gradeAttempt(attempt);

        // Mark participant as completed in session
        sessionService.markParticipantCompleted(attempt.getSessionId(), attempt.getStudentId());

        return attemptRepository.save(attempt);
    }

    /**
     * Auto-submit quiz when time expires
     *
     * @param sessionId Session ID
     * @param studentId Student ID
     * @return Graded attempt
     */
    public LiveQuizAttempt autoSubmit(String sessionId, String studentId) {
        LiveQuizAttempt attempt = attemptRepository.findBySessionIdAndStudentIdAndStatus(
                sessionId, studentId, LiveQuizAttempt.AttemptStatus.IN_PROGRESS)
                .orElseThrow(() -> new RuntimeException("No active attempt found"));

        attempt.markTimedOut();
        gradeAttempt(attempt);
        sessionService.markParticipantCompleted(sessionId, studentId);

        return attemptRepository.save(attempt);
    }

    // ==================== GRADING ====================

    /**
     * Grade a quiz attempt
     */
    private void gradeAttempt(LiveQuizAttempt attempt) {
        // Get the quiz
        Quiz quiz = quizAiService.getQuizById(attempt.getQuizId());
        if (quiz == null || quiz.getQuestions() == null) {
            throw new RuntimeException("Quiz not found");
        }

        List<Question> questions = quiz.getQuestions();
        int totalMarks = 0;
        int marksEarned = 0;
        int correct = 0;
        int partial = 0;
        int wrong = 0;

        // Grade each question
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String questionId = String.valueOf(i);
            String studentAnswer = attempt.getAnswer(questionId);

            // Assume each question is worth 1 point (can be enhanced)
            totalMarks += 1;

            if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
                wrong++;
                continue;
            }

            if ("MCQ".equals(question.getQuestionType())) {
                // MCQ grading
                if (studentAnswer.equalsIgnoreCase(question.getCorrectAnswer())) {
                    marksEarned += 1;
                    correct++;
                    // Store grading result
                    attempt.getGradingResults().put(questionId,
                        new LiveQuizAttempt.GradingResult(1, 1, "Correct", true));
                } else {
                    wrong++;
                    attempt.getGradingResults().put(questionId,
                        new LiveQuizAttempt.GradingResult(0, 1, "Incorrect. Correct: " + question.getCorrectAnswer(), false));
                }
            } else if ("CQ".equals(question.getQuestionType())) {
                // CQ grading - use AI
                int cqMarks = gradeCQ(question, studentAnswer);
                marksEarned += cqMarks;

                if (cqMarks == 1) {
                    correct++;
                } else if (cqMarks > 0) {
                    partial++;
                } else {
                    wrong++;
                }

                attempt.getGradingResults().put(questionId,
                    new LiveQuizAttempt.GradingResult(cqMarks, 1, "AI graded", cqMarks == 1));
            }
        }

        // Update attempt scores
        attempt.setMarksEarned(marksEarned);
        attempt.setTotalMarks(totalMarks);
        attempt.setCorrectAnswers(correct);
        attempt.setPartialAnswers(partial);
        attempt.setWrongAnswers(wrong);
        attempt.calculateScorePercentage();
        attempt.setStatus(LiveQuizAttempt.AttemptStatus.GRADED);

        // Set results visibility based on session config
        LiveQuizSession session = sessionService.getSessionById(attempt.getSessionId()).orElse(null);
        if (session != null) {
            attempt.setResultsVisible(session.isShowResultsToStudents());
        }
    }

    /**
     * Grade a constructive question using AI
     * Returns 0, 1, or partial points
     */
    private int gradeCQ(Question question, String studentAnswer) {
        // For now, use simple keyword matching
        // Can be enhanced with AI grading
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null || correctAnswer.isEmpty()) {
            return 0; // No correct answer to compare against
        }

        // Simple keyword matching
        String[] keywords = correctAnswer.toLowerCase().split("\\s+");
        String studentLower = studentAnswer.toLowerCase();

        int matches = 0;
        for (String keyword : keywords) {
            if (studentLower.contains(keyword) && keyword.length() > 2) {
                matches++;
            }
        }

        double matchRatio = (double) matches / keywords.length;
        if (matchRatio >= 0.7) {
            return 1; // Full credit
        } else if (matchRatio >= 0.3) {
            return 0; // Partial credit (could be enhanced)
        }
        return 0; // No credit
    }

    // ==================== READ ====================

    /**
     * Get attempt by ID
     */
    public Optional<LiveQuizAttempt> getAttemptById(String attemptId) {
        return attemptRepository.findById(attemptId);
    }

    /**
     * Get attempt with access validation
     */
    public Optional<LiveQuizAttempt> getAttemptWithAccess(String attemptId, String userId) {
        Optional<LiveQuizAttempt> attemptOpt = attemptRepository.findById(attemptId);
        if (attemptOpt.isEmpty()) {
            return Optional.empty();
        }

        LiveQuizAttempt attempt = attemptOpt.get();

        // Get session to check permissions
        LiveQuizSession session = sessionService.getSessionById(attempt.getSessionId()).orElse(null);
        if (session == null) {
            return Optional.empty();
        }

        // Teacher can see all attempts
        if (session.getTeacherId().equals(userId)) {
            return attemptOpt;
        }

        // Student can only see their own
        if (attempt.getStudentId().equals(userId)) {
            return attemptOpt;
        }

        return Optional.empty();
    }

    /**
     * Get all attempts for a session
     */
    public List<LiveQuizAttempt> getAttemptsForSession(String sessionId) {
        return attemptRepository.findBySessionId(sessionId);
    }

    /**
     * Get leaderboard for a session
     */
    public List<LiveQuizAttempt> getLeaderboard(String sessionId) {
        return attemptRepository.findBySessionIdAndCompletedStatusesOrderByScorePercentageDesc(sessionId);
    }

    /**
     * Get student's attempts for a session
     */
    public List<LiveQuizAttempt> getStudentAttempts(String sessionId, String studentId) {
        return attemptRepository.findBySessionIdAndStudentId(sessionId, studentId);
    }

    /**
     * Get current in-progress attempt for a student
     */
    public Optional<LiveQuizAttempt> getCurrentAttempt(String sessionId, String studentId) {
        return attemptRepository.findBySessionIdAndStudentIdAndStatus(
                sessionId, studentId, LiveQuizAttempt.AttemptStatus.IN_PROGRESS);
    }

    /**
     * Check if student can still take the quiz
     */
    public boolean canStudentTakeQuiz(String sessionId, String studentId) {
        // Check session is active
        LiveQuizSession session = sessionService.getSessionById(sessionId).orElse(null);
        if (session == null) {
            return false;
        }

        // Check status
        if (session.getStatus() != LiveQuizSession.SessionStatus.LIVE &&
            session.getStatus() != LiveQuizSession.SessionStatus.LOBBY_OPEN) {
            return false;
        }

        // Check late entry
        if (session.getStatus() == LiveQuizSession.SessionStatus.LIVE &&
            !session.isLateEntryAllowed()) {
            return false;
        }

        // Check attempts remaining
        long completed = attemptRepository.countCompletedAttempts(sessionId, studentId);
        return completed < session.getMaxAttempts();
    }

    /**
     * Get time remaining for an attempt
     */
    public long getTimeRemaining(String attemptId) {
        LiveQuizAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) {
            return 0;
        }

        LiveQuizSession session = sessionService.getSessionById(attempt.getSessionId()).orElse(null);
        if (session == null) {
            return 0;
        }

        // Use session's remaining time calculation
        return session.getRemainingSeconds();
    }

    // ==================== DELETE ====================

    /**
     * Delete all attempts for a session
     */
    public void deleteAttemptsForSession(String sessionId) {
        attemptRepository.deleteBySessionId(sessionId);
    }
}
