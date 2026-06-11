package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Classroom;
import bd.edu.seu.studysync.model.LiveQuizSession;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.LiveQuizSession.SessionStatus;
import bd.edu.seu.studysync.repository.LiveQuizSessionRepository;
import bd.edu.seu.studysync.service.QuizAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Live Quiz Sessions
 *
 * Handles:
 * - Creating scheduled live quiz sessions
 * - Managing session state transitions
 * - Participant management
 * - Time extensions
 * - Session queries for teachers and students
 */
@Service
@RequiredArgsConstructor
public class LiveQuizSessionService {

    private final LiveQuizSessionRepository liveQuizSessionRepository;
    private final ClassroomService classroomService;
    private final QuizAiService quizAiService;
    private final UserService userService;

    // ==================== CREATE ====================

    /**
     * Create a new live quiz session
     *
     * @param title Session title
     * @param quizId Quiz to use
     * @param classroomId Classroom to host in
     * @param teacherId Teacher creating the session
     * @param scheduledStart When the quiz should start
     * @param durationSeconds Duration of quiz
     * @param instructions Instructions for students
     * @return Created session
     */
    public LiveQuizSession createSession(
            String title,
            String quizId,
            String classroomId,
            String teacherId,
            LocalDateTime scheduledStart,
            long durationSeconds,
            String instructions
    ) {
        // Validate teacher owns the classroom
        if (!classroomService.isTeacherOfClassroom(classroomId, teacherId)) {
            throw new RuntimeException("Only classroom teacher can schedule live quizzes");
        }

        // Validate quiz exists and belongs to teacher
        Quiz quiz = quizAiService.getQuizById(quizId);
        if (quiz == null) {
            throw new RuntimeException("Quiz not found");
        }

        if (!quiz.getUserId().equals(teacherId)) {
            throw new RuntimeException("You can only schedule your own quizzes");
        }

        // Check if already scheduled (only active/future sessions)
        Optional<LiveQuizSession> existing = liveQuizSessionRepository
                .findByClassroomIdAndQuizId(classroomId, quizId);

        if (existing.isPresent()) {
            LiveQuizSession existingSession = existing.get();
            // Check if session is still active or in the future
            boolean isInactive = existingSession.getStatus() == SessionStatus.CANCELLED
                    || existingSession.getStatus() == SessionStatus.COMPLETED
                    || existingSession.getStatus() == SessionStatus.ARCHIVED;

            // Also check if the session has already ended
            boolean hasEnded = existingSession.getScheduledEnd() != null
                    && existingSession.getScheduledEnd().isBefore(LocalDateTime.now());

            if (!isInactive && !hasEnded) {
                throw new RuntimeException("This quiz is already scheduled for this classroom");
            }
        }

        // Validate scheduled time is in the future
        if (scheduledStart.isBefore(LocalDateTime.now().plusMinutes(5))) {
            throw new RuntimeException("Quiz must be scheduled at least 5 minutes in advance");
        }

        // Validate duration
        if (durationSeconds < 60) {
            throw new RuntimeException("Quiz duration must be at least 1 minute");
        }
        if (durationSeconds > 14400) { // 4 hours max
            throw new RuntimeException("Quiz duration cannot exceed 4 hours");
        }

        // Create session
        LiveQuizSession session = new LiveQuizSession(
                title,
                quizId,
                classroomId,
                teacherId,
                scheduledStart,
                durationSeconds,
                instructions
        );

        return liveQuizSessionRepository.save(session);
    }

    /**
     * Create a new live quiz session with configuration
     */
    public LiveQuizSession createSessionWithConfig(
            String title,
            String quizId,
            String classroomId,
            String teacherId,
            LocalDateTime scheduledStart,
            long durationSeconds,
            String instructions,
            boolean allowLateEntry,
            long lateEntryGracePeriodSeconds,
            int maxParticipants,
            int maxAttempts,
            boolean autoPublishResults,
            boolean showResultsToStudents
    ) {
        LiveQuizSession session = createSession(
                title,
                quizId,
                classroomId,
                teacherId,
                scheduledStart,
                durationSeconds,
                instructions
        );

        // Apply additional configuration
        session.setAllowLateEntry(allowLateEntry);
        session.setLateEntryGracePeriodSeconds(lateEntryGracePeriodSeconds);
        session.setMaxParticipants(maxParticipants);
        session.setMaxAttempts(maxAttempts);
        session.setAutoPublishResults(autoPublishResults);
        session.setShowResultsToStudents(showResultsToStudents);

        return liveQuizSessionRepository.save(session);
    }

    // ==================== READ ====================

    /**
     * Get session by ID
     */
    public Optional<LiveQuizSession> getSessionById(String sessionId) {
        return liveQuizSessionRepository.findById(sessionId);
    }

    /**
     * Get all sessions for a classroom
     */
    public List<LiveQuizSession> getSessionsByClassroom(String classroomId) {
        return liveQuizSessionRepository.findByClassroomIdOrderByScheduledStartDesc(classroomId);
    }

    /**
     * Get all sessions for a teacher
     */
    public List<LiveQuizSession> getSessionsByTeacher(String teacherId) {
        return liveQuizSessionRepository.findByTeacherIdOrderByScheduledStartDesc(teacherId);
    }

    /**
     * Get upcoming sessions for a classroom
     */
    public List<LiveQuizSession> getUpcomingSessions(String classroomId) {
        return liveQuizSessionRepository.findByClassroomIdAndStatusIn(
                classroomId,
                List.of(SessionStatus.SCHEDULED, SessionStatus.LOBBY_OPEN)
        );
    }

    /**
     * Get completed sessions for a classroom
     */
    public List<LiveQuizSession> getCompletedSessions(String classroomId) {
        return liveQuizSessionRepository.findByClassroomIdAndStatus(
                classroomId,
                SessionStatus.COMPLETED
        );
    }

    /**
     * Get sessions where student is enrolled
     */
    public List<LiveQuizSession> getSessionsForStudent(String studentId) {
        // Get all classrooms where student is member
        List<Classroom> classrooms = classroomService.getStudentClassrooms(studentId);
        List<String> classroomIds = classrooms.stream()
                .map(Classroom::getId)
                .toList();

        if (classroomIds.isEmpty()) {
            return List.of();
        }

        return liveQuizSessionRepository.findByClassroomIdInAndStatusIn(
                classroomIds,
                List.of(SessionStatus.SCHEDULED, SessionStatus.LOBBY_OPEN, SessionStatus.LIVE)
        );
    }

    /**
     * Get session with validation that user can access it
     */
    public Optional<LiveQuizSession> getSessionWithAccess(String sessionId, String userId) {
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        LiveQuizSession session = sessionOpt.get();

        // Teacher can always access their sessions
        if (session.getTeacherId().equals(userId)) {
            return sessionOpt;
        }

        // Students can access if they're classroom members
        if (classroomService.isStudentInClassroom(session.getClassroomId(), userId)) {
            return sessionOpt;
        }

        return Optional.empty();
    }

    // ==================== UPDATE ====================

    /**
     * Update session details
     */
    public LiveQuizSession updateSession(
            String sessionId,
            String teacherId,
            String title,
            LocalDateTime scheduledStart,
            long durationSeconds,
            String instructions
    ) {
        LiveQuizSession session = liveQuizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Validate ownership
        if (!session.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("Only session creator can update");
        }

        // Can only update if not started yet
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new RuntimeException("Cannot update session that has already started");
        }

        // Update fields
        if (title != null && !title.isBlank()) {
            session.setTitle(title);
        }
        if (scheduledStart != null) {
            if (scheduledStart.isBefore(LocalDateTime.now().plusMinutes(5))) {
                throw new RuntimeException("Quiz must be scheduled at least 5 minutes in advance");
            }
            session.setScheduledStart(scheduledStart);
            session.setScheduledEnd(scheduledStart.plusSeconds(durationSeconds));
        }
        if (durationSeconds > 0) {
            if (durationSeconds < 60) {
                throw new RuntimeException("Quiz duration must be at least 1 minute");
            }
            session.setDurationSeconds(durationSeconds);
            session.setScheduledEnd(session.getScheduledStart().plusSeconds(durationSeconds));
        }
        if (instructions != null) {
            session.setInstructions(instructions);
        }

        return liveQuizSessionRepository.save(session);
    }

    // ==================== STATE MANAGEMENT ====================

    /**
     * Open lobby for students to join
     */
    public LiveQuizSession openLobby(String sessionId, String teacherId) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new RuntimeException("Can only open lobby for scheduled sessions");
        }

        session.openLobby();
        return liveQuizSessionRepository.save(session);
    }

    /**
     * Start the quiz
     */
    public LiveQuizSession startQuiz(String sessionId, String teacherId) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() != SessionStatus.LOBBY_OPEN) {
            throw new RuntimeException("Quiz must be in lobby to start");
        }

        session.startQuiz();
        return liveQuizSessionRepository.save(session);
    }

    /**
     * End the quiz immediately
     */
    public LiveQuizSession endQuiz(String sessionId, String teacherId) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() != SessionStatus.LIVE) {
            throw new RuntimeException("Can only end live quizzes");
        }

        session.endQuiz();
        return liveQuizSessionRepository.save(session);
    }

    /**
     * Cancel the quiz
     */
    public LiveQuizSession cancelQuiz(String sessionId, String teacherId) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel completed or cancelled quiz");
        }

        session.cancelQuiz();
        return liveQuizSessionRepository.save(session);
    }

    /**
     * Extend quiz time
     */
    public LiveQuizSession extendTime(String sessionId, String teacherId, long secondsToAdd) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() != SessionStatus.LIVE) {
            throw new RuntimeException("Can only extend time for live quizzes");
        }

        // Validate extension amount
        if (secondsToAdd < -300) {
            throw new RuntimeException("Cannot reduce time by more than 5 minutes");
        }
        if (secondsToAdd > 3600) {
            throw new RuntimeException("Cannot extend time by more than 1 hour");
        }

        // Check if extension would make time negative
        long currentRemaining = session.getRemainingSeconds();
        if (currentRemaining + secondsToAdd < 0) {
            throw new RuntimeException("Cannot reduce time below current remaining");
        }

        session.extendTime(secondsToAdd);
        return liveQuizSessionRepository.save(session);
    }

    // ==================== PARTICIPANT MANAGEMENT ====================

    /**
     * Student joins waiting room
     */
    public LiveQuizSession joinWaitingRoom(String sessionId, String studentId) {
        LiveQuizSession session = liveQuizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Validate student is classroom member
        if (!classroomService.isStudentInClassroom(session.getClassroomId(), studentId)) {
            throw new RuntimeException("Only classroom members can join");
        }

        // Check if can join
        if (!session.canStudentJoin(studentId)) {
            throw new RuntimeException("Cannot join at this time");
        }

        session.addParticipant(studentId);
        return liveQuizSessionRepository.save(session);
    }

    /**
     * Student completes quiz
     */
    public LiveQuizSession markParticipantCompleted(String sessionId, String studentId) {
        LiveQuizSession session = liveQuizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getParticipantIds().contains(studentId)) {
            throw new RuntimeException("Student not in participants list");
        }

        session.markParticipantCompleted(studentId);
        return liveQuizSessionRepository.save(session);
    }

    // ==================== DELETE ====================

    /**
     * Delete a session (only if not started)
     */
    public void deleteSession(String sessionId, String teacherId) {
        LiveQuizSession session = validateAndGetSession(sessionId, teacherId);

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new RuntimeException("Can only delete scheduled sessions");
        }

        liveQuizSessionRepository.deleteById(sessionId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validate session exists and user is the teacher
     */
    private LiveQuizSession validateAndGetSession(String sessionId, String teacherId) {
        LiveQuizSession session = liveQuizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("Only session creator can perform this action");
        }

        return session;
    }

    /**
     * Get quiz details for a session
     */
    public Quiz getSessionQuiz(String sessionId) {
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        return quizAiService.getQuizById(sessionOpt.get().getQuizId());
    }

    /**
     * Get classroom for a session
     */
    public Optional<Classroom> getSessionClassroom(String sessionId) {
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        return classroomService.getClassroomById(sessionOpt.get().getClassroomId());
    }
}
