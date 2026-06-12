package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.LiveQuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LiveQuizAttempt entities
 */
@Repository
public interface LiveQuizAttemptRepository extends MongoRepository<LiveQuizAttempt, String> {

    /**
     * Find all attempts for a specific session
     */
    List<LiveQuizAttempt> findBySessionId(String sessionId);

    /**
     * Find all attempts for a specific session by student
     */
    List<LiveQuizAttempt> findBySessionIdAndStudentId(String sessionId, String studentId);

    /**
     * Find the current attempt for a student in a session
     * (where status is IN_PROGRESS)
     */
    Optional<LiveQuizAttempt> findBySessionIdAndStudentIdAndStatus(
            String sessionId,
            String studentId,
            LiveQuizAttempt.AttemptStatus status
    );

    /**
     * Find all submitted attempts for a session
     */
    List<LiveQuizAttempt> findBySessionIdAndStatusIn(
            String sessionId,
            List<LiveQuizAttempt.AttemptStatus> statuses
    );

    /**
     * Find all attempts by a student
     */
    List<LiveQuizAttempt> findByStudentIdOrderByStartedAtDesc(String studentId);

    /**
     * Check if student has already submitted/completed a quiz in a session
     */
    @Query(value = "{'sessionId': ?0, 'studentId': ?1, 'status': {$in: ['SUBMITTED', 'TIMED_OUT', 'GRADED']}}", count = true)
    long countCompletedAttempts(String sessionId, String studentId);

    /**
     * Get leaderboard for a session (top N by score percentage)
     */
    @Query(value = "{'sessionId': ?0, 'status': {$in: ['SUBMITTED', 'TIMED_OUT', 'GRADED']}}", sort = "{'scorePercentage': -1}")
    List<LiveQuizAttempt> findBySessionIdAndCompletedStatusesOrderByScorePercentageDesc(String sessionId);

    /**
     * Delete all attempts for a session
     */
    void deleteBySessionId(String sessionId);
}
