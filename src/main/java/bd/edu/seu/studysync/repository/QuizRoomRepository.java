package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.QuizRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface QuizRoomRepository extends MongoRepository<QuizRoom, String> {
    Optional<QuizRoom> findByAccessCode(String accessCode);
    List<QuizRoom> findByCreatorId(String creatorId);
}
