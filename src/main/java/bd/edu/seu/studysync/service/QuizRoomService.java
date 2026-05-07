package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.QuizRoom;
import bd.edu.seu.studysync.model.RoomParticipant;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.repository.QuizRoomRepository;
import bd.edu.seu.studysync.repository.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizRoomService {

    private final QuizRoomRepository quizRoomRepository;
    private final RoomParticipantRepository participantRepository;
    private final bd.edu.seu.studysync.repository.UserRepository userRepository; // Inject

    public QuizRoom createRoom(String userId, String quizId, String title, LocalDateTime deadline) {
        QuizRoom room = new QuizRoom();
        room.setCreatorId(userId);
        room.setQuizId(quizId);
        room.setTitle(title);
        room.setDeadline(deadline);
        room.setAccessCode(generateUniqueAccessCode());
        
        QuizRoom savedRoom = quizRoomRepository.save(room);
        
        // Auto-join creator
        joinRoom(savedRoom.getId(), userId);
        
        return savedRoom;
    }

    public RoomParticipant joinRoom(String roomId, String userId) {
        Optional<RoomParticipant> existing = participantRepository.findByRoomIdAndUserId(roomId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        RoomParticipant participant = new RoomParticipant();
        participant.setRoomId(roomId);
        participant.setUserId(userId);
        participant.setJoinedAt(LocalDateTime.now());
        
        // Fetch and set username
        userRepository.findById(userId).ifPresent(user -> participant.setUsername(user.getUsername()));
        
        return participantRepository.save(participant);
    }
    
    public RoomParticipant joinRoomByCode(String accessCode, String userId) {
        QuizRoom room = quizRoomRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid access code"));
                
        if (!room.isActive()) {
            throw new IllegalStateException("Room is expired");
        }
        
        return joinRoom(room.getId(), userId);
    }

    public List<RoomParticipant> getLeaderboard(String roomId) {
        List<RoomParticipant> participants = participantRepository.findByRoomId(roomId);
        
        boolean needsUpdate = false;
        for (RoomParticipant p : participants) {
            if (p.getUsername() == null) {
                userRepository.findById(p.getUserId()).ifPresent(user -> p.setUsername(user.getUsername()));
                participantRepository.save(p); // Save back to DB
            }
        }
        
        return participants.stream()
                .sorted(Comparator.comparingDouble(RoomParticipant::getScore).reversed())
                .collect(Collectors.toList());
    }
    
    public void updateScore(String roomId, String userId, String attemptId, double score) {
        Optional<RoomParticipant> participantOpt = participantRepository.findByRoomIdAndUserId(roomId, userId);
        if (participantOpt.isPresent()) {
            RoomParticipant participant = participantOpt.get();
            participant.setAttemptId(attemptId);
            participant.setScore(score);
            participantRepository.save(participant);
        }
    }
    
    public List<QuizRoom> getMyRooms(String userId) {
        List<RoomParticipant> participations = participantRepository.findByUserId(userId);
        List<String> roomIds = participations.stream()
                .map(RoomParticipant::getRoomId)
                .collect(Collectors.toList());
                
        return quizRoomRepository.findAllById(roomIds);
    }
    
    public Optional<QuizRoom> getRoomById(String id) {
        return quizRoomRepository.findById(id);
    }

    private String generateUniqueAccessCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
