package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.LiveQuizSession;
import bd.edu.seu.studysync.model.LiveQuizSession.SessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LiveQuizSession entity
 *
 * Provides database operations for live quiz sessions including:
 * - CRUD operations
 * - Status-based queries
 * - Classroom/Teacher specific queries
 * - Time-based queries for scheduled sessions
 */
@Repository
public interface LiveQuizSessionRepository extends MongoRepository<LiveQuizSession, String> {

    // ==================== BASIC FINDERS ====================

    /**
     * Find all sessions for a specific classroom
     */
    List<LiveQuizSession> findByClassroomId(String classroomId);

    /**
     * Find all sessions for a specific teacher
     */
    List<LiveQuizSession> findByTeacherId(String teacherId);

    /**
     * Find session by classroom and quiz (to check if already scheduled)
     */
    Optional<LiveQuizSession> findByClassroomIdAndQuizId(String classroomId, String quizId);

    // ==================== STATUS-BASED QUERIES ====================

    /**
     * Find all sessions with a specific status
     */
    List<LiveQuizSession> findByStatus(SessionStatus status);

    /**
     * Find all sessions for a classroom with specific status
     */
    List<LiveQuizSession> findByClassroomIdAndStatus(String classroomId, SessionStatus status);

    /**
     * Find all sessions for a classroom with any of the specified statuses
     */
    List<LiveQuizSession> findByClassroomIdAndStatusIn(String classroomId, List<SessionStatus> statuses);

    /**
     * Find all sessions for a teacher with specific status
     */
    List<LiveQuizSession> findByTeacherIdAndStatus(String teacherId, SessionStatus status);

    /**
     * Find all scheduled sessions (not cancelled, not completed)
     */
    List<LiveQuizSession> findByStatusIn(List<SessionStatus> statuses);

    // ==================== TIME-BASED QUERIES ====================

    /**
     * Find sessions scheduled between two dates
     */
    List<LiveQuizSession> findByScheduledStartBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find sessions scheduled to start after a specific time
     * Useful for finding upcoming sessions
     */
    List<LiveQuizSession> findByScheduledStartAfter(LocalDateTime start);

    /**
     * Find sessions that should be in lobby (scheduled start is now or passed)
     * and scheduled end is in future
     */
    List<LiveQuizSession> findByScheduledStartBeforeAndScheduledEndAfter(LocalDateTime start, LocalDateTime end);

    /**
     * Find sessions that should be live (actual start is set, end time is in future)
     */
    List<LiveQuizSession> findByStatusAndActualStartIsNotNullAndActualEndIsNull(SessionStatus status);

    /**
     * Find sessions that have ended (actual end is set)
     */
    List<LiveQuizSession> findByActualEndIsNotNull();

    // ==================== STUDENT/CLASSROOM QUERIES ====================

    /**
     * Find all upcoming sessions for a student's classrooms
     * This would require joining with classroom enrollment
     */
    List<LiveQuizSession> findByClassroomIdInAndStatusIn(
        List<String> classroomIds,
        List<SessionStatus> statuses
    );

    /**
     * Find sessions where a specific student is a participant
     * (assuming participantIds contains student references)
     */
    List<LiveQuizSession> findByParticipantIdsContaining(String studentId);

    /**
     * Find sessions where a student has completed
     */
    List<LiveQuizSession> findByCompletedParticipantIdsContaining(String studentId);

    // ==================== COUNT QUERIES ====================

    /**
     * Count sessions by status for a classroom
     */
    long countByClassroomIdAndStatus(String classroomId, SessionStatus status);

    /**
     * Count sessions by status for a teacher
     */
    long countByTeacherIdAndStatus(String teacherId, SessionStatus status);

    /**
     * Check if a quiz is already scheduled for a classroom
     */
    boolean existsByClassroomIdAndQuizIdAndStatusNot(
        String classroomId,
        String quizId,
        SessionStatus status
    );

    // ==================== ORDERED QUERIES ====================

    /**
     * Find all sessions for a classroom ordered by scheduled start time
     */
    List<LiveQuizSession> findByClassroomIdOrderByScheduledStartDesc(String classroomId);

    /**
     * Find all sessions for a teacher ordered by scheduled start time
     */
    List<LiveQuizSession> findByTeacherIdOrderByScheduledStartDesc(String teacherId);

    /**
     * Find upcoming sessions ordered by start time (soonest first)
     */
    List<LiveQuizSession> findByStatusAndScheduledStartAfterOrderByScheduledStartAsc(
        SessionStatus status,
        LocalDateTime start
    );

    /**
     * Find completed sessions ordered by end time (most recent first)
     */
    List<LiveQuizSession> findByStatusOrderByActualEndDesc(SessionStatus status);
}
