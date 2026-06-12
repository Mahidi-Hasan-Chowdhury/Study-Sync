package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LiveQuizAttempt - Represents a student's attempt at a live quiz session
 *
 * This tracks:
 * - Which session and student
 * - When they started and submitted
 * - Their answers to each question
 * - Their score and results
 * - The status of their attempt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "live_quiz_attempts")
public class LiveQuizAttempt {

    @Id
    private String id;

    // ==================== REFERENCES ====================

    /**
     * ID of the live quiz session
     */
    @Indexed
    private String sessionId;

    /**
     * ID of the quiz being used
     */
    @Indexed
    private String quizId;

    /**
     * ID of the student attempting the quiz
     */
    @Indexed
    private String studentId;

    // ==================== ANSWERS ====================

    /**
     * Map of question ID to student's answer
     * For MCQ: "A", "B", "C", "D"
     * For CQ: Text answer
     */
    private Map<String, String> answers;

    /**
     * Map of question ID to grading result
     * Used for CQ questions graded by AI
     */
    private Map<String, GradingResult> gradingResults;

    // ==================== SCORING ====================

    /**
     * Total marks earned
     */
    private int marksEarned;

    /**
     * Total possible marks
     */
    private int totalMarks;

    /**
     * Score percentage (0-100)
     */
    private double scorePercentage;

    /**
     * Number of correct answers (MCQ)
     */
    private int correctAnswers;

    /**
     * Number of partial credit answers (CQ)
     */
    private int partialAnswers;

    /**
     * Number of wrong/incorrect answers
     */
    private int wrongAnswers;

    /**
     * Total questions attempted
     */
    private int totalQuestions;

    // ==================== TIMING ====================

    /**
     * When the student started the quiz
     */
    private LocalDateTime startedAt;

    /**
     * When the student submitted (or timed out)
     */
    private LocalDateTime submittedAt;

    /**
     * Time taken in seconds
     */
    private long timeTakenSeconds;

    // ==================== STATUS ====================

    /**
     * Current status of this attempt
     */
    @Indexed
    private AttemptStatus status;

    /**
     * Whether results are visible to student
     */
    private boolean resultsVisible;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor for creating a new attempt
     */
    public LiveQuizAttempt(String sessionId, String quizId, String studentId, int totalQuestions) {
        this.sessionId = sessionId;
        this.quizId = quizId;
        this.studentId = studentId;
        this.totalQuestions = totalQuestions;
        this.answers = new HashMap<>();
        this.gradingResults = new HashMap<>();
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.resultsVisible = false;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Add or update an answer for a question
     */
    public void addAnswer(String questionId, String answer) {
        if (this.answers == null) {
            this.answers = new HashMap<>();
        }
        this.answers.put(questionId, answer);
    }

    /**
     * Check if student answered a specific question
     */
    public boolean hasAnswered(String questionId) {
        return answers != null && answers.containsKey(questionId);
    }

    /**
     * Get answer for a specific question
     */
    public String getAnswer(String questionId) {
        return answers != null ? answers.get(questionId) : null;
    }

    /**
     * Mark the attempt as submitted
     */
    public void markSubmitted() {
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.timeTakenSeconds = java.time.Duration.between(this.startedAt, this.submittedAt).getSeconds();
        }
    }

    /**
     * Mark the attempt as timed out
     */
    public void markTimedOut() {
        this.status = AttemptStatus.TIMED_OUT;
        this.submittedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.timeTakenSeconds = java.time.Duration.between(this.startedAt, this.submittedAt).getSeconds();
        }
    }

    /**
     * Calculate score percentage
     */
    public void calculateScorePercentage() {
        if (totalMarks > 0) {
            this.scorePercentage = (marksEarned * 100.0) / totalMarks;
        } else {
            this.scorePercentage = 0.0;
        }
    }

    /**
     * Get the number of questions answered
     */
    public int getAnsweredCount() {
        return answers != null ? answers.size() : 0;
    }

    /**
     * Get the number of unanswered questions
     */
    public int getUnansweredCount() {
        return totalQuestions - getAnsweredCount();
    }

    // ==================== ENUMS ====================

    /**
     * Status of a quiz attempt
     */
    public enum AttemptStatus {
        /**
         * Student is currently taking the quiz
         */
        IN_PROGRESS,

        /**
         * Student submitted the quiz
         */
        SUBMITTED,

        /**
         * Quiz timed out before submission
         */
        TIMED_OUT,

        /**
         * Attempt was graded
         */
        GRADED
    }

    /**
     * Grading result for a question (mainly for CQ)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradingResult {
        private int marksEarned;
        private int totalMarks;
        private String feedback;
        private boolean isCorrect;
    }
}
