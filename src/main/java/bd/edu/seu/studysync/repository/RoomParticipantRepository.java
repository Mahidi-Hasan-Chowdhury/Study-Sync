package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.RoomParticipant;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends MongoRepository<RoomParticipant, String> {
    List<RoomParticipant> findByRoomId(String roomId);
    List<RoomParticipant> findByUserId(String userId);
    Optional<RoomParticipant> findByRoomIdAndUserId(String roomId, String userId);
}
