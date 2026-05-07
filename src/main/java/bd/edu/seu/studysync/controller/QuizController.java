package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.DashboardStats;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizAttempt;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final DocumentService documentService;
    private final QuizAiService quizAiService;
    private final QuizAttemptService quizAttemptService;
    private final DashboardService dashboardService;
    private final UserService userService;


    @GetMapping
    public String quizPage(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("contentPage", "quiz");
        return "layout";
    }

    @GetMapping("/my-quizzes")
    public String myQuizzesPage(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        List<Quiz> quizzes = quizAiService.getQuizzesByUserId(currentUser.get().getId());
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("contentPage", "my-quizzes");
        return "layout";
    }

    /**
     * UPDATED: Handle difficulty, question count, and PRO features
     */
    @PostMapping("/upload")
    public String uploadFileAndGenerateQuiz(
            @RequestParam("pdfFile") MultipartFile file, // keeping param name as pdfFile for frontend compatibility, but it supports others
            @RequestParam("difficulty") String difficulty,
            @RequestParam("questionCount") int questionCount,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        try {
            // Validate file presence
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file");
                return "redirect:/quiz";
            }

            String filename = file.getOriginalFilename().toLowerCase();
            long fileSize = file.getSize();
            boolean isPro = currentUser.isPro();

            // 1. Validate File Size
            long maxFreeSize = 5 * 1024 * 1024; // 5MB
            long maxProSize = 20 * 1024 * 1024; // 20MB

            if (isPro) {
                if (fileSize > maxProSize) {
                    redirectAttributes.addFlashAttribute("error", "File too large. Pro limit is 20MB.");
                    return "redirect:/quiz";
                }
            } else {
                if (fileSize > maxFreeSize) {
                    redirectAttributes.addFlashAttribute("error", "File too large. Free limit is 5MB. Upgrade to Pro for 20MB!");
                    return "redirect:/quiz";
                }
            }

            // 2. Validate File Type
            boolean isPdf = filename.endsWith(".pdf");
            boolean isDocx = filename.endsWith(".docx");
            boolean isPptx = filename.endsWith(".pptx");

            if (!isPdf && !isDocx && !isPptx) {
                redirectAttributes.addFlashAttribute("error", "Only PDF, DOCX, and PPTX files are allowed");
                return "redirect:/quiz";
            }

            // 3. Pro-only Formats
            if ((isDocx || isPptx) && !isPro) {
                redirectAttributes.addFlashAttribute("error", "DOCX and PPTX are Pro features. Upgrade to upload these formats!");
                return "redirect:/quiz";
            }

            // Validate question count
            if (questionCount < 1 || questionCount > 20) {
                redirectAttributes.addFlashAttribute("error", "Question count must be between 1 and 20");
                return "redirect:/quiz";
            }

            // Save File through DocumentService
            String savedFileName = documentService.saveFile(file);

            // Extract text
            String extractedText = documentService.extractText(savedFileName);

            // Generate quiz
            Quiz quiz = quizAiService.generateQuiz(
                    extractedText,
                    file.getOriginalFilename(),
                    difficulty,
                    questionCount,
                    currentUser.getId()
            );

            // Success message
            redirectAttributes.addFlashAttribute("success",
                    "Quiz generated successfully! " + questionCount + " questions at " + difficulty + " level.");
            return "redirect:/quiz/my-quizzes";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to generate quiz: " + e.getMessage());
            return "redirect:/quiz";
        }
    }

    @GetMapping("/take/{id}")
    public String takeQuiz(@PathVariable String id, 
                           @RequestParam(required = false) String roomId, // New
                           Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        Quiz quiz = quizAiService.getQuizById(id);
        
        // Start: Security Check - Ensure user owns the quiz OR it's a room quiz (which is public if you have code?)
        // Wait, if it's a room quiz, the user might not be the CREATOR of the quiz.
        // We should allow if roomId is valid and user is participant.
        // For simplicity, let's relax the check IF roomId is present.
        if (roomId == null && !quiz.getUserId().equals(currentUser.get().getId())) {
             return "redirect:/quiz?error=Unauthorized access";
        }
        // End: Security Check
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("roomId", roomId);
        model.addAttribute("contentPage", "quiz-take");
        return "layout";
    }

    @PostMapping("/submit/{id}")
    public String submitQuiz(
            @PathVariable String id,
            @RequestParam(required = false) String roomId, // New
            @RequestParam Map<String, String> allParams,
            Model model) {
            
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        try {
            Quiz quiz = quizAiService.getQuizById(id);
            int timeTaken = Integer.parseInt(allParams.getOrDefault("timeTaken", "0"));

            allParams.remove("timeTaken");
            allParams.remove("_csrf");
            allParams.remove("roomId"); // Remove from answer map

            QuizAttempt attempt = quizAttemptService.evaluateQuiz(
                quiz, 
                allParams, 
                timeTaken,
                currentUser.get().getId(),
                roomId // Pass room ID
            );
            
            // Redirect to Room if part of a room
            if (roomId != null && !roomId.isEmpty()) {
                return "redirect:/room/" + roomId;
            }
            
            return "redirect:/quiz/results/" + attempt.getId();

        } catch (Exception e) {
            model.addAttribute("error", "Failed to submit quiz: " + e.getMessage());
            return "redirect:/quiz/take/" + id;
        }
    }

    @GetMapping("/results/{attemptId}")
    public String showResults(@PathVariable String attemptId, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        QuizAttempt attempt = quizAttemptService.getAttemptById(attemptId);
        
        // Security check
        if (!attempt.getUserId().equals(currentUser.get().getId())) {
            return "redirect:/quiz?error=Unauthorized access";
        }
        
        Quiz quiz = quizAiService.getQuizById(attempt.getQuizId());

        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", quiz);
        model.addAttribute("contentPage", "quiz-results");
        return "layout";
    }

    @GetMapping("/view/{id}")
    public String viewQuiz(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        Quiz quiz = quizAiService.getQuizById(id);
        
         // Security Check
        if (!quiz.getUserId().equals(currentUser.get().getId())) {
             return "redirect:/quiz?error=Unauthorized access";
        }
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("contentPage", "quiz-view");
        return "layout";
    }

    @GetMapping("/history")
    public String viewHistory(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        // Filter attempts by user ID
        List<QuizAttempt> attempts = quizAttemptService.getAttemptsByUserId(currentUser.get().getId());
        model.addAttribute("attempts", attempts);
        model.addAttribute("contentPage", "quiz-history");
        return "layout";
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        // Get stats for current user
        DashboardStats stats = dashboardService.getStatistics(currentUser.get().getId());
        model.addAttribute("stats", stats);
        model.addAttribute("contentPage", "dashboard");
        return "layout";
    }
}