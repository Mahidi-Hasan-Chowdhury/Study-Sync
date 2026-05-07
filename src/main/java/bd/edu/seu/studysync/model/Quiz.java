package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quizzes")
public class Quiz {
    @Id
    private String id;
    private String pdfFileName;
    private List<Question> questions;
    // NEW: Difficulty level
    private String difficulty;  // "EASY", "MEDIUM", "HARD"

    // NEW: Number of questions
    private int questionCount;

    // NEW: Calculated time limit (in seconds)
    private int timeLimitSeconds;
    private LocalDateTime createdAt;
    private String extractedText; // Store for reference (optional)
    
    // NEW: User ID of the creator
    @Indexed
    private String userId;
}