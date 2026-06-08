package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String question;

    // MCQ fields (optional for CQ questions)
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer; // For MCQ: "A", "B", "C", or "D"; For CQ: expected answer key points

    // Question type: "MCQ", "CQ", "TRUE_FALSE", etc.
    private String questionType; // "MCQ" (default), "CQ" (Constructed Response)

    // For CQ: detailed answer/explanation for AI grading reference
    private String answerExplanation;

    // Constructor for MCQ (backward compatibility)
    public Question(String question, String optionA, String optionB, String optionC, String optionD, String correctAnswer) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.questionType = "MCQ";
    }
}