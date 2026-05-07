package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_attempts")
public class QuizAttempt {
    @Id
    private String id;

    private String quizId;            // Reference to Quiz
    private String pdfFileName;       // For display

    // NEW: Store difficulty and question count
    private String difficulty;
    private int questionCount;
    private int timeLimitSeconds;

    private List<UserAnswer> userAnswers;   // User's answers
    private int totalQuestions;             // 5
    private int correctAnswers;             // 0-5
    private double scorePercentage;         // 0-100

    private int timeTakenSeconds;           // Time spent on quiz
    private LocalDateTime attemptedAt;      // When quiz was taken

    // NEW: User ID who took the quiz
    private String userId;

    private String roomId;
}