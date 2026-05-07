package bd.edu.seu.studysync.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "quiz_rooms")
public class QuizRoom {
    @Id
    private String id;
    private String title;
    private String quizId;
    private String creatorId;

    @Indexed(unique = true)
    private String accessCode;

    private LocalDateTime deadline;
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isActive() {
        return deadline != null && LocalDateTime.now().isBefore(deadline);
    }
}
