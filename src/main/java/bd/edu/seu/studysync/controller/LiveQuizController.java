package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.LiveQuizSession;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.ClassroomService;
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

        // Get user's classrooms (if teacher)
        List<?> classrooms = List.of();
        if (user.isTeacher()) {
            classrooms = classroomService.getTeacherClassrooms(user.getId());
        }

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

        // Validate teacher access
        if (!classroomService.isTeacherOfClassroom(id, user.getId())) {
            return "redirect:/classroom?error=Not+authorized";
        }

        // Get user's quizzes
        List<Quiz> userQuizzes = quizAiService.getQuizzesByUserId(user.getId());

        // Get classroom details
        var classroom = classroomService.getClassroomById(id);

        // Ensure classroomId is set (from path or flash attribute)
        String classroomId = (String) model.getAttribute("classroomId");
        if (classroomId == null || classroomId.isEmpty()) {
            classroomId = id;
        }

        model.addAttribute("classroomId", classroomId);
        model.addAttribute("classroom", classroom.isPresent() ? classroom.get() : null);
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
        var classroom = classroomService.getClassroomById(session.getClassroomId());

        // Check if user is teacher
        boolean isTeacher = session.getTeacherId().equals(user.getId());

        model.addAttribute("session", session);
        model.addAttribute("quiz", quiz);
        model.addAttribute("classroom", classroom.isPresent() ? classroom.get() : null);
        model.addAttribute("isTeacher", isTeacher);
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

        List<LiveQuizSession> sessions;
        if (user.isTeacher()) {
            sessions = liveQuizSessionService.getSessionsByTeacher(user.getId());
        } else {
            sessions = liveQuizSessionService.getSessionsForStudent(user.getId());
        }

        model.addAttribute("sessions", sessions);
        model.addAttribute("currentUser", user);
        model.addAttribute("isTeacher", user.isTeacher());
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

        // Validate access
        if (!classroomService.isTeacherOfClassroom(id, user.getId()) &&
                !classroomService.isStudentInClassroom(id, user.getId())) {
            return "redirect:/classroom?error=Not+a+member";
        }

        List<LiveQuizSession> sessions = liveQuizSessionService.getSessionsByClassroom(id);

        model.addAttribute("classroomId", id);
        model.addAttribute("sessions", sessions);
        model.addAttribute("isTeacher", classroomService.isTeacherOfClassroom(id, user.getId()));
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
