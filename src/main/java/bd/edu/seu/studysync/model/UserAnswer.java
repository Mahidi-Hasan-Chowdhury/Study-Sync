package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    private int questionNumber;      // 1, 2, 3, 4, 5
    private String selectedAnswer;   // "A", "B", "C", "D"
    private String correctAnswer;    // "A", "B", "C", "D"
    private boolean isCorrect;       // true/false
    private String question;         // Question text (for display)
}