package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.QuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {

    // NEW: Find attempts by user ID
    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(String userId);
}