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

    // For CQ questions - Points-based scoring (out of 5)
    private String textAnswer;       // User's written answer
    private String expectedAnswer;   // Model answer from PDF
    private Integer pointsEarned;    // Points earned (0-5) - Integer to allow null
    private Integer maxPoints;       // Max points (5 for CQ, 1 for MCQ) - Integer to allow null
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
        // MCQ questions are worth 1 point each
        this.pointsEarned = isCorrect ? 1 : 0;
        this.maxPoints = 1;
    }
}