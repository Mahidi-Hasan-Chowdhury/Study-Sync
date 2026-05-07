package bd.edu.seu.studysync.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "room_participants")
public class RoomParticipant {
    @Id
    private String id;
    private String roomId;
    private String userId;
    private String username;
    private LocalDateTime joinedAt = LocalDateTime.now();

    private String attemptId;
    private double score;

    public boolean isCompleted() {
        return attemptId != null;
    }
}
