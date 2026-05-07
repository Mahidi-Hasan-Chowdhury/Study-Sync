package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends MongoRepository<Quiz, String> {

    // NEW: Find quizzes by user ID
    List<Quiz> findByUserIdOrderByCreatedAtDesc(String userId);
}