package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LiveQuizSession - Represents a scheduled live quiz session
 *
 * This entity stores all information about a live quiz including:
 * - Schedule (start time, duration, end time)
 * - Configuration (late entry, max participants)
 * - State tracking (status, participants)
 * - Teacher controls (time extensions, messages)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "live_quiz_sessions")
public class LiveQuizSession {

    @Id
    private String id;

    // ==================== BASIC INFO ====================

    /**
     * Session title (e.g., "Midterm Exam - Chapter 5")
     */
    @Indexed
    private String title;

    /**
     * Instructions for students
     */
    private String instructions;

    /**
     * ID of the quiz to be used (references Quiz entity)
     */
    @Indexed
    private String quizId;

    /**
     * ID of the classroom where this quiz is scheduled
     */
    @Indexed
    private String classroomId;

    /**
     * ID of the teacher who created this session
     */
    @Indexed
    private String teacherId;

    // ==================== SCHEDULE ====================

    /**
     * Scheduled start date and time
     */
    @Indexed
    private LocalDateTime scheduledStart;

    /**
     * Duration in seconds
     */
    private long durationSeconds;

    /**
     * Calculated end time (scheduledStart + duration)
     * Can be extended by teacher during live session
     */
    private LocalDateTime scheduledEnd;

    /**
     * Actual start time (when teacher clicked "Start Quiz")
     */
    private LocalDateTime actualStart;

    /**
     * Actual end time (when quiz ended or timed out)
     */
    private LocalDateTime actualEnd;

    // ==================== CONFIGURATION ====================

    /**
     * Allow students to join after quiz has started
     */
    private boolean allowLateEntry = false;

    /**
     * Grace period for late entry (in seconds)
     * Default: 0 (no late entry allowed)
     */
    private long lateEntryGracePeriodSeconds = 0;

    /**
     * Maximum number of participants
     * 0 = unlimited
     */
    private int maxParticipants = 0;

    /**
     * Maximum attempts per student
     */
    private int maxAttempts = 1;

    /**
     * Auto-publish results immediately after quiz ends
     */
    private boolean autoPublishResults = true;

    /**
     * Show results to students immediately
     */
    private boolean showResultsToStudents = true;

    // ==================== STATE ====================

    /**
     * Current status of the live quiz session
     */
    @Indexed
    private SessionStatus status = SessionStatus.SCHEDULED;

    /**
     * List of participant IDs who have joined the waiting room or quiz
     */
    private List<String> participantIds = new ArrayList<>();

    /**
     * List of participant IDs who have completed the quiz
     */
    private List<String> completedParticipantIds = new ArrayList<>();

    /**
     * Total time extension in seconds added by teacher
     */
    private long totalExtensionSeconds = 0;

    /**
     * Timestamp when session was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when session was last updated
     */
    private LocalDateTime updatedAt;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor for creating a new scheduled live quiz
     */
    public LiveQuizSession(String title, String quizId, String classroomId, String teacherId,
                           LocalDateTime scheduledStart, long durationSeconds, String instructions) {
        this.title = title;
        this.quizId = quizId;
        this.classroomId = classroomId;
        this.teacherId = teacherId;
        this.scheduledStart = scheduledStart;
        this.durationSeconds = durationSeconds;
        this.instructions = instructions;
        this.scheduledEnd = scheduledStart.plusSeconds(durationSeconds);
        this.status = SessionStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.participantIds = new ArrayList<>();
        this.completedParticipantIds = new ArrayList<>();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get the current end time considering any extensions
     */
    public LocalDateTime getEffectiveEndTime() {
        if (actualStart != null) {
            return actualStart.plusSeconds(durationSeconds + totalExtensionSeconds);
        }
        return scheduledEnd.plusSeconds(totalExtensionSeconds);
    }

    /**
     * Get remaining time in seconds
     * Returns 0 if quiz hasn't started or has ended
     */
    public long getRemainingSeconds() {
        LocalDateTime now = LocalDateTime.now();

        if (status != SessionStatus.LIVE && status != SessionStatus.LOBBY_OPEN) {
            return 0;
        }

        if (actualStart == null) {
            // Not started yet, return time until scheduled start
            if (now.isBefore(scheduledStart)) {
                return java.time.Duration.between(now, scheduledStart).getSeconds();
            }
            return 0;
        }

        LocalDateTime endTime = getEffectiveEndTime();
        if (now.isAfter(endTime)) {
            return 0;
        }

        return java.time.Duration.between(now, endTime).getSeconds();
    }

    /**
     * Check if late entry is allowed at this moment
     */
    public boolean isLateEntryAllowed() {
        if (!allowLateEntry) {
            return false;
        }

        if (status != SessionStatus.LIVE) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (actualStart == null) {
            return false;
        }

        long elapsed = java.time.Duration.between(actualStart, now).getSeconds();
        return elapsed <= lateEntryGracePeriodSeconds;
    }

    /**
     * Check if student can join
     */
    public boolean canStudentJoin(String studentId) {
        // Check if already participant
        if (participantIds.contains(studentId)) {
            return true; // Already in, can reconnect
        }

        // Check max participants
        if (maxParticipants > 0 && participantIds.size() >= maxParticipants) {
            return false;
        }

        // Check status
        if (status == SessionStatus.SCHEDULED) {
            return false; // Not open yet
        }

        if (status == SessionStatus.LOBBY_OPEN) {
            return true; // Can always join lobby
        }

        if (status == SessionStatus.LIVE) {
            return isLateEntryAllowed();
        }

        return false; // Ended, cancelled, or completed
    }

    /**
     * Check if session is currently in lobby
     */
    public boolean isInLobby() {
        return status == SessionStatus.LOBBY_OPEN;
    }

    /**
     * Check if session is currently live
     */
    public boolean isLive() {
        return status == SessionStatus.LIVE;
    }

    /**
     * Add participant to session
     */
    public void addParticipant(String participantId) {
        if (participantIds == null) {
            participantIds = new ArrayList<>();
        }
        if (!participantIds.contains(participantId)) {
            participantIds.add(participantId);
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * Mark participant as completed
     */
    public void markParticipantCompleted(String participantId) {
        if (completedParticipantIds == null) {
            completedParticipantIds = new ArrayList<>();
        }
        if (!completedParticipantIds.contains(participantId)) {
            completedParticipantIds.add(participantId);
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get participant count
     */
    public int getParticipantCount() {
        return participantIds != null ? participantIds.size() : 0;
    }

    /**
     * Get completed count
     */
    public int getCompletedCount() {
        return completedParticipantIds != null ? completedParticipantIds.size() : 0;
    }

    /**
     * Extend time by specified seconds
     */
    public void extendTime(long secondsToAdd) {
        this.totalExtensionSeconds += secondsToAdd;
        this.scheduledEnd = this.scheduledEnd.plusSeconds(secondsToAdd);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Start the quiz (transitions from LOBBY_OPEN to LIVE)
     */
    public void startQuiz() {
        this.status = SessionStatus.LIVE;
        this.actualStart = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * End the quiz
     */
    public void endQuiz() {
        this.status = SessionStatus.COMPLETED;
        this.actualEnd = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Cancel the quiz
     */
    public void cancelQuiz() {
        this.status = SessionStatus.CANCELLED;
        this.actualEnd = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Open lobby for students to join
     */
    public void openLobby() {
        this.status = SessionStatus.LOBBY_OPEN;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Session Status Enum
     */
    public enum SessionStatus {
        /**
         * Quiz is scheduled, waiting for lobby to open
         */
        SCHEDULED,

        /**
         * Lobby is open, students can join waiting room
         */
        LOBBY_OPEN,

        /**
         * Quiz is live, students are taking it
         */
        LIVE,

        /**
         * Quiz has ended normally
         */
        COMPLETED,

        /**
         * Quiz was cancelled by teacher
         */
        CANCELLED,

        /**
         * Quiz was archived
         */
        ARCHIVED
    }
}
