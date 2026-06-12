package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.LiveQuizAttempt;
import bd.edu.seu.studysync.model.LiveQuizSession;
import bd.edu.seu.studysync.model.Question;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.ClassroomService;
import bd.edu.seu.studysync.service.LiveQuizAttemptService;
import bd.edu.seu.studysync.service.LiveQuizSessionService;
import bd.edu.seu.studysync.service.QuizAiService;
import bd.edu.seu.studysync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Live Quiz Session management
 *
 * Handles:
 * - Creating scheduled live quiz sessions
 * - Viewing session lists
 * - Managing session state (start, end, extend)
 * - Student joining waiting room
 */
@Controller
@RequiredArgsConstructor
public class LiveQuizController {

    private final LiveQuizSessionService liveQuizSessionService;
    private final UserService userService;
    private final QuizAiService quizAiService;
    private final ClassroomService classroomService;
    private final LiveQuizAttemptService attemptService;

    // ==================== SCHEDULE PAGE ====================

    /**
     * Show schedule live quiz page
     */
    @GetMapping("/live-quiz/schedule")
    public String showSchedulePage(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get user's quizzes
        List<Quiz> userQuizzes = quizAiService.getQuizzesByUserId(user.getId());

        // Get user's classrooms (where user is owner)
        List<?> classrooms = classroomService.getTeacherClassrooms(user.getId());

        model.addAttribute("quizzes", userQuizzes);
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-schedule");
        return "layout";
    }

    /**
     * Show schedule form for specific classroom
     */
    @GetMapping("/classroom/{id}/live-quiz/schedule")
    public String showScheduleFormForClassroom(
            @PathVariable String id,
            Model model,
            @ModelAttribute("error") String error
    ) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get classroom details first
        var classroom = classroomService.getClassroomById(id);
        if (classroom.isEmpty()) {
            return "redirect:/classroom?error=Classroom+not+found";
        }

        // Validate owner access (only owner can schedule quizzes)
        if (!classroom.get().getTeacherId().equals(user.getId())) {
            return "redirect:/classroom?error=Not+authorized";
        }

        // Get user's quizzes
        List<Quiz> userQuizzes = quizAiService.getQuizzesByUserId(user.getId());

        // Ensure classroomId is set (from path or flash attribute)
        String classroomId = (String) model.getAttribute("classroomId");
        if (classroomId == null || classroomId.isEmpty()) {
            classroomId = id;
        }

        model.addAttribute("classroomId", classroomId);
        model.addAttribute("classroom", classroom.get());
        model.addAttribute("quizzes", userQuizzes);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-schedule-classroom");
        return "layout";
    }

    // ==================== CREATE SESSION ====================

    /**
     * Create a new live quiz session
     */
    @PostMapping("/live-quiz/create")
    public String createSession(
            @RequestParam String title,
            @RequestParam String quizId,
            @RequestParam String classroomId,
            @RequestParam String scheduledDate,
            @RequestParam String scheduledTime,
            @RequestParam long durationMinutes,
            @RequestParam(required = false) String instructions,
            @RequestParam(defaultValue = "false") boolean allowLateEntry,
            @RequestParam(defaultValue = "0") long lateEntryGraceMinutes,
            @RequestParam(defaultValue = "0") int maxParticipants,
            @RequestParam(defaultValue = "1") int maxAttempts,
            @RequestParam(defaultValue = "true") boolean autoPublishResults,
            @RequestParam(defaultValue = "true") boolean showResultsToStudents,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            // Parse date and time
            LocalDateTime scheduledStart = parseDateTime(scheduledDate, scheduledTime);
            long durationSeconds = durationMinutes * 60;
            long lateEntryGraceSeconds = lateEntryGraceMinutes * 60;

            // Create session
            LiveQuizSession session = liveQuizSessionService.createSessionWithConfig(
                    title,
                    quizId,
                    classroomId,
                    currentUser.get().getId(),
                    scheduledStart,
                    durationSeconds,
                    instructions,
                    allowLateEntry,
                    lateEntryGraceSeconds,
                    maxParticipants,
                    maxAttempts,
                    autoPublishResults,
                    showResultsToStudents
            );

            redirectAttributes.addFlashAttribute("success",
                    "Live quiz scheduled successfully! Starts at: " + formatDateTime(scheduledStart));
            return "redirect:/classroom/" + classroomId;

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // Also pass classroomId as flash attribute for error display
            redirectAttributes.addFlashAttribute("classroomId", classroomId);
            return "redirect:/classroom/" + classroomId + "/live-quiz/schedule";
        }
    }

    // ==================== VIEW SESSION ====================

    /**
     * View session details (teacher view)
     */
    @GetMapping("/live-quiz/{id}")
    public String viewSession(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get session with access validation
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionService.getSessionWithAccess(id, user.getId());

        if (sessionOpt.isEmpty()) {
            return "redirect:/classroom?error=Session+not+found+or+access+denied";
        }

        LiveQuizSession session = sessionOpt.get();

        // Get quiz and classroom details
        Quiz quiz = quizAiService.getQuizById(session.getQuizId());

        // Get classroom to check membership
        var classroom = classroomService.getClassroomById(session.getClassroomId());

        // Check if user is owner (creator of the classroom/session)
        boolean isOwner = session.getTeacherId().equals(user.getId());

        // Check if user is member (joined the classroom)
        boolean isMember = classroom.isPresent() &&
                classroom.get().getStudentIds() != null &&
                classroom.get().getStudentIds().contains(user.getId());

        model.addAttribute("session", session);
        model.addAttribute("quiz", quiz);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isMember", isMember);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-details");
        return "layout";
    }

    /**
     * View session details for specific classroom
     */
    @GetMapping("/classroom/{classroomId}/live-quiz/{id}")
    public String viewSessionFromClassroom(
            @PathVariable String classroomId,
            @PathVariable String id,
            Model model
    ) {
        return viewSession(id, model);
    }

    // ==================== LIST SESSIONS ====================

    /**
     * List all sessions for current user
     */
    @GetMapping("/live-quiz/my-sessions")
    public String listMySessions(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get all sessions: where user is owner (teacher) or member of the classroom
        List<LiveQuizSession> ownerSessions = liveQuizSessionService.getSessionsByTeacher(user.getId());
        List<LiveQuizSession> memberSessions = liveQuizSessionService.getSessionsForStudent(user.getId());

        // Combine and deduplicate
        List<LiveQuizSession> allSessions = new java.util.ArrayList<>(ownerSessions);
        for (LiveQuizSession session : memberSessions) {
            if (!allSessions.contains(session)) {
                allSessions.add(session);
            }
        }

        model.addAttribute("sessions", allSessions);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-list");
        return "layout";
    }

    /**
     * List sessions for a classroom
     */
    @GetMapping("/classroom/{id}/live-quiz")
    public String listClassroomSessions(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get classroom to check access
        var classroom = classroomService.getClassroomById(id);
        if (classroom.isEmpty()) {
            return "redirect:/classroom?error=Classroom+not+found";
        }

        // Validate access (owner or member)
        boolean isOwner = classroom.get().getTeacherId().equals(user.getId());
        boolean isMember = classroom.get().getStudentIds() != null &&
                classroom.get().getStudentIds().contains(user.getId());

        if (!isOwner && !isMember) {
            return "redirect:/classroom?error=Not+a+member";
        }

        List<LiveQuizSession> sessions = liveQuizSessionService.getSessionsByClassroom(id);

        model.addAttribute("classroomId", id);
        model.addAttribute("sessions", sessions);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isMember", isMember);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-classroom-list");
        return "layout";
    }

    // ==================== STATE MANAGEMENT ====================

    /**
     * Open lobby
     */
    @PostMapping("/live-quiz/{id}/open-lobby")
    public String openLobby(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.openLobby(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Lobby opened successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    /**
     * Start quiz
     */
    @PostMapping("/live-quiz/{id}/start")
    public String startQuiz(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.startQuiz(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Quiz started successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    /**
     * End quiz
     */
    @PostMapping("/live-quiz/{id}/end")
    public String endQuiz(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.endQuiz(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Quiz ended successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    /**
     * Cancel quiz
     */
    @PostMapping("/live-quiz/{id}/cancel")
    public String cancelQuiz(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.cancelQuiz(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Quiz cancelled successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    /**
     * Extend time
     */
    @PostMapping("/live-quiz/{id}/extend")
    public String extendTime(
            @PathVariable String id,
            @RequestParam long minutes,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            long seconds = minutes * 60;
            liveQuizSessionService.extendTime(id, currentUser.get().getId(), seconds);

            String action = seconds > 0 ? "extended" : "reduced";
            redirectAttributes.addFlashAttribute("success",
                    "Quiz time " + action + " by " + Math.abs(minutes) + " minute(s)");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    /**
     * Delete session
     */
    @PostMapping("/live-quiz/{id}/delete")
    public String deleteSession(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.deleteSession(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Session deleted successfully");
            return "redirect:/live-quiz/my-sessions";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/live-quiz/" + id;
        }
    }

    // ==================== QUIZ TAKING ====================

    /**
     * Take a live quiz (student view)
     */
    @GetMapping("/live-quiz/{id}/take")
    public String takeQuiz(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get session with access validation
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionService.getSessionById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/classroom?error=Session+not+found";
        }

        LiveQuizSession session = sessionOpt.get();

        // Get classroom to check membership
        var classroom = classroomService.getClassroomById(session.getClassroomId());

        // Validate access (owner or member)
        boolean isOwner = session.getTeacherId().equals(user.getId());
        boolean isMember = classroom.isPresent() &&
                classroom.get().getStudentIds() != null &&
                classroom.get().getStudentIds().contains(user.getId());

        if (!isOwner && !isMember) {
            return "redirect:/classroom?error=Not+authorized";
        }

        // Check if student can take quiz
        if (!attemptService.canStudentTakeQuiz(id, user.getId())) {
            return "redirect:/live-quiz/" + id + "?error=Cannot+take+quiz+at+this+time";
        }

        // Start or get existing attempt
        LiveQuizAttempt attempt;
        Optional<LiveQuizAttempt> existingAttempt = attemptService.getCurrentAttempt(id, user.getId());

        if (existingAttempt.isPresent()) {
            attempt = existingAttempt.get();
        } else {
            attempt = attemptService.startAttempt(id, user.getId());
        }

        // Get quiz questions
        Quiz quiz = quizAiService.getQuizById(session.getQuizId());
        if (quiz == null) {
            return "redirect:/classroom?error=Quiz+not+found";
        }

        // Calculate time remaining
        long timeRemaining = attemptService.getTimeRemaining(attempt.getId());

        model.addAttribute("session", session);
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("timeRemaining", timeRemaining);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-take");
        return "layout";
    }

    /**
     * Save an answer (auto-save during quiz)
     */
    @PostMapping("/live-quiz/attempts/{attemptId}/save-answer")
    @ResponseBody
    public String saveAnswer(
            @PathVariable String attemptId,
            @RequestParam String questionId,
            @RequestParam String answer) {
        try {
            attemptService.saveAnswer(attemptId, questionId, answer);
            return "{\"success\": true}";
        } catch (RuntimeException e) {
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Submit quiz for grading
     */
    @PostMapping("/live-quiz/attempts/{attemptId}/submit")
    public String submitQuiz(
            @PathVariable String attemptId,
            RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            LiveQuizAttempt attempt = attemptService.submitQuiz(attemptId);

            // Redirect to results
            redirectAttributes.addFlashAttribute("success", "Quiz submitted successfully!");
            return "redirect:/live-quiz/attempts/" + attemptId + "/results";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/live-quiz/" + attemptId;
        }
    }

    /**
     * View quiz attempt results
     */
    @GetMapping("/live-quiz/attempts/{attemptId}/results")
    public String viewResults(@PathVariable String attemptId, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get attempt with access validation
        Optional<LiveQuizAttempt> attemptOpt = attemptService.getAttemptWithAccess(attemptId, user.getId());
        if (attemptOpt.isEmpty()) {
            return "redirect:/classroom?error=Attempt+not+found+or+access+denied";
        }

        LiveQuizAttempt attempt = attemptOpt.get();

        // Get session and quiz details
        LiveQuizSession session = liveQuizSessionService.getSessionById(attempt.getSessionId())
                .orElse(null);
        Quiz quiz = quizAiService.getQuizById(attempt.getQuizId());

        model.addAttribute("attempt", attempt);
        model.addAttribute("session", session);
        model.addAttribute("quiz", quiz);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-results");
        return "layout";
    }

    /**
     * View leaderboard for a session
     */
    @GetMapping("/live-quiz/{id}/leaderboard")
    public String viewLeaderboard(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get session with access validation
        Optional<LiveQuizSession> sessionOpt = liveQuizSessionService.getSessionById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/classroom?error=Session+not+found";
        }

        LiveQuizSession session = sessionOpt.get();

        // Get classroom to check membership
        var classroom = classroomService.getClassroomById(session.getClassroomId());

        // Validate access
        boolean isOwner = session.getTeacherId().equals(user.getId());
        boolean isMember = classroom.isPresent() &&
                classroom.get().getStudentIds() != null &&
                classroom.get().getStudentIds().contains(user.getId());

        if (!isOwner && !isMember) {
            return "redirect:/classroom?error=Not+authorized";
        }

        // Get leaderboard
        List<LiveQuizAttempt> leaderboard = attemptService.getLeaderboard(id);

        model.addAttribute("session", session);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isMember", isMember);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "live-quiz-leaderboard");
        return "layout";
    }

    // ==================== STUDENT ACTIONS ====================

    /**
     * Student joins waiting room
     */
    @PostMapping("/live-quiz/{id}/join")
    public String joinWaitingRoom(@PathVariable String id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            liveQuizSessionService.joinWaitingRoom(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Joined waiting room successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/live-quiz/" + id;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Parse date and time strings into LocalDateTime
     */
    private LocalDateTime parseDateTime(String date, String time) {
        try {
            // date format: "2026-06-07"
            // time format: "16:00"
            String[] dateParts = date.split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);

            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date or time format");
        }
    }

    /**
     * Format LocalDateTime for display
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.toString().replace("T", " ");
    }
}
