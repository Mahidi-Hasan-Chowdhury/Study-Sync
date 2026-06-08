package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    private int questionNumber;      // 1, 2, 3, 4, 5
    private String questionType;     // "MCQ", "CQ"

    // For MCQ questions
    private String selectedAnswer;   // "A", "B", "C", "D"
    private String correctAnswer;    // "A", "B", "C", "D"
    private boolean isCorrect;       // true/false

    // For CQ questions
    private String textAnswer;       // User's written answer
    private String expectedAnswer;   // Expected key points
    private double cqScore;          // AI-graded score for CQ (0.0 to 1.0)
    private String aiFeedback;       // AI-generated feedback

    private String question;         // Question text (for display)

    // Constructor for MCQ (backward compatibility)
    public UserAnswer(int questionNumber, String selectedAnswer, String correctAnswer, boolean isCorrect, String question) {
        this.questionNumber = questionNumber;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.question = question;
        this.questionType = "MCQ";
        this.cqScore = isCorrect ? 1.0 : 0.0;
    }
}